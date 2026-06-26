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
                 SETUP_PLACE_BENCH,
                 ACCEPT_MULLIGAN_BONUS -> {
                BoardState newState = turnManager.advancePhase(state, action);
                yield EngineResult.of(newState, List.of());
            }
            case SETUP_PLACE_ACTIVE -> {
                BoardState newState = turnManager.advancePhase(state, action);

                // Both players have placed their Active Pokémon — setup placement done.
                // Transition responsibility moved here from TurnManager as part of State pattern.
                if (newState.getPlayer1State().getActivePokemon() != null &&
                        newState.getPlayer2State().getActivePokemon() != null) {

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