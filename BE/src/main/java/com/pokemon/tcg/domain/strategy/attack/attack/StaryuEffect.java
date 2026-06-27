package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StaryuEffect implements AttackEffect {

    // 10 damage to itself = 1 damage counter
    private static final int SELF_DAMAGE_COUNTERS = 1;

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("staryu|reckless charge");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackerId    = ctx.getAction().getPlayerId();
        PlayerState attacker = ctx.getBoardState().getStateFor(attackerId);
        ActivePokemon self   = attacker.getActivePokemon();

        if (self == null) return;

        self.setDamageCounters(self.getDamageCounters() + SELF_DAMAGE_COUNTERS);
    }
}