package com.pokemon.tcg.domain.state;

import com.pokemon.tcg.domain.model.game.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class WaitingState implements GameStateHandler {

    @Override
    public EngineResult handle(BoardState state, GameAction action) {
        if (action.getType() != GameActionType.JOIN_GAME) {
            return new EngineResult(state, List.of(
                GameEvent.builder()
                    .type(GameEventType.GAME_OVER)
                    .data(Map.of("error", "Acción no válida en estado WAITING"))
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
