package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import com.pokemon.tcg.domain.strategy.attack.DamageModifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * xy1-79 Yveltal-EX
 *
 * Evil Ball: 20+ damage. Does 20 more damage times the amount of Energy
 *            attached to both Active Pokémon (attacker's and defender's).
 * Y Cyclone: 90 damage. Move an Energy from this Pokémon to 1 of your
 *            Benched Pokémon.
 */
@Component
public class YveltalExEffect implements AttackEffect {

    private static final String EVIL_BALL = "evil ball";
    private static final String Y_CYCLONE = "y cyclone";

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("yveltal-ex|evil ball", "yveltal-ex|y cyclone");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        switch (attackName) {
            case EVIL_BALL -> applyEvilBall(ctx);
            case Y_CYCLONE -> applyYCyclone(ctx);
            default        -> { }
        }
    }

    /**
     * Evil Ball: add 20 damage for every Energy card attached to either
     * Active Pokémon (attacker's and defender's combined), regardless of type.
     */
    private void applyEvilBall(AttackContext ctx) {
        String attackerId    = ctx.getAction().getPlayerId();
        PlayerState attacker = ctx.getBoardState().getStateFor(attackerId);
        PlayerState defender = ctx.getBoardState().getOpponentState(attackerId);

        int totalEnergy = countEnergies(attacker.getActivePokemon())
                + countEnergies(defender.getActivePokemon());

        if (totalEnergy == 0) return;

        List<DamageModifier> modifiers = new ArrayList<>(
                ctx.getModifiers() != null ? ctx.getModifiers() : new ArrayList<>());
        modifiers.add(new DamageModifier("evil-ball-energy-count", totalEnergy * 20, true));
        ctx.setModifiers(modifiers);
    }

    private int countEnergies(ActivePokemon pokemon) {
        if (pokemon == null || pokemon.getAttachedEnergyIds() == null) return 0;
        return pokemon.getAttachedEnergyIds().size();
    }

    /**
     * Y Cyclone: move one Energy from Yveltal-EX to one of the attacker's
     * own Benched Pokémon.
     * <p>
     * Payload: {@code energyCardId} — the energy to move,
     * {@code targetBenchInstanceId} — the bench Pokémon to receive it.
     */
    private void applyYCyclone(AttackContext ctx) {
        String energyCardId          = ctx.getAction().getPayloadString("energyCardId");
        String targetBenchInstanceId = ctx.getAction().getPayloadString("targetBenchInstanceId");

        if (energyCardId == null || targetBenchInstanceId == null) return;

        String attackerId     = ctx.getAction().getPlayerId();
        PlayerState attacker  = ctx.getBoardState().getStateFor(attackerId);
        ActivePokemon yveltal = attacker.getActivePokemon();

        if (yveltal == null) return;
        if (yveltal.getAttachedEnergyIds() == null
                || !yveltal.getAttachedEnergyIds().contains(energyCardId)) return;

        BenchPokemon target = null;
        if (attacker.getBench() != null) {
            target = attacker.getBench().stream()
                    .filter(b -> b.getInstanceId().equals(targetBenchInstanceId))
                    .findFirst().orElse(null);
        }
        if (target == null) return;

        // Remove from Yveltal-EX
        List<String> activeEnergies = new ArrayList<>(yveltal.getAttachedEnergyIds());
        activeEnergies.remove(energyCardId);
        yveltal.setAttachedEnergyIds(activeEnergies);

        // Attach to the chosen Benched Pokémon
        List<String> benchEnergies = new ArrayList<>(
                target.getAttachedEnergyIds() != null
                        ? target.getAttachedEnergyIds() : new ArrayList<>());
        benchEnergies.add(energyCardId);
        target.setAttachedEnergyIds(benchEnergies);
    }
}