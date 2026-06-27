package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import com.pokemon.tcg.domain.strategy.attack.DamageModifier;
import com.pokemon.tcg.engine.CardLookupPort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

@Component
public class EmolgaExEffect implements AttackEffect {

    private static final String ENERGY_GLIDE    = "energy glide";
    private static final String ELECTRON_CRUSH  = "electron crush";
    private static final String LIGHTNING_TYPE  = EnergyType.LIGHTNING.name();
    private static final int    DISCARD_BONUS   = 30;

    private final CardLookupPort cardLookupPort;

    public EmolgaExEffect(CardLookupPort cardLookupPort) {
        this.cardLookupPort = cardLookupPort;
    }

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("emolga-ex|energy glide", "emolga-ex|electron crush");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        switch (attackName) {
            case ENERGY_GLIDE   -> applyEnergyGlide(ctx);
            case ELECTRON_CRUSH -> applyElectronCrush(ctx);
            default             -> { }
        }
    }

    /**
     * Energy Glide: search the deck for a Lightning Energy and attach it to
     * Emolga-EX. Shuffle the deck afterward. If energy was attached, switch
     * Emolga-EX with a chosen Benched Pokémon.
     *
     * <p>The player specifies the replacement via {@code replacementInstanceId}.
     * Falls back to first bench slot if not specified.
     */
    private void applyEnergyGlide(AttackContext ctx) {
        String attackerId    = ctx.getAction().getPlayerId();
        PlayerState attacker = ctx.getBoardState().getStateFor(attackerId);
        ActivePokemon emolga = attacker.getActivePokemon();

        if (emolga == null) return;

        List<String> deck = new ArrayList<>(
                attacker.getDeck() != null ? attacker.getDeck() : new ArrayList<>());

        // Find first Lightning Energy in deck
        String lightningEnergy = deck.stream()
                .filter(this::isLightningEnergy)
                .findFirst()
                .orElse(null);

        if (lightningEnergy == null) return; // no Lightning Energy in deck

        // Attach the energy
        deck.remove(lightningEnergy);
        Collections.shuffle(deck);
        attacker.setDeck(deck);

        List<String> energies = new ArrayList<>(
                emolga.getAttachedEnergyIds() != null
                        ? emolga.getAttachedEnergyIds() : new ArrayList<>());
        energies.add(lightningEnergy);
        emolga.setAttachedEnergyIds(energies);

        // Switch with bench if available
        List<BenchPokemon> bench = attacker.getBench();
        if (bench == null || bench.isEmpty()) return;

        String replacementInstanceId =
                ctx.getAction().getPayloadString("replacementInstanceId");

        BenchPokemon replacement = null;
        if (replacementInstanceId != null) {
            replacement = bench.stream()
                    .filter(b -> b.getInstanceId().equals(replacementInstanceId))
                    .findFirst().orElse(null);
        }
        if (replacement == null) {
            replacement = bench.get(0);
        }

        BenchPokemon newBench = BenchPokemon.builder()
                .instanceId(emolga.getInstanceId())
                .cardId(emolga.getCardId())
                .attachedEnergyIds(emolga.getAttachedEnergyIds())
                .attachedToolId(emolga.getAttachedToolId())
                .evolutionStack(emolga.getEvolutionStack())
                .damageCounters(emolga.getDamageCounters())
                .enteredThisTurn(false)
                .build();

        ActivePokemon newActive = ActivePokemon.builder()
                .instanceId(replacement.getInstanceId())
                .cardId(replacement.getCardId())
                .attachedEnergyIds(replacement.getAttachedEnergyIds())
                .attachedToolId(replacement.getAttachedToolId())
                .evolutionStack(replacement.getEvolutionStack())
                .damageCounters(replacement.getDamageCounters())
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .enteredThisTurn(false)
                .build();

        List<BenchPokemon> newBenchList = new ArrayList<>(bench);
        newBenchList.remove(replacement);
        newBenchList.add(newBench);

        attacker.setBench(newBenchList);
        attacker.setActivePokemon(newActive);
    }

    /**
     * Electron Crush: optionally discard one Energy attached to Emolga-EX
     * for 30 extra damage.
     */
    private void applyElectronCrush(AttackContext ctx) {
        String energyToDiscardId = ctx.getAction().getPayloadString("energyToDiscardId");
        if (energyToDiscardId == null) return;

        String attackerId    = ctx.getAction().getPlayerId();
        PlayerState attacker = ctx.getBoardState().getStateFor(attackerId);
        ActivePokemon emolga = attacker.getActivePokemon();

        if (emolga == null) return;
        if (emolga.getAttachedEnergyIds() == null
                || !emolga.getAttachedEnergyIds().contains(energyToDiscardId)) return;

        List<String> energies = new ArrayList<>(emolga.getAttachedEnergyIds());
        energies.remove(energyToDiscardId);
        emolga.setAttachedEnergyIds(energies);

        List<String> discard = new ArrayList<>(
                attacker.getDiscard() != null ? attacker.getDiscard() : new ArrayList<>());
        discard.add(energyToDiscardId);
        attacker.setDiscard(discard);

        List<DamageModifier> modifiers = new ArrayList<>(
                ctx.getModifiers() != null ? ctx.getModifiers() : new ArrayList<>());
        modifiers.add(new DamageModifier("electron-crush-discard", DISCARD_BONUS, true));
        ctx.setModifiers(modifiers);
    }

    private boolean isLightningEnergy(String cardId) {
        return cardLookupPort.findCardById(cardId)
                .map(card -> card.getTypes() != null
                        && card.getTypes().contains(LIGHTNING_TYPE))
                .orElse(false);
    }
}