package com.pokemon.tcg.application;

import com.pokemon.tcg.api.mapper.GameMapper;
import com.pokemon.tcg.api.mapper.GameStateMapper;
import com.pokemon.tcg.api.dto.response.GameResponseDTO;
import com.pokemon.tcg.api.dto.response.GameStateResponseDTO;
import com.pokemon.tcg.domain.engine.GameEngineFacade;
import com.pokemon.tcg.domain.model.card.Card;
import com.pokemon.tcg.domain.model.deck.Deck;
import com.pokemon.tcg.domain.model.deck.DeckCard;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.model.player.Player;
import com.pokemon.tcg.domain.model.player.PlayerMatchup;
import com.pokemon.tcg.infrastructure.repository.*;
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
    private final GameStateMapper gameStateMapper;
    private final GameMapper gameMapper;

    public GameService(GameEngineFacade engine,
                       GameRepository gameRepository,
                       GameStateRepository stateRepository,
                       GameLogRepository logRepository,
                       CardRepository cardRepository,
                       PlayerRepository playerRepository,
                       DeckRepository deckRepository,
                       PlayerMatchupRepository matchupRepository,
                       GameStateMapper gameStateMapper,
                       GameMapper gameMapper) {
        this.engine           = engine;
        this.gameRepository   = gameRepository;
        this.stateRepository  = stateRepository;
        this.logRepository    = logRepository;
        this.cardRepository   = cardRepository;
        this.playerRepository = playerRepository;
        this.deckRepository   = deckRepository;
        this.matchupRepository= matchupRepository;
        this.gameStateMapper  = gameStateMapper;
        this.gameMapper       = gameMapper;
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
            game.setState(GameState.ACTIVE);
            GameStateSnapshot snapshot = GameStateSnapshot.builder()
                    .game(game)
                    .turnNumber(result.newState().getTurnNumber())
                    .turnPhase(result.newState().getTurnPhase())
                    .currentPlayer(player)
                    .boardState(result.newState())
                    .build();
            stateRepository.save(snapshot);
        }

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
        PlayerState ownState = boardState.getStateFor(requestingPlayerId.toString());
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

        return gameStateMapper.toGameStateResponse(
                boardState,
                requestingPlayerId.toString(),
                requestingPlayerName,
                opponentPlayerName,
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
     * Processes a player action through the game engine and persists
     * the resulting board state as a new snapshot in the database.
     */
    public EngineResult processAction(UUID gameId, UUID playerId, GameAction action) {
        BoardState currentState = getCurrentState(gameId);
        EngineResult result = engine.processAction(currentState, action);

        if (result != null && result.newState() != null) {
            Game game = gameRepository.findById(gameId)
                    .orElseThrow(() -> new IllegalArgumentException("Game not found"));

            Player player = playerRepository.findById(playerId)
                    .orElseThrow(() -> new IllegalArgumentException("Player not found"));

            GameStateSnapshot snapshot = GameStateSnapshot.builder()
                    .game(game)
                    .turnNumber(result.newState().getTurnNumber())
                    .turnPhase(result.newState().getTurnPhase())
                    .currentPlayer(player)
                    .boardState(result.newState())
                    .build();

            stateRepository.save(snapshot);

            if (result.newState().getGameState() == GameState.FINISHED
                    && game.getWinner() != null) {
                updateMatchups(game);
            }
        }

        return result;
    }

    /**
     * Returns the complete action log for the specified game in chronological order.
     */
    public List<GameLogEntry> getLog(UUID gameId) {
        return logRepository.findByGameIdOrderByCreatedAtAsc(gameId);
    }

    /**
     * Returns all games currently in WAITING state, available for a second player to join.
     */
    public List<GameResponseDTO> listOpenGames() {
        List<Game> openGames = gameRepository.findByStateWithPlayersOrderByCreatedAtDesc(GameState.WAITING);
        return gameMapper.toResponseDTOList(openGames);
    }

    /**
     * Updates win/loss records for both players after a game finishes.
     * Uses upsert logic: creates the matchup record if it does not exist yet.
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
}