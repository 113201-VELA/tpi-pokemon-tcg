package com.pokemon.tcg.engine;

import com.pokemon.tcg.domain.model.game.*;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GameEngineFacadeImpl implements GameEngineFacade {

    private final SetupManager setupManager;
    private final TurnManager turnManager;
    private final RuleValidator ruleValidator;
    private final VictoryConditionChecker victoryChecker;
    private final StatusEffectManager statusEffectManager;

    public GameEngineFacadeImpl(SetupManager setupManager,
                                TurnManager turnManager,
                                RuleValidator ruleValidator,
                                VictoryConditionChecker victoryChecker,
                                StatusEffectManager statusEffectManager) {
        this.setupManager        = setupManager;
        this.turnManager         = turnManager;
        this.ruleValidator       = ruleValidator;
        this.victoryChecker      = victoryChecker;
        this.statusEffectManager = statusEffectManager;
    }

    /**
     * Initializes a new game: shuffles decks, deals hands, sets prizes,
     * determines first player and sets the initial board state.
     */
    @Override
    public EngineResult initializeGame(String gameId, PlayerState player1, PlayerState player2) {
        setupManager.shuffleDeck(player1);
        setupManager.shuffleDeck(player2);

        setupManager.drawInitialHand(player1);
        setupManager.drawInitialHand(player2);

        setupManager.setupPrizes(player1);
        setupManager.setupPrizes(player2);

        String firstPlayerId = setupManager.determineFirstPlayer(
                player1.getPlayerId(), player2.getPlayerId());

        BoardState initialState = BoardState.builder()
                .gameId(gameId)
                .gameState(GameState.SETUP)
                .turnPhase(TurnPhase.SETUP)
                .currentPlayerId(firstPlayerId)
                .turnNumber(0)
                .player1State(player1)
                .player2State(player2)
                .turnFlags(TurnFlags.fresh())
                .pendingEvents(new ArrayList<>())
                .build();

        GameEvent startEvent = GameEvent.builder()
                .type(GameEventType.GAME_STARTED)
                .gameId(gameId)
                .turnNumber(0)
                .data(Map.of("firstPlayerId", firstPlayerId))
                .occurredAt(java.time.Instant.now())
                .build();

        return EngineResult.of(initialState, List.of(startEvent));
    }

    /**
     * Processes a player action, validates it, advances the game state,
     * and checks for victory conditions.
     *
     * <p>Events generated inside the engine (e.g. pipeline cancellations stored
     * in {@code BoardState.pendingEvents}) are collected here and included in the
     * result so the WebSocket layer can broadcast them to clients.
     */
    @Override
    public EngineResult processAction(BoardState currentState, GameAction action) {
        // Validate action
        ValidationResult validation = ruleValidator.validate(currentState, action);
        if (!validation.isValid()) {
            return EngineResult.of(currentState, List.of(
                    GameEvent.builder()
                            .type(GameEventType.TURN_ENDED)
                            .gameId(currentState.getGameId())
                            .playerId(action.getPlayerId())
                            .turnNumber(currentState.getTurnNumber())
                            .data(Map.of("error", validation.getErrorMessage()))
                            .occurredAt(java.time.Instant.now())
                            .build()
            ));
        }

        // Advance state
        BoardState newState = turnManager.advancePhase(currentState, action);

        // Collect any events generated inside the engine (e.g. pipeline cancellations)
        // that were stored in pendingEvents by TurnManager, then clear them so they
        // are not re-emitted on the next action.
        List<GameEvent> events = new ArrayList<>();
        if (newState.getPendingEvents() != null && !newState.getPendingEvents().isEmpty()) {
            events.addAll(newState.getPendingEvents());
            newState = newState.toBuilder()
                    .pendingEvents(new ArrayList<>())
                    .build();
        }

        // Check victory conditions
        Optional<GameEvent> victoryEvent = victoryChecker.check(newState);
        if (victoryEvent.isPresent()) {
            events.add(victoryEvent.get());
            newState = newState.toBuilder()
                    .gameState(GameState.FINISHED)
                    .build();
        }

        return EngineResult.of(newState, events);
    }
}