package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import com.pokemon.tcg.domain.strategy.attack.DamageModifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * xy1-90 Wigglytuff
 *
 * Balloon Barrage: 20+ damage. Does 20 damage times the amount of Energy
 *                  attached to this Pokémon. Printed base damage is 0 —
 *                  same pattern as Rhyperior's Rock Blast / Yveltal-EX's
 *                  Evil Ball.
 * Double-Edge: 90 damage. This Pokémon does 10 damage to itself
 *              (same recoil pattern as Staryu's Reckless Charge).
 */
@Component
public class Wigglytuff90Effect implements AttackEffect {

    private static final String BALLOON_BARRAGE = "balloon barrage";
    private static final String DOUBLE_EDGE     = "double-edge";
    private static final int    DAMAGE_PER_ENERGY = 20;
    // 10 damage to itself = 1 damage counter
    private static final int    SELF_DAMAGE_COUNTERS = 1;

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("wigglytuff|balloon barrage", "wigglytuff|double-edge");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        switch (attackName) {
            case BALLOON_BARRAGE -> applyBalloonBarrage(ctx);
            case DOUBLE_EDGE     -> applyDoubleEdge(ctx);
            default              -> { }
        }
    }

    /**
     * Add 20 damage for every Energy card attached to Wigglytuff itself.
     */
    private void applyBalloonBarrage(AttackContext ctx) {
        String attackerId       = ctx.getAction().getPlayerId();
        PlayerState attacker    = ctx.getBoardState().getStateFor(attackerId);
        ActivePokemon wigglytuff = attacker.getActivePokemon();

        int energyCount = countEnergies(wigglytuff);
        if (energyCount == 0) return;

        List<DamageModifier> modifiers = new ArrayList<>(
                ctx.getModifiers() != null ? ctx.getModifiers() : new ArrayList<>());
        modifiers.add(new DamageModifier("balloon-barrage-energy-count",
                energyCount * DAMAGE_PER_ENERGY, true));
        ctx.setModifiers(modifiers);
    }

    private int countEnergies(ActivePokemon pokemon) {
        if (pokemon == null || pokemon.getAttachedEnergyIds() == null) return 0;
        return pokemon.getAttachedEnergyIds().size();
    }

    /**
     * 10 damage to itself = 1 damage counter, applied directly to the
     * attacker's own Active Pokémon.
     */
    private void applyDoubleEdge(AttackContext ctx) {
        String attackerId        = ctx.getAction().getPlayerId();
        PlayerState attacker     = ctx.getBoardState().getStateFor(attackerId);
        ActivePokemon wigglytuff = attacker.getActivePokemon();

        if (wigglytuff == null) return;

        wigglytuff.setDamageCounters(wigglytuff.getDamageCounters() + SELF_DAMAGE_COUNTERS);
    }
}