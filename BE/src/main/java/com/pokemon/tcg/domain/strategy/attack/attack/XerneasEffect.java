package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import com.pokemon.tcg.engine.CardLookupPort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * xy1-96 Xerneas
 *
 * Geomancy: choose 2 of your Benched Pokémon. For each of those Pokémon,
 *           search your deck for a Fairy Energy card and attach it to that
 *           Pokémon. Shuffle your deck afterward (once, not per search).
 * Rainbow Spear: 100 damage. Discard an Energy attached to this Pokémon
 *                (mandatory, any Energy type — not restricted to Fairy).
 */
@Component
public class XerneasEffect implements AttackEffect {

    private static final String GEOMANCY      = "geomancy";
    private static final String RAINBOW_SPEAR = "rainbow spear";
    private static final String FAIRY_TYPE    = EnergyType.FAIRY.name();
    private static final int    MAX_TARGETS   = 2;

    private final CardLookupPort cardLookupPort;

    public XerneasEffect(CardLookupPort cardLookupPort) {
        this.cardLookupPort = cardLookupPort;
    }

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("xerneas|geomancy", "xerneas|rainbow spear");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        switch (attackName) {
            case GEOMANCY      -> applyGeomancy(ctx);
            case RAINBOW_SPEAR -> applyRainbowSpear(ctx);
            default            -> { }
        }
    }

    /**
     * For up to 2 chosen Benched Pokémon (payload: {@code targetInstanceIds},
     * a list of instanceIds), search the deck for a Fairy Energy card and
     * attach it to that Pokémon. Each search consumes a different card from
     * the deck. Shuffle happens once at the end, only if at least one
     * Energy was actually found and attached.
     */
    private void applyGeomancy(AttackContext ctx) {
        String attackerId    = ctx.getAction().getPlayerId();
        PlayerState attacker = ctx.getBoardState().getStateFor(attackerId);

        List<String> targetInstanceIds = getTargetInstanceIds(ctx.getAction()).stream()
                .distinct()
                .limit(MAX_TARGETS)
                .toList();

        if (targetInstanceIds.isEmpty() || attacker.getBench() == null) return;

        List<String> deck = new ArrayList<>(
                attacker.getDeck() != null ? attacker.getDeck() : new ArrayList<>());
        boolean deckChanged = false;

        for (String targetId : targetInstanceIds) {
            BenchPokemon target = attacker.getBench().stream()
                    .filter(b -> b.getInstanceId().equals(targetId))
                    .findFirst().orElse(null);
            if (target == null) continue;

            String fairyEnergy = deck.stream().filter(this::isFairyEnergy).findFirst().orElse(null);
            if (fairyEnergy == null) continue; // no more Fairy Energy left in deck

            deck.remove(fairyEnergy);
            deckChanged = true;

            List<String> energies = new ArrayList<>(
                    target.getAttachedEnergyIds() != null
                            ? target.getAttachedEnergyIds() : new ArrayList<>());
            energies.add(fairyEnergy);
            target.setAttachedEnergyIds(energies);
        }

        if (deckChanged) {
            Collections.shuffle(deck);
            attacker.setDeck(deck);
        }
    }

    /**
     * Mandatory cost: discard an Energy attached to Xerneas, any type.
     * Falls back to the first attached Energy if none is explicitly
     * specified or the specified one isn't attached.
     */
    private void applyRainbowSpear(AttackContext ctx) {
        String attackerId    = ctx.getAction().getPlayerId();
        PlayerState attacker = ctx.getBoardState().getStateFor(attackerId);
        ActivePokemon xerneas = attacker.getActivePokemon();

        if (xerneas == null
                || xerneas.getAttachedEnergyIds() == null
                || xerneas.getAttachedEnergyIds().isEmpty()) return;

        List<String> energies = new ArrayList<>(xerneas.getAttachedEnergyIds());
        String requestedEnergyId = ctx.getAction().getPayloadString("energyToDiscardId");

        String chosen = (requestedEnergyId != null && energies.contains(requestedEnergyId))
                ? requestedEnergyId
                : energies.get(0);

        energies.remove(chosen);
        xerneas.setAttachedEnergyIds(energies);

        List<String> discard = new ArrayList<>(
                attacker.getDiscard() != null ? attacker.getDiscard() : new ArrayList<>());
        discard.add(chosen);
        attacker.setDiscard(discard);
    }

    private boolean isFairyEnergy(String cardId) {
        return cardLookupPort.findCardById(cardId)
                .map(card -> card.getTypes() != null && card.getTypes().contains(FAIRY_TYPE))
                .orElse(false);
    }

    @SuppressWarnings("unchecked")
    private List<String> getTargetInstanceIds(GameAction action) {
        Object raw = action.getPayload() != null
                ? action.getPayload().get("targetInstanceIds")
                : null;
        if (raw instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }
}