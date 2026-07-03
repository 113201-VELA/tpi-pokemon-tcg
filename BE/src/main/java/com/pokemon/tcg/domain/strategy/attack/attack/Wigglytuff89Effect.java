package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import com.pokemon.tcg.engine.CardLookupPort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * xy1-89 Wigglytuff
 *
 * Gather Energy: search your deck for a basic Energy card and attach it
 *                to 1 of your Pokémon. Shuffle your deck afterward.
 * Hocus Pinkus: 60 damage. The Defending Pokémon can't attack during your
 *               opponent's next turn.
 */
@Component
public class Wigglytuff89Effect implements AttackEffect {

    private static final String GATHER_ENERGY = "gather energy";
    private static final String HOCUS_PINKUS  = "hocus pinkus";

    private final CardLookupPort cardLookupPort;

    public Wigglytuff89Effect(CardLookupPort cardLookupPort) {
        this.cardLookupPort = cardLookupPort;
    }

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("wigglytuff|gather energy", "wigglytuff|hocus pinkus");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        switch (attackName) {
            case GATHER_ENERGY -> applyGatherEnergy(ctx);
            case HOCUS_PINKUS  -> applyHocusPinkus(ctx);
            default            -> { }
        }
    }

    /**
     * Search the deck for a basic Energy card and attach it to one of the
     * attacker's own Pokémon (Active or Bench), then shuffle the deck.
     * <p>
     * Payload: {@code energyCardId} (optional) — a specific basic Energy to
     * search for, falls back to the first basic Energy found in the deck.
     * {@code targetInstanceId} (optional) — which own Pokémon receives the
     * Energy, falls back to the attacker's own Active Pokémon.
     */
    private void applyGatherEnergy(AttackContext ctx) {
        String attackerId    = ctx.getAction().getPlayerId();
        PlayerState attacker = ctx.getBoardState().getStateFor(attackerId);

        List<String> deck = new ArrayList<>(
                attacker.getDeck() != null ? attacker.getDeck() : new ArrayList<>());

        String requestedEnergyId = ctx.getAction().getPayloadString("energyCardId");
        String chosenEnergy;
        if (requestedEnergyId != null && deck.contains(requestedEnergyId)
                && isBasicEnergy(requestedEnergyId)) {
            chosenEnergy = requestedEnergyId;
        } else {
            chosenEnergy = deck.stream()
                    .filter(this::isBasicEnergy)
                    .findFirst()
                    .orElse(null);
        }

        if (chosenEnergy == null) return; // no basic Energy in deck

        deck.remove(chosenEnergy);
        Collections.shuffle(deck);
        attacker.setDeck(deck);

        String targetInstanceId = ctx.getAction().getPayloadString("targetInstanceId");

        if (targetInstanceId != null && attacker.getBench() != null) {
            BenchPokemon benchTarget = attacker.getBench().stream()
                    .filter(b -> b.getInstanceId().equals(targetInstanceId))
                    .findFirst().orElse(null);
            if (benchTarget != null) {
                attachEnergy(benchTarget, chosenEnergy);
                return;
            }
        }

        // Falls back to the attacker's own Active Pokémon (also covers the
        // case where targetInstanceId matches the Active Pokémon itself).
        if (attacker.getActivePokemon() != null) {
            attachEnergy(attacker.getActivePokemon(), chosenEnergy);
        }
    }

    private void attachEnergy(ActivePokemon pokemon, String energyId) {
        List<String> energies = new ArrayList<>(
                pokemon.getAttachedEnergyIds() != null
                        ? pokemon.getAttachedEnergyIds() : new ArrayList<>());
        energies.add(energyId);
        pokemon.setAttachedEnergyIds(energies);
    }

    private void attachEnergy(BenchPokemon pokemon, String energyId) {
        List<String> energies = new ArrayList<>(
                pokemon.getAttachedEnergyIds() != null
                        ? pokemon.getAttachedEnergyIds() : new ArrayList<>());
        energies.add(energyId);
        pokemon.setAttachedEnergyIds(energies);
    }

    private boolean isBasicEnergy(String cardId) {
        return cardLookupPort.findCardById(cardId)
                .map(card -> Boolean.TRUE.equals(card.isBasicEnergy()))
                .orElse(false);
    }

    /**
     * The Defending Pokémon can't attack during the opponent's next turn.
     * Implemented via {@link PokemonEffect#CANT_ATTACK}, which
     * TurnManager clears on the owner's next handleDrawCard — the same
     * lifecycle already used for CANT_RETREAT.
     */
    private void applyHocusPinkus(AttackContext ctx) {
        String attackerId       = ctx.getAction().getPlayerId();
        PlayerState opponent    = ctx.getBoardState().getOpponentState(attackerId);
        ActivePokemon defender  = opponent.getActivePokemon();

        if (defender == null) return;

        List<PokemonEffect> effects = new ArrayList<>(
                defender.getActiveEffects() != null
                        ? defender.getActiveEffects() : new ArrayList<>());
        if (!effects.contains(PokemonEffect.CANT_ATTACK)) {
            effects.add(PokemonEffect.CANT_ATTACK);
        }
        defender.setActiveEffects(effects);
    }
}