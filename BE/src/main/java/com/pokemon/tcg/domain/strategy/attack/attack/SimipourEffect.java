package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SimipourEffect implements AttackEffect {

    private static final String RECYCLE = "recycle";

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("simipour|recycle");
    }

    @Override
    public void apply(AttackContext ctx) {
        if (!RECYCLE.equals(ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase() : "")) return;

        String cardId = ctx.getAction().getPayloadString("cardId");
        if (cardId == null) return;

        String attackerId    = ctx.getAction().getPlayerId();
        PlayerState attacker = ctx.getBoardState().getStateFor(attackerId);

        List<String> discard = new ArrayList<>(
                attacker.getDiscard() != null ? attacker.getDiscard() : new ArrayList<>());
        if (!discard.contains(cardId)) return;

        discard.remove(cardId);
        attacker.setDiscard(discard);

        List<String> deck = new ArrayList<>(
                attacker.getDeck() != null ? attacker.getDeck() : new ArrayList<>());
        deck.add(0, cardId);
        attacker.setDeck(deck);
    }
}