package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import com.pokemon.tcg.domain.strategy.attack.DamageModifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * xy1-64 Solrock
 *
 * Cosmic Spin: 10 damage + 30 more if Lunatone is on your Bench.
 * Solar Beam: 60 damage. No additional effect.
 */
@Component
public class SolrockEffect implements AttackEffect {

    private static final String COSMIC_SPIN  = "cosmic spin";
    private static final String SOLAR_BEAM   = "solar beam";
    private static final String LUNATONE_ID  = "xy1-63";
    private static final int    LUNATONE_BONUS = 30;

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("solrock|cosmic spin", "solrock|solar beam");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        switch (attackName) {
            case COSMIC_SPIN -> applyCosmicSpin(ctx);
            case SOLAR_BEAM  -> { } // 60 damage handled by pipeline — no extra effect
            default          -> { }
        }
    }

    /**
     * Cosmic Spin: if Lunatone (xy1-63) is on the attacker's Bench,
     * add 30 damage via a pre-weakness modifier.
     */
    private void applyCosmicSpin(AttackContext ctx) {
        String attackerId    = ctx.getAction().getPlayerId();
        PlayerState attacker = ctx.getBoardState().getStateFor(attackerId);

        List<BenchPokemon> bench = attacker.getBench();
        if (bench == null || bench.isEmpty()) return;

        boolean lunatoneOnBench = bench.stream()
                .anyMatch(bp -> LUNATONE_ID.equals(bp.getCardId()));

        if (!lunatoneOnBench) return;

        List<DamageModifier> modifiers = new ArrayList<>(
                ctx.getModifiers() != null ? ctx.getModifiers() : new ArrayList<>());
        modifiers.add(new DamageModifier("cosmic-spin-lunatone", LUNATONE_BONUS, true));
        ctx.setModifiers(modifiers);
    }
}