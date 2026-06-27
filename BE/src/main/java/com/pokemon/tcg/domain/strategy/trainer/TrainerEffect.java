package com.pokemon.tcg.domain.strategy.trainer;

import com.pokemon.tcg.domain.model.game.BoardState;
import com.pokemon.tcg.domain.model.game.EngineResult;
import com.pokemon.tcg.domain.model.game.GameAction;
import com.pokemon.tcg.domain.model.game.ValidationResult;

public interface TrainerEffect {

    EngineResult apply(BoardState state, GameAction action);

    ValidationResult canApply(BoardState state, GameAction action);

    String getCardIdentifier();
}
