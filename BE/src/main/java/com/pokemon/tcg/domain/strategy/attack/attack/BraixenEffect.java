package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.engine.CardLookupPort;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class BraixenEffect implements AttackEffect {

    private static final String CLAIRVOYANT_DECK = "clairvoyant deck";
    private static final String FIRETAIL_SLAP    = "firetail slap";
    private static final String ATTACK_KEY       = "braixen|clairvoyant deck";
    private static final String FIRE_TYPE        = EnergyType.FIRE.name();
    private static final int    LOOK_AT_COUNT    = 3;

    private final CardLookupPort cardLookupPort;

    public BraixenEffect(CardLookupPort cardLookupPort) {
        this.cardLookupPort = cardLookupPort;
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        switch (attackName) {
            case CLAIRVOYANT_DECK -> applyClairvoyantDeck(ctx);
            case FIRETAIL_SLAP    -> applyFiretailSlap(ctx);
            default               -> { }
        }
    }

    private void applyClairvoyantDeck(AttackContext ctx) {
        String attackerId = ctx.getAction().getPlayerId();
        PlayerState ps    = ctx.getBoardState().getStateFor(attackerId);

        List<String> deck = ps.getDeck() != null ? ps.getDeck() : List.of();
        if (deck.isEmpty()) return;

        List<String> top = deck.stream().limit(LOOK_AT_COUNT).toList();

        ctx.setBoardState(ctx.getBoardState().toBuilder()
                .pendingAttackSelectionKey(ATTACK_KEY)
                .pendingAttackSelectionPlayerId(attackerId)
                .pendingAttackSelectionMaxCards(LOOK_AT_COUNT)
                .pendingAttackSelectionType(AttackSelectionType.REORDER)
                .pendingDeckSelectionCardIds(new ArrayList<>(top))
                .build());
    }

    private void applyFiretailSlap(AttackContext ctx) {
        String energyToDiscardId = ctx.getAction().getPayloadString("energyToDiscardId");
        if (energyToDiscardId == null) return;

        String attackerId         = ctx.getAction().getPlayerId();
        PlayerState attackerState = ctx.getBoardState().getStateFor(attackerId);
        ActivePokemon braixen     = attackerState.getActivePokemon();

        if (braixen == null) return;
        if (braixen.getAttachedEnergyIds() == null
                || !braixen.getAttachedEnergyIds().contains(energyToDiscardId)) return;
        if (!isFireEnergy(energyToDiscardId)) return;

        List<String> energies = new ArrayList<>(braixen.getAttachedEnergyIds());
        energies.remove(energyToDiscardId);
        braixen.setAttachedEnergyIds(energies);

        List<String> discard = new ArrayList<>(
                attackerState.getDiscard() != null
                        ? attackerState.getDiscard() : new ArrayList<>());
        discard.add(energyToDiscardId);
        attackerState.setDiscard(discard);
    }

    private boolean isFireEnergy(String cardId) {
        return cardLookupPort.findCardById(cardId)
                .map(card -> card.getTypes() != null
                        && card.getTypes().contains(FIRE_TYPE))
                .orElse(false);
    }
}
