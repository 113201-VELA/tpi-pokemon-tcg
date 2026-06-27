package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ChesnaughtEffect implements AttackEffect {

    private static final int HEAL_COUNTERS = 2;

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("chesnaught|touchdown");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackerId         = ctx.getAction().getPlayerId();
        PlayerState attackerState = ctx.getBoardState().getStateFor(attackerId);
        ActivePokemon chesnaught  = attackerState.getActivePokemon();

        if (chesnaught == null) return;

        int current = chesnaught.getDamageCounters();
        chesnaught.setDamageCounters(Math.max(0, current - HEAL_COUNTERS));
    }
}
