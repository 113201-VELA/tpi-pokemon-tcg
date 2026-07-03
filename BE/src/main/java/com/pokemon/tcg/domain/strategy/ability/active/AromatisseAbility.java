package com.pokemon.tcg.domain.strategy.ability.active;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.ability.ActiveAbilityEffect;
import com.pokemon.tcg.engine.CardLookupPort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Aromatisse — Fairy Transfer (Active Ability).
 *
 * <p>As often as you like during your turn (before your attack), you may
 * move a Fairy Energy attached to 1 of your Pokémon to another of your
 * Pokémon. Unlike single-use active abilities (e.g. Delphox's Mystical
 * Fire, Greninja's Water Shuriken), this ability is never marked as used —
 * RuleValidator only blocks reuse when {@code isAbilityUsed} returns true,
 * so omitting {@code markAbilityUsed} keeps it available all turn.
 *
 * <p>Payload expected:
 * <ul>
 *   <li>{@code instanceId} — instanceId of Aromatisse using the ability.</li>
 *   <li>{@code abilityName} — "Fairy Transfer".</li>
 *   <li>{@code sourceInstanceId} — instanceId of the Pokémon (Active or
 *       Bench, owned by the player) currently holding the Fairy Energy.</li>
 *   <li>{@code targetInstanceId} — instanceId of the Pokémon (Active or
 *       Bench, owned by the player) that receives the Fairy Energy.</li>
 *   <li>{@code energyCardId} — cardId of the Fairy Energy to move.</li>
 * </ul>
 */
@Component
public class AromatisseAbility implements ActiveAbilityEffect {

    private static final String IDENTIFIER  = "aromatisse|fairy transfer";
    private static final String FAIRY_TYPE  = EnergyType.FAIRY.name();

    private final CardLookupPort cardLookupPort;

    public AromatisseAbility(CardLookupPort cardLookupPort) {
        this.cardLookupPort = cardLookupPort;
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public ValidationResult canApply(BoardState state, GameAction action) {
        String sourceInstanceId = action.getPayloadString("sourceInstanceId");
        String targetInstanceId = action.getPayloadString("targetInstanceId");
        String energyCardId     = action.getPayloadString("energyCardId");

        if (sourceInstanceId == null) {
            return ValidationResult.fail("You must specify the source Pokémon.");
        }
        if (targetInstanceId == null) {
            return ValidationResult.fail("You must specify the target Pokémon.");
        }
        if (energyCardId == null) {
            return ValidationResult.fail("You must specify a Fairy Energy card to move.");
        }

        PlayerState ps = state.getStateFor(action.getPlayerId());

        if (!pokemonExists(ps, sourceInstanceId)) {
            return ValidationResult.fail("The source Pokémon is not in play.");
        }
        if (!pokemonExists(ps, targetInstanceId)) {
            return ValidationResult.fail("The target Pokémon is not in play.");
        }

        List<String> sourceEnergies = getAttachedEnergies(ps, sourceInstanceId);
        if (sourceEnergies == null || !sourceEnergies.contains(energyCardId)) {
            return ValidationResult.fail(
                    "The specified Energy is not attached to the source Pokémon.");
        }

        boolean isFairy = cardLookupPort.findCardById(energyCardId)
                .map(card -> card.getTypes() != null && card.getTypes().contains(FAIRY_TYPE))
                .orElse(false);
        if (!isFairy) {
            return ValidationResult.fail("You must move a Fairy Energy card.");
        }

        return ValidationResult.ok();
    }

    @Override
    public BoardState apply(BoardState state, GameAction action) {
        String sourceInstanceId = action.getPayloadString("sourceInstanceId");
        String targetInstanceId = action.getPayloadString("targetInstanceId");
        String energyCardId     = action.getPayloadString("energyCardId");

        PlayerState ps = state.getStateFor(action.getPlayerId());

        List<String> sourceEnergies = new ArrayList<>(getAttachedEnergies(ps, sourceInstanceId));
        sourceEnergies.remove(energyCardId);
        setAttachedEnergies(ps, sourceInstanceId, sourceEnergies);

        List<String> targetEnergies = new ArrayList<>(
                getAttachedEnergies(ps, targetInstanceId) != null
                        ? getAttachedEnergies(ps, targetInstanceId) : new ArrayList<>());
        targetEnergies.add(energyCardId);
        setAttachedEnergies(ps, targetInstanceId, targetEnergies);

        // Deliberately NOT calling state.getTurnFlags().markAbilityUsed(...):
        // Fairy Transfer can be used any number of times per turn.

        return state;
    }

    // ── Private helpers (own Active or own Bench, no common interface) ────────

    private boolean pokemonExists(PlayerState ps, String instanceId) {
        if (ps.getActivePokemon() != null
                && ps.getActivePokemon().getInstanceId().equals(instanceId)) {
            return true;
        }
        return ps.getBench() != null && ps.getBench().stream()
                .anyMatch(b -> b.getInstanceId().equals(instanceId));
    }

    private List<String> getAttachedEnergies(PlayerState ps, String instanceId) {
        if (ps.getActivePokemon() != null
                && ps.getActivePokemon().getInstanceId().equals(instanceId)) {
            return ps.getActivePokemon().getAttachedEnergyIds();
        }
        if (ps.getBench() != null) {
            return ps.getBench().stream()
                    .filter(b -> b.getInstanceId().equals(instanceId))
                    .findFirst()
                    .map(BenchPokemon::getAttachedEnergyIds)
                    .orElse(null);
        }
        return null;
    }

    private void setAttachedEnergies(PlayerState ps, String instanceId, List<String> energies) {
        if (ps.getActivePokemon() != null
                && ps.getActivePokemon().getInstanceId().equals(instanceId)) {
            ps.getActivePokemon().setAttachedEnergyIds(energies);
            return;
        }
        if (ps.getBench() != null) {
            ps.getBench().stream()
                    .filter(b -> b.getInstanceId().equals(instanceId))
                    .findFirst()
                    .ifPresent(b -> b.setAttachedEnergyIds(energies));
        }
    }
}