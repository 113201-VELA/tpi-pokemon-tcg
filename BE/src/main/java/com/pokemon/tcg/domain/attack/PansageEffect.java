package com.pokemon.tcg.domain.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.engine.attack.AttackContext;
import com.pokemon.tcg.engine.attack.AttackEffect;
import org.springframework.stereotype.Component;

@Component
public class PansageEffect implements AttackEffect {

    private static final int HEAL_COUNTERS = 1;

    @Override
    public void apply(AttackContext ctx) {
        String attackerId     = ctx.getAction().getPlayerId();
        PlayerState attackerState = ctx.getBoardState().getStateFor(attackerId);
        ActivePokemon pansage = attackerState.getActivePokemon();

        if (pansage == null) return;

        int current = pansage.getDamageCounters();
        pansage.setDamageCounters(Math.max(0, current - HEAL_COUNTERS));
    }
}
