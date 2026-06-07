package com.pokemon.tcg.domain.engine;

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
        // Shuffle decks
        setupManager.shuffleDeck(player1);
        setupManager.shuffleDeck(player2);

        // Draw initial hands
        setupManager.drawInitialHand(player1);
        setupManager.drawInitialHand(player2);

        // Set up prizes
        setupManager.setupPrizes(player1);
        setupManager.setupPrizes(player2);

        // Determine first player
        String firstPlayerId = setupManager.determineFirstPlayer(
                player1.getPlayerId(), player2.getPlayerId());

        // Build initial board state
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

        // Build game started event
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

        // Check victory
        List<GameEvent> events = new ArrayList<>();
        victoryChecker.check(newState).ifPresent(events::add);

        return EngineResult.of(newState, events);
    }
}