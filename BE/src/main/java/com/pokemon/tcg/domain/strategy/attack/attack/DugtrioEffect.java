package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * xy1-59 Dugtrio
 *
 * Earthquake: 60 damage to the opponent's Active; 10 damage to each of
 *             your own Benched Pokémon (direct counters, no Weakness/Resistance).
 * Rock Tumble: 60 damage not affected by Resistance.
 */
@Component
public class DugtrioEffect implements AttackEffect {

    private static final String EARTHQUAKE  = "earthquake";
    private static final String ROCK_TUMBLE = "rock tumble";

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("dugtrio|earthquake", "dugtrio|rock tumble");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        switch (attackName) {
            case EARTHQUAKE  -> applyEarthquake(ctx);
            case ROCK_TUMBLE -> applyRockTumble(ctx);
            default          -> { }
        }
    }

    /**
     * Earthquake: place 1 damage counter on each of the attacker's own
     * Benched Pokémon (10 damage each, direct — not affected by Weakness
     * or Resistance for Benched Pokémon).
     * The 60 damage to the opponent's Active is handled by the pipeline.
     */
    private void applyEarthquake(AttackContext ctx) {
        String attackerId    = ctx.getAction().getPlayerId();
        PlayerState attacker = ctx.getBoardState().getStateFor(attackerId);

        List<BenchPokemon> bench = attacker.getBench();
        if (bench == null) return;

        for (BenchPokemon bp : bench) {
            bp.setDamageCounters(bp.getDamageCounters() + 1);
        }
    }

    /**
     * Rock Tumble: damage is not affected by Resistance.
     * Setting ignoreDefenderEffects skips the Resistance step in DamageApplicationStep.
     */
    private void applyRockTumble(AttackContext ctx) {
        ctx.setIgnoreDefenderEffects(true);
    }
}