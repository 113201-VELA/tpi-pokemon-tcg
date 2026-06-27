package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class FletchlingEffect implements AttackEffect {

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("fletchling|me first");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackerId         = ctx.getAction().getPlayerId();
        PlayerState attackerState = ctx.getBoardState().getStateFor(attackerId);

        List<String> deck = attackerState.getDeck();
        if (deck == null || deck.isEmpty()) return;

        List<String> mutableDeck = new ArrayList<>(deck);
        List<String> hand = new ArrayList<>(
                attackerState.getHand() != null
                        ? attackerState.getHand() : new ArrayList<>());

        hand.add(mutableDeck.remove(0));

        attackerState.setDeck(mutableDeck);
        attackerState.setHand(hand);
    }
}
