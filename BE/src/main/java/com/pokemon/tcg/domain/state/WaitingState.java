package com.pokemon.tcg.domain.state;

import com.pokemon.tcg.domain.model.game.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Handles the WAITING state — the game is waiting for a second player to join.
 * All actions except JOIN_GAME are rejected in this state.
 */
@Component
public class WaitingState implements GameStateHandler {

    @Override
    public EngineResult handle(BoardState state, GameAction action) {
        if (action.getType() != GameActionType.JOIN_GAME) {
            return EngineResult.of(state, List.of(
                    GameEvent.builder()
                            .type(GameEventType.TURN_ENDED)
                            .gameId(state.getGameId())
                            .playerId(action.getPlayerId())
                            .turnNumber(state.getTurnNumber())
                            .data(Map.of("error", "Game has not started yet."))
                            .occurredAt(Instant.now())
                            .build()
            ));
        }
        return EngineResult.of(state, List.of());
    }

    @Override
    public GameState getState() {
        return GameState.WAITING;
    }
}