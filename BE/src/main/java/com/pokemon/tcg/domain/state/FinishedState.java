package com.pokemon.tcg.domain.state;

import com.pokemon.tcg.domain.model.game.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class FinishedState implements GameStateHandler {

    @Override
    public EngineResult handle(BoardState state, GameAction action) {
        return new EngineResult(state, List.of(
            GameEvent.builder()
                .type(GameEventType.GAME_OVER)
                .data(Map.of("error", "La partida ya ha finalizado"))
                .build()
        ));
    }

    @Override
    public GameState getState() {
        return GameState.FINISHED;
    }
}
