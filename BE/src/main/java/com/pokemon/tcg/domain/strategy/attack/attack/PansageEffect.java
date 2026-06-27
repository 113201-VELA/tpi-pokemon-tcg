package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PansageEffect implements AttackEffect {

    private static final int HEAL_COUNTERS = 1;

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("pansage|leech seed");
    }

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
