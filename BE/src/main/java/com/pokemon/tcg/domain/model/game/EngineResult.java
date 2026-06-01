package com.pokemon.tcg.domain.model.game;

import java.util.List;

public record EngineResult(BoardState newState, List<GameEvent> events) {
    public static EngineResult of(BoardState state, List<GameEvent> events) {
        return new EngineResult(state, events);
    }
}
