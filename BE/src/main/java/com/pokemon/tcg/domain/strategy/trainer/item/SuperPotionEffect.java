package com.pokemon.tcg.domain.strategy.trainer.item;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.trainer.TrainerEffect;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Strategy implementation for Super Potion (xy1-128).
 * Effect: Heal 60 damage from one of your Pokémon.
 * If you do, discard an Energy attached to that Pokémon.
 *
 * <p>Payload expected:
 * <ul>
 *   <li>{@code targetInstanceId} — instanceId of the Pokémon to heal</li>
 *   <li>{@code energyToDiscardId} — cardId of the energy to discard from that Pokémon</li>
 * </ul>
 *
 * <p>Healing is represented as reducing damage counters (each counter = 10 damage).
 * 60 damage = 6 counters removed, minimum 0.
 *
 * <p>The target may be the Active Pokémon or a Benched Pokémon — both are
 * checked in {@link #canApply} and handled in {@link #apply}.
 */
@Component
public class SuperPotionEffect implements TrainerEffect {

    private static final int HEAL_AMOUNT   = 60;
    private static final int COUNTERS_HEAL = HEAL_AMOUNT / 10;

    @Override
    public String getCardIdentifier() {
        return "super potion";
    }

    @Override
    public ValidationResult canApply(BoardState state, GameAction action) {
        String targetId  = action.getPayloadString("targetInstanceId");
        String energyId  = action.getPayloadString("energyToDiscardId");
        PlayerState ps   = state.getStateFor(action.getPlayerId());

        if (targetId == null) {
            return ValidationResult.fail("No target Pokémon specified for Super Potion.");
        }
        if (energyId == null) {
            return ValidationResult.fail("No energy specified to discard for Super Potion.");
        }

        Integer damageCounters = null;
        List<String> attachedEnergies = null;

        if (ps.getActivePokemon() != null
                && ps.getActivePokemon().getInstanceId().equals(targetId)) {
            damageCounters   = ps.getActivePokemon().getDamageCounters();
            attachedEnergies = ps.getActivePokemon().getAttachedEnergyIds();
        } else if (ps.getBench() != null) {
            BenchPokemon benchTarget = ps.getBench().stream()
                    .filter(b -> b.getInstanceId().equals(targetId))
                    .findFirst().orElse(null);
            if (benchTarget != null) {
                damageCounters   = benchTarget.getDamageCounters();
                attachedEnergies = benchTarget.getAttachedEnergyIds();
            }
        }

        if (damageCounters == null) {
            return ValidationResult.fail("Target Pokémon is not in play.");
        }
        if (damageCounters == 0) {
            return ValidationResult.fail("That Pokémon has no damage to heal.");
        }
        if (attachedEnergies == null || !attachedEnergies.contains(energyId)) {
            return ValidationResult.fail("The specified energy is not attached to that Pokémon.");
        }

        return ValidationResult.ok();
    }

    @Override
    public EngineResult apply(BoardState state, GameAction action) {
        String targetId = action.getPayloadString("targetInstanceId");
        String energyId = action.getPayloadString("energyToDiscardId");
        PlayerState ps  = state.getStateFor(action.getPlayerId());

        // Heal 6 counters (60 damage), minimum 0
        applyHeal(ps, targetId, energyId, state);

        GameEvent event = GameEvent.builder()
                .type(GameEventType.TRAINER_PLAYED)
                .gameId(state.getGameId())
                .playerId(action.getPlayerId())
                .turnNumber(state.getTurnNumber())
                .data(Map.of(
                        "cardId", action.getPayloadString("cardId"),
                        "healedInstanceId", targetId,
                        "healAmount", HEAL_AMOUNT))
                .occurredAt(Instant.now())
                .build();

        return EngineResult.of(state, List.of(event));
    }

    private void applyHeal(PlayerState ps, String targetId,
                           String energyId, BoardState state) {
        // Check Active Pokémon
        if (ps.getActivePokemon() != null &&
                ps.getActivePokemon().getInstanceId().equals(targetId)) {
            ActivePokemon active = ps.getActivePokemon();
            int newCounters = Math.max(0, active.getDamageCounters() - COUNTERS_HEAL);
            active.setDamageCounters(newCounters);
            discardEnergy(ps, active.getAttachedEnergyIds(), energyId, state);
            return;
        }
        // Check Bench
        if (ps.getBench() != null) {
            ps.getBench().stream()
                    .filter(b -> b.getInstanceId().equals(targetId))
                    .findFirst()
                    .ifPresent(b -> {
                        int newCounters = Math.max(0, b.getDamageCounters() - COUNTERS_HEAL);
                        b.setDamageCounters(newCounters);
                        discardEnergy(ps, b.getAttachedEnergyIds(), energyId, state);
                    });
        }
    }

    private void discardEnergy(PlayerState ps, List<String> attachedEnergies,
                               String energyId, BoardState state) {
        if (attachedEnergies == null) return;
        List<String> energies = new ArrayList<>(attachedEnergies);
        energies.remove(energyId);
        attachedEnergies.clear();
        attachedEnergies.addAll(energies);

        List<String> discard = new ArrayList<>(
                ps.getDiscard() != null ? ps.getDiscard() : new ArrayList<>());
        discard.add(energyId);
        ps.setDiscard(discard);
    }
}