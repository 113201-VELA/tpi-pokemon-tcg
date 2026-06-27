package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import com.pokemon.tcg.domain.strategy.attack.DamageModifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RaichuEffect implements AttackEffect {

    private static final String CIRCLE_CIRCUIT = "circle circuit";
    private static final String THUNDERBOLT    = "thunderbolt";
    private static final int    DAMAGE_PER_BENCH = 20;

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("raichu|circle circuit", "raichu|thunderbolt");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        switch (attackName) {
            case CIRCLE_CIRCUIT -> applyCircleCircuit(ctx);
            case THUNDERBOLT    -> applyThunderbolt(ctx);
            default             -> { }
        }
    }

    /**
     * Circle Circuit: does 20 damage times the number of the attacker's Benched Pokémon.
     * The base damage on the card is "20×" (treated as 0), so we set the full damage
     * as a pre-weakness modifier.
     */
    private void applyCircleCircuit(AttackContext ctx) {
        String attackerId    = ctx.getAction().getPlayerId();
        PlayerState attacker = ctx.getBoardState().getStateFor(attackerId);

        int benchCount = attacker.getBench() != null ? attacker.getBench().size() : 0;
        if (benchCount == 0) return;

        List<DamageModifier> modifiers = new ArrayList<>(
                ctx.getModifiers() != null ? ctx.getModifiers() : new ArrayList<>());
        modifiers.add(new DamageModifier(
                "circle-circuit-bench", benchCount * DAMAGE_PER_BENCH, true));
        ctx.setModifiers(modifiers);
    }

    /**
     * Thunderbolt: discard all Energy attached to Raichu.
     */
    private void applyThunderbolt(AttackContext ctx) {
        String attackerId    = ctx.getAction().getPlayerId();
        PlayerState attacker = ctx.getBoardState().getStateFor(attackerId);
        ActivePokemon raichu = attacker.getActivePokemon();

        if (raichu == null) return;

        List<String> energies = raichu.getAttachedEnergyIds();
        if (energies == null || energies.isEmpty()) return;

        List<String> discard = new ArrayList<>(
                attacker.getDiscard() != null ? attacker.getDiscard() : new ArrayList<>());
        discard.addAll(energies);
        attacker.setDiscard(discard);

        raichu.setAttachedEnergyIds(new ArrayList<>());
    }
}