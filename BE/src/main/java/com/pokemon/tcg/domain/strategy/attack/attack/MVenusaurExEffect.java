package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import com.pokemon.tcg.engine.StatusEffectManager;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MVenusaurExEffect implements AttackEffect {

    private static final String CRISIS_VINE = "crisis vine";

    private final StatusEffectManager statusEffectManager;

    public MVenusaurExEffect(StatusEffectManager statusEffectManager) {
        this.statusEffectManager = statusEffectManager;
    }

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

        statusEffectManager.applyCondition(defender, SpecialCondition.PARALYZED);
        statusEffectManager.applyCondition(defender, SpecialCondition.POISONED);
    }
}
