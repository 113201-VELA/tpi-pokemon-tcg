package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MVenusaurExEffect implements AttackEffect {

    private static final String CRISIS_VINE = "crisis vine";

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("m venusaur-ex|crisis vine");
    }

    @Override
    public void apply(AttackContext ctx) {
        if (!CRISIS_VINE.equals(ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase() : "")) return;

        String attackerId     = ctx.getAction().getPlayerId();
        PlayerState opponent  = ctx.getBoardState().getOpponentState(attackerId);
        ActivePokemon defender = opponent.getActivePokemon();

        if (defender == null) return;

        defender.getConditions().remove(SpecialCondition.ASLEEP);
        defender.getConditions().remove(SpecialCondition.CONFUSED);
        defender.getConditions().add(SpecialCondition.PARALYZED);

        defender.getConditions().add(SpecialCondition.POISONED);
    }
}
