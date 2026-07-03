package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * xy1-95 Slurpuff
 *
 * Draining Kiss: 30 damage. Heal 30 damage from this Pokémon
 *                (self-heal after dealing damage).
 */
@Component
public class SlurpuffEffect implements AttackEffect {

    private static final String DRAINING_KISS = "draining kiss";
    private static final int    HEAL_AMOUNT   = 30;
    private static final int    COUNTERS_HEAL = HEAL_AMOUNT / 10;

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("slurpuff|draining kiss");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        if (DRAINING_KISS.equals(attackName)) {
            applyDrainingKiss(ctx);
        }
    }

    /**
     * Heal 30 damage (3 counters) from Slurpuff itself, minimum 0.
     */
    private void applyDrainingKiss(AttackContext ctx) {
        String attackerId       = ctx.getAction().getPlayerId();
        PlayerState attacker    = ctx.getBoardState().getStateFor(attackerId);
        ActivePokemon slurpuff  = attacker.getActivePokemon();

        if (slurpuff == null) return;

        slurpuff.setDamageCounters(Math.max(0, slurpuff.getDamageCounters() - COUNTERS_HEAL));
    }
}