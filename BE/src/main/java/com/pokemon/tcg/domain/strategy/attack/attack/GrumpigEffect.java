package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import com.pokemon.tcg.engine.StatusEffectManager;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class GrumpigEffect implements AttackEffect {

    private static final String TRICKY_STEPS = "tricky steps";
    private static final String PSYBEAM      = "psybeam";

    private final StatusEffectManager statusEffectManager;

    public GrumpigEffect(StatusEffectManager statusEffectManager) {
        this.statusEffectManager = statusEffectManager;
    }

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("grumpig|tricky steps", "grumpig|psybeam");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        switch (attackName) {
            case TRICKY_STEPS -> applyTrickySteps(ctx);
            case PSYBEAM      -> applyPsybeam(ctx);
            default           -> { }
        }
    }

    /**
     * Tricky Steps: optionally move one Energy from the opponent's Active
     * Pokémon to one of their Benched Pokémon.
     *
     * <p>Payload: {@code energyCardId} — the energy to move,
     * {@code targetBenchInstanceId} — the bench Pokémon to receive it.
     */
    private void applyTrickySteps(AttackContext ctx) {
        String energyCardId         = ctx.getAction().getPayloadString("energyCardId");
        String targetBenchInstanceId = ctx.getAction().getPayloadString("targetBenchInstanceId");

        if (energyCardId == null || targetBenchInstanceId == null) return;

        String attackerId    = ctx.getAction().getPlayerId();
        PlayerState opponent = ctx.getBoardState().getOpponentState(attackerId);
        ActivePokemon active = opponent.getActivePokemon();

        if (active == null) return;
        if (active.getAttachedEnergyIds() == null
                || !active.getAttachedEnergyIds().contains(energyCardId)) return;

        BenchPokemon target = null;
        if (opponent.getBench() != null) {
            target = opponent.getBench().stream()
                    .filter(b -> b.getInstanceId().equals(targetBenchInstanceId))
                    .findFirst().orElse(null);
        }
        if (target == null) return;

        // Remove from active
        List<String> activeEnergies = new ArrayList<>(active.getAttachedEnergyIds());
        activeEnergies.remove(energyCardId);
        active.setAttachedEnergyIds(activeEnergies);

        // Attach to bench target
        List<String> benchEnergies = new ArrayList<>(
                target.getAttachedEnergyIds() != null
                        ? target.getAttachedEnergyIds() : new ArrayList<>());
        benchEnergies.add(energyCardId);
        target.setAttachedEnergyIds(benchEnergies);
    }

    /**
     * Psybeam: the opponent's Active Pokémon is now Confused.
     * Confused replaces Asleep and Paralyzed (rotation conditions).
     */
    private void applyPsybeam(AttackContext ctx) {
        String attackerId    = ctx.getAction().getPlayerId();
        PlayerState opponent = ctx.getBoardState().getOpponentState(attackerId);
        ActivePokemon defender = opponent.getActivePokemon();

        if (defender == null) return;

        statusEffectManager.applyCondition(defender, SpecialCondition.CONFUSED);
    }
}