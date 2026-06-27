package com.pokemon.tcg.domain.strategy.ability;

import com.pokemon.tcg.domain.model.game.BoardState;
import com.pokemon.tcg.domain.model.game.GameAction;
import com.pokemon.tcg.domain.model.game.ValidationResult;

public interface ActiveAbilityEffect {

    ValidationResult canApply(BoardState state, GameAction action);

    BoardState apply(BoardState state, GameAction action);

    String getIdentifier();
}
