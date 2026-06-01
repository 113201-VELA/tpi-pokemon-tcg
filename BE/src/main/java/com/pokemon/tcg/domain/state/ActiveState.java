package com.pokemon.tcg.domain.state;

//import com.pokemon.tcg.domain.engine.AttackPipeline;
import com.pokemon.tcg.domain.engine.attack.AttackPipeline;
import com.pokemon.tcg.domain.engine.TurnManager;
import com.pokemon.tcg.domain.model.game.*;
import org.springframework.stereotype.Component;

@Component
public class ActiveState implements GameStateHandler {

    private final TurnManager turnManager;
    private final AttackPipeline attackPipeline;

    public ActiveState(TurnManager turnManager, AttackPipeline attackPipeline) {
        this.turnManager = turnManager;
        this.attackPipeline = attackPipeline;
    }

    @Override
    public EngineResult handle(BoardState state, GameAction action) {
        return EngineResult.of(state, java.util.List.of());
    }

    @Override
    public GameState getState() {
        return GameState.ACTIVE;
    }
}
