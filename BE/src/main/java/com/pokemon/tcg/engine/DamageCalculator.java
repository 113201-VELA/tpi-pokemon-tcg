package com.pokemon.tcg.engine;

import com.pokemon.tcg.domain.model.game.ActivePokemon;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DamageCalculator {

    public int calculate(ActivePokemon attacker, ActivePokemon defender,
                         int baseDamage, List<DamageModifier> modifiers) {
        if (baseDamage == 0) return 0;

        int damage = baseDamage;

        return Math.max(0, damage);
    }

    public int toCounters(int damage) {
        return damage / 10;
    }
}
