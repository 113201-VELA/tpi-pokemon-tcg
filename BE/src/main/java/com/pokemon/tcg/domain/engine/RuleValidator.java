package com.pokemon.tcg.domain.engine;

import com.pokemon.tcg.domain.model.game.BoardState;
import com.pokemon.tcg.domain.model.game.GameAction;
import com.pokemon.tcg.domain.model.game.ValidationResult;
import org.springframework.stereotype.Component;

@Component
public class RuleValidator {

    public ValidationResult validate(BoardState state, GameAction action) {
        return switch (action.getType()) {
            case PLACE_BASIC_POKEMON -> validatePlaceBasicPokemon(state, action);
            case EVOLVE_POKEMON      -> validateEvolution(state, action);
            case ATTACH_ENERGY       -> validateAttachEnergy(state, action);
            case PLAY_TRAINER        -> validatePlayTrainer(state, action);
            case RETREAT             -> validateRetreat(state, action);
            case DECLARE_ATTACK      -> validateAttack(state, action);
            default                  -> ValidationResult.ok();
        };
    }

    private ValidationResult validatePlaceBasicPokemon(BoardState state, GameAction action) {
        return ValidationResult.ok();
    }

    private ValidationResult validateEvolution(BoardState state, GameAction action) {
        return ValidationResult.ok();
    }

    private ValidationResult validateAttachEnergy(BoardState state, GameAction action) {
        return ValidationResult.ok();
    }

    private ValidationResult validatePlayTrainer(BoardState state, GameAction action) {
        return ValidationResult.ok();
    }

    private ValidationResult validateRetreat(BoardState state, GameAction action) {
        return ValidationResult.ok();
    }

    private ValidationResult validateAttack(BoardState state, GameAction action) {
        return ValidationResult.ok();
    }
}
