package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.engine.CardLookupPort;
import com.pokemon.tcg.domain.strategy.attack.DamageModifier;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MagcargoEffect implements AttackEffect {

    private static final int    FIRE_BONUS   = 50;
    private static final String FIRE_TYPE    = EnergyType.FIRE.name();

    private final CardLookupPort cardLookupPort;

    public MagcargoEffect(CardLookupPort cardLookupPort) {
        this.cardLookupPort = cardLookupPort;
    }

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("magcargo|magma mantle");
    }

    @Override
    public void apply(AttackContext ctx) {
        if (!getDiscardTop(ctx.getAction())) return;

        String attackerId         = ctx.getAction().getPlayerId();
        PlayerState attackerState = ctx.getBoardState().getStateFor(attackerId);

        List<String> deck = attackerState.getDeck();
        if (deck == null || deck.isEmpty()) return;

        List<String> mutableDeck = new ArrayList<>(deck);
        String topCardId = mutableDeck.remove(0);
        attackerState.setDeck(mutableDeck);

        List<String> discard = new ArrayList<>(
                attackerState.getDiscard() != null
                        ? attackerState.getDiscard() : new ArrayList<>());
        discard.add(topCardId);
        attackerState.setDiscard(discard);

        if (isFireEnergy(topCardId)) {
            List<DamageModifier> modifiers = new ArrayList<>(
                    ctx.getModifiers() != null ? ctx.getModifiers() : new ArrayList<>());
            modifiers.add(new DamageModifier("magma-mantle-fire", FIRE_BONUS, true));
            ctx.setModifiers(modifiers);
        }
    }

    private boolean getDiscardTop(GameAction action) {
        Object raw = action.getPayload() != null
                ? action.getPayload().get("discardTop")
                : null;
        return Boolean.TRUE.equals(raw);
    }

    private boolean isFireEnergy(String cardId) {
        return cardLookupPort.findCardById(cardId)
                .map(card -> card.isBasicEnergy()
                        && card.getTypes() != null
                        && card.getTypes().contains(FIRE_TYPE))
                .orElse(false);
    }
}
