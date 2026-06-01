package com.pokemon.tcg.domain.engine;

import com.pokemon.tcg.domain.model.game.BoardState;
import com.pokemon.tcg.domain.model.game.GameAction;
import com.pokemon.tcg.domain.model.game.GameActionType;
import com.pokemon.tcg.domain.model.game.TurnPhase;
import com.pokemon.tcg.domain.model.game.ValidationResult;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class TurnManager {

    private final RuleValidator ruleValidator;

    public TurnManager(RuleValidator ruleValidator) {
        this.ruleValidator = ruleValidator;
    }

    public BoardState advancePhase(BoardState state, GameAction action) {
        return state;
    }

    public ValidationResult validateActionForPhase(BoardState state, GameAction action) {
        return ValidationResult.ok();
    }

    public Set<GameActionType> getAvailableActions(BoardState state) {
        return Set.of();
    }
}
