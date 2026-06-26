package com.pokemon.tcg.domain.state;

import com.pokemon.tcg.engine.TurnManager;
import com.pokemon.tcg.domain.model.game.*;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Handles all actions valid during the SETUP phase:
 * mulligan, placing Active and Bench Pokémon, and accepting mulligan bonus draws.
 *
 * <p>This state is responsible for detecting when setup is complete and
 * transitioning the game to ACTIVE state.
 *
 * <p>All action execution is delegated to {@link TurnManager}, which contains
 * the actual mutation logic. SetupState only decides whether the action is
 * valid for this state and whether a state transition should occur after it.
 */
@Component
public class SetupState implements GameStateHandler {

    private final TurnManager turnManager;

    public SetupState(TurnManager turnManager) {
        this.turnManager = turnManager;
    }

    @Override
    public EngineResult handle(BoardState state, GameAction action) {
        return switch (action.getType()) {
            case MULLIGAN_CONFIRM,
                 SETUP_PLACE_ACTIVE,
                 SETUP_PLACE_BENCH -> {
                BoardState newState = turnManager.advancePhase(state, action);
                yield EngineResult.of(newState, List.of());
            }
            case ACCEPT_MULLIGAN_BONUS -> {
                BoardState newState = turnManager.advancePhase(state, action);
                yield EngineResult.of(newState, List.of());
            }
            case CONFIRM_SETUP -> {
                BoardState newState = turnManager.advancePhase(state, action);

                boolean bothConfirmed =
                        newState.getPlayer1State().isSetupConfirmed() &&
                                newState.getPlayer2State().isSetupConfirmed();

                if (bothConfirmed) {
                    // Both players confirmed — check for pending bonus draws first
                    if (newState.hasAnyPendingBonus()) {
                        newState = newState.toBuilder()
                                .bonusDrawPending(true)
                                .build();
                    } else {
                        newState = newState.toBuilder()
                                .turnPhase(TurnPhase.DRAW)
                                .gameState(GameState.ACTIVE)
                                .build();
                    }
                }
                yield EngineResult.of(newState, List.of());
            }
            default -> EngineResult.of(state, List.of(
                    GameEvent.builder()
                            .type(GameEventType.TURN_ENDED)
                            .gameId(state.getGameId())
                            .playerId(action.getPlayerId())
                            .turnNumber(state.getTurnNumber())
                            .data(java.util.Map.of("error",
                                    "Action not valid during setup: " + action.getType()))
                            .occurredAt(java.time.Instant.now())
                            .build()
            ));
        };
    }

    @Override
    public GameState getState() {
        return GameState.SETUP;
    }
}