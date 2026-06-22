package com.pokemon.tcg.domain.state;

import com.pokemon.tcg.engine.SetupManager;
import com.pokemon.tcg.domain.model.game.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SetupState implements GameStateHandler {

    private final SetupManager setupManager;

    public SetupState(SetupManager setupManager) {
        this.setupManager = setupManager;
    }

    @Override
    public EngineResult handle(BoardState state, GameAction action) {
        return switch (action.getType()) {
            case MULLIGAN_CONFIRM -> EngineResult.of(state, List.of());
            case SETUP_PLACE_ACTIVE -> EngineResult.of(state, List.of());
            case SETUP_PLACE_BENCH -> EngineResult.of(state, List.of());
            default -> new EngineResult(state, List.of(
                GameEvent.builder()
                    .type(GameEventType.GAME_OVER)
                    .data(Map.of("error", "Acción no válida en estado SETUP"))
                    .build()
            ));
        };
    }

    @Override
    public GameState getState() {
        return GameState.SETUP;
    }
}
