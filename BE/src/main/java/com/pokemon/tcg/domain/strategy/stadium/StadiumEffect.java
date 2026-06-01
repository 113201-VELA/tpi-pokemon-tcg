package com.pokemon.tcg.domain.strategy.stadium;

import com.pokemon.tcg.domain.model.game.BoardState;
import com.pokemon.tcg.domain.model.game.EngineResult;
import com.pokemon.tcg.domain.model.game.GameAction;
import com.pokemon.tcg.domain.model.game.ValidationResult;
import com.pokemon.tcg.domain.strategy.TrainerEffect;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StadiumEffect implements TrainerEffect {

    @Override
    public EngineResult apply(BoardState state, GameAction action) {
        return EngineResult.of(state, List.of());
    }

    @Override
    public ValidationResult canApply(BoardState state, GameAction action) {
        return ValidationResult.ok();
    }
}
