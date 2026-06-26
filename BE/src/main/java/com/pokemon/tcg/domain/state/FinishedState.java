package com.pokemon.tcg.domain.state;

import com.pokemon.tcg.domain.model.game.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Handles the FINISHED state — the game has ended.
 * All actions are rejected; the game cannot be continued.
 */
@Component
public class FinishedState implements GameStateHandler {

    @Override
    public EngineResult handle(BoardState state, GameAction action) {
        return EngineResult.of(state, List.of(
                GameEvent.builder()
                        .type(GameEventType.TURN_ENDED)
                        .gameId(state.getGameId())
                        .playerId(action.getPlayerId())
                        .turnNumber(state.getTurnNumber())
                        .data(Map.of("error", "The game has already finished."))
                        .occurredAt(Instant.now())
                        .build()
        ));
    }

    @Override
    public GameState getState() {
        return GameState.FINISHED;
    }
}