package com.pokemon.tcg.service;

import com.pokemon.tcg.controller.dto.response.GameLogResponseDTO;
import com.pokemon.tcg.controller.dto.response.OwnPlayerStateResponseDTO;
import com.pokemon.tcg.controller.dto.response.PublicBoardStateDTO;
import com.pokemon.tcg.controller.mapper.GameLogMapper;
import com.pokemon.tcg.controller.mapper.GameMapper;
import com.pokemon.tcg.controller.mapper.GameStateMapper;
import com.pokemon.tcg.controller.dto.response.GameResponseDTO;
import com.pokemon.tcg.controller.dto.response.GameStateResponseDTO;
import com.pokemon.tcg.controller.websocket.GameEventPublisher;
import com.pokemon.tcg.engine.CardLookupPort;
import com.pokemon.tcg.engine.GameEngineFacade;
import com.pokemon.tcg.domain.model.card.Card;
import com.pokemon.tcg.domain.model.deck.Deck;
import com.pokemon.tcg.domain.model.deck.DeckCard;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.model.player.Player;
import com.pokemon.tcg.domain.model.player.PlayerMatchup;
import com.pokemon.tcg.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class GameService {

    private final GameEngineFacade engine;
    private final GameRepository gameRepository;
    private final GameStateRepository stateRepository;
    private final GameLogRepository logRepository;
    private final CardRepository cardRepository;
    private final PlayerRepository playerRepository;
    private final DeckRepository deckRepository;
    private final PlayerMatchupRepository matchupRepository;
    private final GameEventPublisher eventPublisher;
    private final GameStateMapper gameStateMapper;
    private final GameMapper gameMapper;
    private final GameLogMapper gameLogMapper;
    private final CardLookupPort cardLookupPort;

    public GameService(GameEngineFacade engine,
                       GameRepository gameRepository,
                       GameStateRepository stateRepository,
                       GameLogRepository logRepository,
                       CardRepository cardRepository,
                       PlayerRepository playerRepository,
                       DeckRepository deckRepository,
                       PlayerMatchupRepository matchupRepository,
                       GameEventPublisher eventPublisher,
                       GameStateMapper gameStateMapper,
                       GameMapper gameMapper,
                       GameLogMapper gameLogMapper,
                       CardLookupPort cardLookupPort) {
        this.engine            = engine;
        this.gameRepository    = gameRepository;
        this.stateRepository   = stateRepository;
        this.logRepository     = logRepository;
        this.cardRepository    = cardRepository;
        this.playerRepository  = playerRepository;
        this.deckRepository    = deckRepository;
        this.matchupRepository = matchupRepository;
        this.eventPublisher    = eventPublisher;
        this.gameStateMapper   = gameStateMapper;
        this.gameMapper        = gameMapper;
        this.gameLogMapper     = gameLogMapper;
        this.cardLookupPort    = cardLookupPort;
    }

    /**
     * Creates a new game in WAITING state with the authenticated player as player 1.
     * The game waits for a second player to join before initializing.
     */
    public Game createGame(UUID playerId, UUID deckId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));
        Deck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new IllegalArgumentException("Deck not found"));

        Game game = Game.builder()
                .state(GameState.WAITING)
                .build();
        game = gameRepository.save(game);

        GamePlayer gamePlayer = GamePlayer.builder()
                .game(game)
                .player(player)
                .deck(deck)
                .playerNumber(1)
                .build();
        game.getPlayers().add(gamePlayer);

        return gameRepository.save(game);
    }

    /**
     * Joins an existing WAITING game as player 2.
     * Triggers engine initialization: shuffles decks, deals hands,
     * sets prizes, determines first player and saves the initial board state.
     */
    public Game joinGame(UUID gameId, UUID playerId, UUID deckId) {
        Game game = gameRepository.findByIdAndState(gameId, GameState.WAITING)
                .orElseThrow(() -> new IllegalArgumentException("Game not found or not joinable"));

        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));
        Deck deck = deckRepository.findWithCardsById(deckId)
                .orElseThrow(() -> new IllegalArgumentException("Deck not found"));

        GamePlayer gamePlayer = GamePlayer.builder()
                .game(game)
                .player(player)
                .deck(deck)
                .playerNumber(2)
                .build();
        game.getPlayers().add(gamePlayer);
        game.setState(GameState.SETUP);

        GamePlayer player1GamePlayer = game.getPlayers().stream()
                .filter(gp -> gp.getPlayerNumber() == 1)
                .findFirst().orElseThrow();

        Deck deck1 = deckRepository.findWithCardsById(player1GamePlayer.getDeck().getId())
                .orElseThrow(() -> new IllegalArgumentException("Deck 1 not found"));

        List<String> deck1CardIds = buildDeckCardIds(deck1);
        List<String> deck2CardIds = buildDeckCardIds(deck);

        PlayerState ps1 = PlayerState.builder()
                .playerId(player1GamePlayer.getPlayer().getId().toString())
                .deck(deck1CardIds)
                .hand(new ArrayList<>())
                .discard(new ArrayList<>())
                .prizes(new ArrayList<>())
                .bench(new ArrayList<>())
                .build();

        PlayerState ps2 = PlayerState.builder()
                .playerId(player.getId().toString())
                .deck(deck2CardIds)
                .hand(new ArrayList<>())
                .discard(new ArrayList<>())
                .prizes(new ArrayList<>())
                .bench(new ArrayList<>())
                .build();

        EngineResult result = engine.initializeGame(game.getId().toString(), ps1, ps2);

        if (result != null && result.newState() != null) {
            BoardState initialState = result.newState();

            game.setState(GameState.SETUP);
            GameStateSnapshot snapshot = GameStateSnapshot.builder()
                    .game(game)
                    .turnNumber(initialState.getTurnNumber())
                    .turnPhase(initialState.getTurnPhase())
                    .currentPlayer(player)
                    .boardState(initialState)
                    .build();
            stateRepository.save(snapshot);
        }

        eventPublisher.publishLobbyUpdate(
                GameEvent.builder()
                        .type(GameEventType.GAME_STARTED)
                        .data(Map.of("gameId", game.getId().toString()))
                        .build()
        );

        return gameRepository.save(game);
    }

    /**
     * Returns the current board state for the requesting player.
     * Own state is fully visible; opponent state hides hand, deck and prizes.
     * Card IDs are resolved to full card objects for the frontend.
     */
    @Transactional(readOnly = true)
    public GameStateResponseDTO getCurrentState(UUID gameId, UUID requestingPlayerId) {
        GameStateSnapshot snapshot = stateRepository.findTopByGameIdOrderByCreatedAtDesc(gameId)
                .orElseThrow(() -> new IllegalArgumentException("No state found for game: " + gameId));

        BoardState boardState = snapshot.getBoardState();
        PlayerState ownState      = boardState.getStateFor(requestingPlayerId.toString());
        PlayerState opponentState = boardState.getOpponentState(requestingPlayerId.toString());

        Set<String> allCardIds = collectVisibleCardIds(ownState);
        allCardIds.addAll(collectVisibleCardIds(opponentState));

        Map<String, Card> cardCache = cardRepository.findAllById(allCardIds).stream()
                .collect(Collectors.toMap(Card::getId, Function.identity()));

        String requestingPlayerName = playerRepository.findById(requestingPlayerId)
                .map(Player::getUsername)
                .orElse("Unknown");

        UUID opponentPlayerId = UUID.fromString(opponentState.getPlayerId());
        String opponentPlayerName = playerRepository.findById(opponentPlayerId)
                .map(Player::getUsername)
                .orElse("Unknown");

        Game game = gameRepository.findByIdWithPlayersAndDecks(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found"));

        String ownCardBack      = "DEFAULT";
        String ownCoin          = "DEFAULT";
        String opponentCardBack = "DEFAULT";
        String opponentCoin     = "DEFAULT";

        for (GamePlayer gp : game.getPlayers()) {
            if (gp.getPlayer().getId().equals(requestingPlayerId)) {
                ownCardBack = gp.getDeck().getCardBack();
                ownCoin     = gp.getDeck().getCoin();
            } else {
                opponentCardBack = gp.getDeck().getCardBack();
                opponentCoin     = gp.getDeck().getCoin();
            }
        }

        return gameStateMapper.toGameStateResponse(
                boardState,
                requestingPlayerId.toString(),
                requestingPlayerName,
                opponentPlayerName,
                ownCardBack,
                ownCoin,
                opponentCardBack,
                opponentCoin,
                cardCache
        );
    }

    /**
     * Returns the raw board state for internal engine use.
     * Always reads the latest snapshot from the database.
     */
    @Transactional(readOnly = true)
    public BoardState getCurrentState(UUID gameId) {
        GameStateSnapshot snapshot = stateRepository.findTopByGameIdOrderByCreatedAtDesc(gameId)
                .orElseThrow(() -> new IllegalArgumentException("No state found for game: " + gameId));
        return snapshot.getBoardState();
    }

    /**
     * Processes a player action through the game engine, persists the resulting
     * board state as a new snapshot in the database, and logs the action to the game log.
     *
     * <p>After persisting, checks for terminal events:
     * <ul>
     *   <li>{@code GAME_OVER} — marks the game FINISHED and records the winner.</li>
     *   <li>{@code SUDDEN_DEATH_STARTED} — marks the parent game FINISHED with no winner,
     *       then creates a new Sudden Death game and notifies both players via WebSocket.</li>
     * </ul>
     */
    public EngineResult processAction(UUID gameId, UUID playerId, GameAction action) {
        BoardState currentState = getCurrentState(gameId);
        EngineResult result = engine.processAction(currentState, action);

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found"));

        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));

        if (result != null && result.newState() != null) {
            GameStateSnapshot snapshot = GameStateSnapshot.builder()
                    .game(game)
                    .turnNumber(result.newState().getTurnNumber())
                    .turnPhase(result.newState().getTurnPhase())
                    .currentPlayer(player)
                    .boardState(result.newState())
                    .build();
            stateRepository.save(snapshot);

            // ── GAME_OVER: normal victory ──────────────────────────────────────
            if (result.newState().getGameState() == GameState.FINISHED) {
                boolean isSuddenDeathEvent = result.events().stream()
                        .anyMatch(e -> e.getType() == GameEventType.SUDDEN_DEATH_STARTED);

                if (isSuddenDeathEvent) {
                    // ── SUDDEN_DEATH_STARTED: tie — create a new 1-prize game ──
                    game.setState(GameState.FINISHED);
                    game.setFinishReason(FinishReason.SUDDEN_DEATH);
                    game.setFinishedAt(java.time.Instant.now());
                    gameRepository.save(game);

                    createSuddenDeathGame(game);

                } else {
                    // Normal win: record winner and update matchup stats
                    game.setState(GameState.FINISHED);

                    result.events().stream()
                            .filter(e -> e.getType() == GameEventType.GAME_OVER)
                            .findFirst()
                            .ifPresent(e -> {
                                String winnerId = (String) e.getData().get("winnerId");
                                if (winnerId != null && !winnerId.equals("none")) {
                                    playerRepository.findById(UUID.fromString(winnerId))
                                            .ifPresent(game::setWinner);
                                }
                            });

                    gameRepository.save(game);

                    if (game.getWinner() != null) {
                        updateMatchups(game);
                    }
                }
            }

            boolean hasPipelineError = result.events().stream()
                    .anyMatch(e -> e.getType() == GameEventType.TURN_ENDED
                            && e.getData().containsKey("error"));
            String logResult = hasPipelineError ? "FAILED" : "SUCCESS";

            Map<String, Object> actionData = action.getPayload() != null
                    ? new java.util.HashMap<>(action.getPayload())
                    : new java.util.HashMap<>();

            Map<String, Object> resultData = new java.util.HashMap<>();
            if (hasPipelineError) {
                result.events().stream()
                        .filter(e -> e.getData().containsKey("error"))
                        .findFirst()
                        .ifPresent(e -> resultData.put("error", e.getData().get("error")));
            } else {
                result.events().forEach(e ->
                        resultData.put(e.getType().name().toLowerCase(), e.getData()));
            }

            GameLogEntry logEntry = GameLogEntry.builder()
                    .game(game)
                    .turnNumber(currentState.getTurnNumber())
                    .player(player)
                    .actionType(action.getType().name())
                    .actionData(actionData)
                    .result(logResult)
                    .resultData(resultData)
                    .build();
            logRepository.save(logEntry);

            // ── Publish mapped DTOs to WebSocket subscribers ──────────────
            Game gameForPublish = gameRepository.findByIdWithPlayersAndDecks(gameId)
                    .orElseThrow(() -> new IllegalArgumentException("Game not found"));

            String p1Id = result.newState().getPlayer1State().getPlayerId();
            String p2Id = result.newState().getPlayer2State().getPlayerId();

            String p1Name     = "Unknown";
            String p2Name     = "Unknown";
            String p1CardBack = "DEFAULT";
            String p1Coin     = "DEFAULT";
            String p2CardBack = "DEFAULT";
            String p2Coin     = "DEFAULT";

            for (GamePlayer gp : gameForPublish.getPlayers()) {
                String gpPlayerId = gp.getPlayer().getId().toString();
                if (gpPlayerId.equals(p1Id)) {
                    p1Name     = gp.getPlayer().getUsername();
                    p1CardBack = gp.getDeck().getCardBack();
                    p1Coin     = gp.getDeck().getCoin();
                } else if (gpPlayerId.equals(p2Id)) {
                    p2Name     = gp.getPlayer().getUsername();
                    p2CardBack = gp.getDeck().getCardBack();
                    p2Coin     = gp.getDeck().getCoin();
                }
            }

            Map<String, Card> cardCache = cardLookupPort.findAllById(
                    collectCardIds(result.newState()));

            int p1BenchCount = result.newState().getPlayer1State().getBench() != null
                    ? result.newState().getPlayer1State().getBench().size() : 0;
            int p2BenchCount = result.newState().getPlayer2State().getBench() != null
                    ? result.newState().getPlayer2State().getBench().size() : 0;

            BoardState sanitized = sanitizeForPublic(result.newState());

            PublicBoardStateDTO publicDto = gameStateMapper.toPublicBoardStateDTO(
                    sanitized,
                    p1Name, p1CardBack, p1Coin,
                    p2Name, p2CardBack, p2Coin,
                    cardCache,
                    p1BenchCount,
                    p2BenchCount);
            eventPublisher.publishBoardStateDTO(gameId.toString(), publicDto);

            OwnPlayerStateResponseDTO p1Dto = gameStateMapper.toOwnPlayerStateDTO(
                    result.newState().getPlayer1State(), cardCache);
            OwnPlayerStateResponseDTO p2Dto = gameStateMapper.toOwnPlayerStateDTO(
                    result.newState().getPlayer2State(), cardCache);

            eventPublisher.publishPrivateStateDTO(
                    gameId.toString(),
                    result.newState().getPlayer1State().getPlayerId(),
                    p1Dto);
            eventPublisher.publishPrivateStateDTO(
                    gameId.toString(),
                    result.newState().getPlayer2State().getPlayerId(),
                    p2Dto);

            if (result.events() != null) {
                for (GameEvent event : result.events()) {
                    eventPublisher.publishEvent(gameId.toString(), event);
                }
            }
        }

        return result;
    }

    /**
     * Creates a new Sudden Death game from an existing finished game.
     *
     * <p>Per the rulebook, Sudden Death is a complete new game played with the
     * same players and decks, but each player starts with only 1 Prize card
     * instead of 6. The new game is linked to the parent via {@code parentGame}.
     *
     * <p>The new game is initialized immediately (no WAITING state) and jumps
     * directly to SETUP so both players can place their Pokémon. A
     * {@code GAME_STARTED} lobby event is published so the frontend can redirect
     * both players to the new board.
     */
    private void createSuddenDeathGame(Game parentGame) {
        // Retrieve both players and their decks from the parent game
        GamePlayer gp1 = parentGame.getPlayers().stream()
                .filter(gp -> gp.getPlayerNumber() == 1)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Player 1 not found in parent game"));
        GamePlayer gp2 = parentGame.getPlayers().stream()
                .filter(gp -> gp.getPlayerNumber() == 2)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Player 2 not found in parent game"));

        Player player1 = gp1.getPlayer();
        Player player2 = gp2.getPlayer();

        Deck deck1 = deckRepository.findWithCardsById(gp1.getDeck().getId())
                .orElseThrow(() -> new IllegalArgumentException("Deck 1 not found"));
        Deck deck2 = deckRepository.findWithCardsById(gp2.getDeck().getId())
                .orElseThrow(() -> new IllegalArgumentException("Deck 2 not found"));

        // Create the new Sudden Death game entity
        Game sdGame = Game.builder()
                .state(GameState.SETUP)
                .suddenDeath(true)
                .parentGame(parentGame)
                .build();
        sdGame = gameRepository.save(sdGame);

        GamePlayer sdGp1 = GamePlayer.builder()
                .game(sdGame)
                .player(player1)
                .deck(deck1)
                .playerNumber(1)
                .build();
        GamePlayer sdGp2 = GamePlayer.builder()
                .game(sdGame)
                .player(player2)
                .deck(deck2)
                .playerNumber(2)
                .build();
        sdGame.getPlayers().add(sdGp1);
        sdGame.getPlayers().add(sdGp2);
        gameRepository.save(sdGame);

        // Build initial player states and run the engine initialization
        List<String> deck1CardIds = buildDeckCardIds(deck1);
        List<String> deck2CardIds = buildDeckCardIds(deck2);

        PlayerState ps1 = PlayerState.builder()
                .playerId(player1.getId().toString())
                .deck(deck1CardIds)
                .hand(new ArrayList<>())
                .discard(new ArrayList<>())
                .prizes(new ArrayList<>())
                .bench(new ArrayList<>())
                .build();

        PlayerState ps2 = PlayerState.builder()
                .playerId(player2.getId().toString())
                .deck(deck2CardIds)
                .hand(new ArrayList<>())
                .discard(new ArrayList<>())
                .prizes(new ArrayList<>())
                .bench(new ArrayList<>())
                .build();

        EngineResult result = engine.initializeGame(sdGame.getId().toString(), ps1, ps2);

        if (result != null && result.newState() != null) {
            BoardState initialState = result.newState();

            // Truncate prizes to 1 per player — the only difference from a normal game
            truncatePrizesToOne(initialState.getPlayer1State());
            truncatePrizesToOne(initialState.getPlayer2State());

            GameStateSnapshot snapshot = GameStateSnapshot.builder()
                    .game(sdGame)
                    .turnNumber(initialState.getTurnNumber())
                    .turnPhase(initialState.getTurnPhase())
                    .currentPlayer(player1)
                    .boardState(initialState)
                    .build();
            stateRepository.save(snapshot);
        }

        // Notify both players so the frontend can redirect them to the new board
        final Game savedSdGame = sdGame;
        eventPublisher.publishLobbyUpdate(
                GameEvent.builder()
                        .type(GameEventType.SUDDEN_DEATH_STARTED)
                        .data(Map.of(
                                "parentGameId", parentGame.getId().toString(),
                                "newGameId",    savedSdGame.getId().toString()
                        ))
                        .build()
        );
    }

    /**
     * Reduces a player's prize list to exactly 1 card for Sudden Death.
     * The remaining prizes are moved back to the bottom of the deck and shuffled.
     *
     * <p>Moving the discarded prizes back into the deck rather than discarding
     * them outright gives both players the full card pool for the tiebreaker,
     * which is the fairest interpretation of the rulebook.
     */
    private void truncatePrizesToOne(PlayerState ps) {
        List<String> prizes = new ArrayList<>(
                ps.getPrizes() != null ? ps.getPrizes() : new ArrayList<>());

        if (prizes.size() <= 1) return;

        // Keep only the first prize; return the rest to the deck
        String keptPrize = prizes.get(0);
        List<String> returnedToDeck = prizes.subList(1, prizes.size());

        List<String> deck = new ArrayList<>(
                ps.getDeck() != null ? ps.getDeck() : new ArrayList<>());
        deck.addAll(returnedToDeck);
        Collections.shuffle(deck);

        ps.setPrizes(new ArrayList<>(List.of(keptPrize)));
        ps.setDeck(deck);
    }

    /**
     * Returns the complete action log for the specified game in chronological order.
     */
    public List<GameLogResponseDTO> getLog(UUID gameId) {
        return gameLogMapper.toResponseDTOList(
                logRepository.findByGameIdOrderByCreatedAtAsc(gameId));
    }

    /**
     * Returns all games currently in WAITING state, available for a second player to join.
     */
    public List<GameResponseDTO> listOpenGames() {
        List<Game> openGames = gameRepository.findByStateWithPlayersOrderByCreatedAtDesc(GameState.WAITING);
        return gameMapper.toResponseDTOList(openGames);
    }

    /**
     * Returns the most recent active game for the given player, if any.
     */
    @Transactional(readOnly = true)
    public Optional<GameResponseDTO> getActiveGame(UUID playerId) {
        List<GameState> activeStates = List.of(
                GameState.WAITING,
                GameState.SETUP,
                GameState.ACTIVE
        );
        return gameRepository.findActiveGamesByPlayerId(playerId, activeStates)
                .stream()
                .findFirst()
                .map(gameMapper::toResponseDTO);
    }

    /**
     * Cancels a WAITING game. Only the creator (player 1) can cancel it.
     */
    public void cancelGame(UUID gameId, UUID playerId) {
        Game game = gameRepository.findByIdAndState(gameId, GameState.WAITING)
                .orElseThrow(() -> new IllegalArgumentException("Game not found or not cancellable"));

        boolean isCreator = game.getPlayers().stream()
                .anyMatch(gp -> gp.getPlayerNumber() == 1
                        && gp.getPlayer().getId().equals(playerId));

        if (!isCreator) {
            throw new IllegalArgumentException("Only the game creator can cancel the game");
        }

        game.setState(GameState.CANCELLED);
        gameRepository.save(game);
    }

    /**
     * Surrenders an active game on behalf of the requesting player.
     */
    public void surrenderGame(UUID gameId, UUID playerId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found"));

        if (game.getState() != GameState.SETUP && game.getState() != GameState.ACTIVE) {
            throw new IllegalArgumentException("Game is not in a surrenderable state");
        }

        boolean isParticipant = game.getPlayers().stream()
                .anyMatch(gp -> gp.getPlayer().getId().equals(playerId));

        if (!isParticipant) {
            throw new IllegalArgumentException("Player is not a participant of this game");
        }

        Player winner = game.getPlayers().stream()
                .filter(gp -> !gp.getPlayer().getId().equals(playerId))
                .map(GamePlayer::getPlayer)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Opponent not found"));

        game.setState(GameState.FINISHED);
        game.setWinner(winner);
        game.setFinishReason(FinishReason.SURRENDER);
        game.setFinishedAt(java.time.Instant.now());
        gameRepository.save(game);

        updateMatchups(game);
    }

    /**
     * Updates win/loss records for both players after a game finishes.
     */
    private void updateMatchups(Game game) {
        UUID winnerId = game.getWinner().getId();
        UUID loserId = game.getPlayers().stream()
                .map(gp -> gp.getPlayer().getId())
                .filter(id -> !id.equals(winnerId))
                .findFirst()
                .orElse(null);

        if (loserId == null) return;

        Player winner = playerRepository.findById(winnerId).orElseThrow();
        Player loser  = playerRepository.findById(loserId).orElseThrow();

        PlayerMatchup winnerRecord = matchupRepository
                .findByPlayerIdAndOpponentId(winnerId, loserId)
                .orElseGet(() -> PlayerMatchup.builder()
                        .player(winner)
                        .opponent(loser)
                        .build());
        winnerRecord.setWins(winnerRecord.getWins() + 1);
        matchupRepository.save(winnerRecord);

        PlayerMatchup loserRecord = matchupRepository
                .findByPlayerIdAndOpponentId(loserId, winnerId)
                .orElseGet(() -> PlayerMatchup.builder()
                        .player(loser)
                        .opponent(winner)
                        .build());
        loserRecord.setLosses(loserRecord.getLosses() + 1);
        matchupRepository.save(loserRecord);
    }

    /** Expands deck cards into a flat list of card IDs respecting quantity. */
    private List<String> buildDeckCardIds(Deck deck) {
        List<String> cardIds = new ArrayList<>();
        for (DeckCard dc : deck.getCards()) {
            for (int i = 0; i < dc.getQuantity(); i++) {
                cardIds.add(dc.getCard().getId());
            }
        }
        return cardIds;
    }

    /** Collects all card IDs visible to a player for card data resolution. */
    private Set<String> collectVisibleCardIds(PlayerState ps) {
        Set<String> ids = new HashSet<>();
        if (ps.getHand() != null) ids.addAll(ps.getHand());
        if (ps.getPrizes() != null) ids.addAll(ps.getPrizes());
        if (ps.getDiscard() != null) ids.addAll(ps.getDiscard());
        addPokemonCardIds(ids, ps.getActivePokemon());
        if (ps.getBench() != null) {
            ps.getBench().forEach(bp -> addPokemonCardIds(ids, bp));
        }
        return ids;
    }

    private void addPokemonCardIds(Set<String> ids, ActivePokemon ap) {
        if (ap == null) return;
        if (ap.getCardId() != null) ids.add(ap.getCardId());
        if (ap.getAttachedEnergyIds() != null) ids.addAll(ap.getAttachedEnergyIds());
        if (ap.getAttachedToolId() != null) ids.add(ap.getAttachedToolId());
        if (ap.getEvolutionStack() != null) ids.addAll(ap.getEvolutionStack());
    }

    private void addPokemonCardIds(Set<String> ids, BenchPokemon bp) {
        if (bp == null) return;
        if (bp.getCardId() != null) ids.add(bp.getCardId());
        if (bp.getAttachedEnergyIds() != null) ids.addAll(bp.getAttachedEnergyIds());
        if (bp.getAttachedToolId() != null) ids.add(bp.getAttachedToolId());
        if (bp.getEvolutionStack() != null) ids.addAll(bp.getEvolutionStack());
    }

    private Set<String> collectCardIds(BoardState state) {
        Set<String> ids = new HashSet<>();
        for (PlayerState ps : List.of(state.getPlayer1State(), state.getPlayer2State())) {
            if (ps == null) continue;
            if (ps.getHand()    != null) ids.addAll(ps.getHand());
            if (ps.getPrizes()  != null) ids.addAll(ps.getPrizes());
            if (ps.getDiscard() != null) ids.addAll(ps.getDiscard());
            if (ps.getDeck()    != null) ids.addAll(ps.getDeck());
            if (ps.getActivePokemon() != null) {
                ids.add(ps.getActivePokemon().getCardId());
                if (ps.getActivePokemon().getAttachedEnergyIds() != null)
                    ids.addAll(ps.getActivePokemon().getAttachedEnergyIds());
                if (ps.getActivePokemon().getAttachedToolId() != null)
                    ids.add(ps.getActivePokemon().getAttachedToolId());
                if (ps.getActivePokemon().getEvolutionStack() != null)
                    ids.addAll(ps.getActivePokemon().getEvolutionStack());
            }
            if (ps.getBench() != null) {
                for (BenchPokemon bp : ps.getBench()) {
                    if (bp == null) continue;
                    ids.add(bp.getCardId());
                    if (bp.getAttachedEnergyIds() != null)
                        ids.addAll(bp.getAttachedEnergyIds());
                    if (bp.getAttachedToolId() != null)
                        ids.add(bp.getAttachedToolId());
                    if (bp.getEvolutionStack() != null)
                        ids.addAll(bp.getEvolutionStack());
                }
            }
        }
        ids.remove(null);
        return ids;
    }

    private BoardState sanitizeForPublic(BoardState state) {
        boolean isSetup = state.getGameState() == GameState.SETUP;

        PlayerState p1 = PlayerState.builder()
                .playerId(state.getPlayer1State().getPlayerId())
                .activePokemon(isSetup ? null : state.getPlayer1State().getActivePokemon())
                .bench(isSetup ? List.of() : state.getPlayer1State().getBench())
                .discard(state.getPlayer1State().getDiscard())
                .hand(List.of())
                .deck(List.of())
                .prizes(List.of())
                .totalMulligans(state.getPlayer1State().getTotalMulligans())
                .mulliganBonusDraws(state.getPlayer1State().getMulliganBonusDraws())
                .setupConfirmed(state.getPlayer1State().isSetupConfirmed())
                .build();

        PlayerState p2 = PlayerState.builder()
                .playerId(state.getPlayer2State().getPlayerId())
                .activePokemon(isSetup ? null : state.getPlayer2State().getActivePokemon())
                .bench(isSetup ? List.of() : state.getPlayer2State().getBench())
                .discard(state.getPlayer2State().getDiscard())
                .hand(List.of())
                .deck(List.of())
                .prizes(List.of())
                .totalMulligans(state.getPlayer2State().getTotalMulligans())
                .mulliganBonusDraws(state.getPlayer2State().getMulliganBonusDraws())
                .setupConfirmed(state.getPlayer2State().isSetupConfirmed())
                .build();

        return BoardState.builder()
                .gameId(state.getGameId())
                .gameState(state.getGameState())
                .turnPhase(state.getTurnPhase())
                .currentPlayerId(state.getCurrentPlayerId())
                .turnNumber(state.getTurnNumber())
                .player1State(p1)
                .player2State(p2)
                .activeStadiumCardId(state.getActiveStadiumCardId())
                .turnFlags(state.getTurnFlags())
                .pendingEvents(state.getPendingEvents())
                .bonusDrawPending(state.isBonusDrawPending())
                .pendingBenchChoicePlayerId(state.getPendingBenchChoicePlayerId())
                .firstPlayerId(state.getFirstPlayerId())
                .pendingDeckSelectionPlayerId(state.getPendingDeckSelectionPlayerId())
                .pendingDeckSelectionCardIds(state.getPendingDeckSelectionCardIds())
                .pendingBonusPlacement(state.getPendingBonusPlacement())
                .pendingPrizeTakePlayerId(state.getPendingPrizeTakePlayerId())
                .pendingPrizeTakeCount(state.getPendingPrizeTakeCount())
                .pendingNextPlayerId(state.getPendingNextPlayerId())
                .build();
    }
}