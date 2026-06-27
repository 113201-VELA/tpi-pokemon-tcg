package com.pokemon.tcg.domain.strategy.trainer.stadium;

import com.pokemon.tcg.domain.model.game.BoardState;
import com.pokemon.tcg.domain.model.game.EngineResult;
import com.pokemon.tcg.domain.model.game.GameAction;
import com.pokemon.tcg.domain.model.game.ValidationResult;
import com.pokemon.tcg.domain.strategy.trainer.TrainerEffect;
import org.springframework.stereotype.Component;

@Component
public class ShadowCircleEffect implements TrainerEffect {

    @Override
    public String getCardIdentifier() {
        return "shadow circle";
    }

    @Override
    public EngineResult apply(BoardState state, GameAction action) {
        state.setActiveStadiumCardId("shadow circle");
        return EngineResult.of(state, java.util.List.of());
    }

    @Override
    public ValidationResult canApply(BoardState state, GameAction action) {
        return ValidationResult.ok();
    }
}
