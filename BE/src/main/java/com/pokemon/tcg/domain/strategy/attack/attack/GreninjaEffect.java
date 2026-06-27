package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Greninja — Mist Slash (50 damage).
 * This attack's damage isn't affected by Weakness, Resistance, or any
 * other effects on the opponent's Active Pokémon.
 *
 * <p>Sets {@code ignoreDefenderEffects} on the context so that
 * DamageApplicationStep skips weakness, resistance and active effects.
 */
@Component
public class GreninjaEffect implements AttackEffect {

    private static final String MIST_SLASH = "mist slash";

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("greninja|mist slash");
    }

    @Override
    public void apply(AttackContext ctx) {
        if (!MIST_SLASH.equals(ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase() : "")) return;

        ctx.setIgnoreDefenderEffects(true);
    }
}