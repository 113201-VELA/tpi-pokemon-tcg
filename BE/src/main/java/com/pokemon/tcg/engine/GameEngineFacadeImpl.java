package com.pokemon.tcg.engine;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.state.GameStateHandler;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class GameEngineFacadeImpl implements GameEngineFacade {

    private final SetupManager setupManager;
    private final TurnManager turnManager;
    private final RuleValidator ruleValidator;
    private final VictoryConditionChecker victoryChecker;
    private final StatusEffectManager statusEffectManager;
    private final Map<GameState, GameStateHandler> stateHandlers;

    public GameEngineFacadeImpl(SetupManager setupManager,
                                TurnManager turnManager,
                                RuleValidator ruleValidator,
                                VictoryConditionChecker victoryChecker,
                                StatusEffectManager statusEffectManager,
                                List<GameStateHandler> handlers) {
        this.setupManager        = setupManager;
        this.turnManager         = turnManager;
        this.ruleValidator       = ruleValidator;
        this.victoryChecker      = victoryChecker;
        this.statusEffectManager = statusEffectManager;

        // Build a map of GameState → handler for O(1) dispatch
        this.stateHandlers = new EnumMap<>(GameState.class);
        for (GameStateHandler handler : handlers) {
            this.stateHandlers.put(handler.getState(), handler);
        }
    }

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
                .firstPlayerId(firstPlayerId)
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
     * Processes a player action by delegating to the appropriate
     * {@link GameStateHandler} based on the current game state.
     *
     * <p>The facade validates the action first, then dispatches to the
     * correct handler. After the handler resolves the new state, victory
     * conditions are checked and pending events are collected.
     *
     * <p>This is the core of the State pattern: the facade does not know
     * how each state handles actions — it only knows which handler to call.
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

        // Dispatch to the correct state handler
        GameStateHandler handler = stateHandlers.get(currentState.getGameState());
        if (handler == null) {
            return EngineResult.of(currentState, List.of());
        }

        EngineResult handlerResult = handler.handle(currentState, action);
        BoardState newState = handlerResult.newState();

        // Collect pending events generated inside the handler
        List<GameEvent> events = new ArrayList<>();
        if (handlerResult.events() != null) {
            events.addAll(handlerResult.events());
        }
        if (newState.getPendingEvents() != null && !newState.getPendingEvents().isEmpty()) {
            events.addAll(newState.getPendingEvents());
            newState = newState.toBuilder()
                    .pendingEvents(new ArrayList<>())
                    .build();
        }

        // Check victory conditions only if the game is still in progress.
        // Skipping this when already FINISHED avoids emitting duplicate GAME_OVER events
        // when a player attempts to act after the game has ended.
        if (newState.getGameState() != GameState.FINISHED) {
            Optional<GameEvent> victoryEvent = victoryChecker.check(newState);
            if (victoryEvent.isPresent()) {
                events.add(victoryEvent.get());
                newState = newState.toBuilder()
                        .gameState(GameState.FINISHED)
                        .build();
            }
        }

        return EngineResult.of(newState, events);
    }
}