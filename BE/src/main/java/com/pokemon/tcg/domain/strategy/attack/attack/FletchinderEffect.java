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

@Component
public class FletchinderEffect implements AttackEffect {

    private static final String FIRE_TYPE = EnergyType.FIRE.name();

    private final CardLookupPort cardLookupPort;

    public FletchinderEffect(CardLookupPort cardLookupPort) {
        this.cardLookupPort = cardLookupPort;
    }

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("fletchinder|flame charge");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackerId         = ctx.getAction().getPlayerId();
        PlayerState attackerState = ctx.getBoardState().getStateFor(attackerId);
        ActivePokemon fletchinder = attackerState.getActivePokemon();

        if (fletchinder == null) return;

        List<String> deck = attackerState.getDeck() != null
                ? new ArrayList<>(attackerState.getDeck())
                : new ArrayList<>();

        if (deck.isEmpty()) return;

        String fireEnergyId = deck.stream()
                .filter(this::isFireEnergy)
                .findFirst()
                .orElse(null);

        if (fireEnergyId == null) return;

        deck.remove(fireEnergyId);

        List<String> attachedEnergies = new ArrayList<>(
                fletchinder.getAttachedEnergyIds() != null
                        ? fletchinder.getAttachedEnergyIds() : new ArrayList<>());
        attachedEnergies.add(fireEnergyId);
        fletchinder.setAttachedEnergyIds(attachedEnergies);

        Collections.shuffle(deck);
        attackerState.setDeck(deck);
    }

    private boolean isFireEnergy(String cardId) {
        return cardLookupPort.findCardById(cardId)
                .map(card -> card.isBasicEnergy()
                        && card.getTypes() != null
                        && card.getTypes().contains(FIRE_TYPE))
                .orElse(false);
    }
}
