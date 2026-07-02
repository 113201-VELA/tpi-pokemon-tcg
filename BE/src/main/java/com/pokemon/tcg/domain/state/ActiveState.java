package com.pokemon.tcg.domain.state;

import com.pokemon.tcg.engine.TurnManager;
import com.pokemon.tcg.domain.model.game.*;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Handles all actions valid during the ACTIVE phase (normal gameplay).
 *
 * <p>Delegates entirely to {@link TurnManager} for action execution.
 * This state does not contain game logic — it acts as a dispatcher
 * that ensures only ACTIVE-phase actions reach the turn manager.
 *
 * <p>Actions not valid during ACTIVE (e.g. MULLIGAN_CONFIRM, SETUP_PLACE_ACTIVE)
 * are blocked here with a descriptive error event.
 */
@Component
public class ActiveState implements GameStateHandler {

    private final TurnManager turnManager;

    public ActiveState(TurnManager turnManager) {
        this.turnManager = turnManager;
    }

    @Override
    public EngineResult handle(BoardState state, GameAction action) {
        return switch (action.getType()) {
            case DRAW_CARD,
                 PLACE_BASIC_POKEMON,
                 EVOLVE_POKEMON,
                 ATTACH_ENERGY,
                 PLAY_TRAINER,
                 USE_ABILITY,
                 RETREAT,
                 DECLARE_ATTACK,
                 END_TURN,
                 CHOOSE_BENCH_POKEMON,
                 TAKE_PRIZE -> {
                BoardState newState = turnManager.advancePhase(state, action);
                yield EngineResult.of(newState, List.of());
            }
            default -> EngineResult.of(state, List.of(
                    GameEvent.builder()
                            .type(GameEventType.TURN_ENDED)
                            .gameId(state.getGameId())
                            .playerId(action.getPlayerId())
                            .turnNumber(state.getTurnNumber())
                            .data(java.util.Map.of("error",
                                    "Action not valid during active game: " + action.getType()))
                            .occurredAt(java.time.Instant.now())
                            .build()
            ));
        };
    }

    @Override
    public GameState getState() {
        return GameState.ACTIVE;
    }
}