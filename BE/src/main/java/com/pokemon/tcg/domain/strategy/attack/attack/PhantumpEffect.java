package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class PhantumpEffect implements AttackEffect {

    private static final String ASTONISH = "astonish";

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("phantump|astonish");
    }

    @Override
    public void apply(AttackContext ctx) {
        if (!ASTONISH.equals(ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase() : "")) return;

        String attackerId    = ctx.getAction().getPlayerId();
        PlayerState opponent = ctx.getBoardState().getOpponentState(attackerId);

        List<String> hand = opponent.getHand();
        if (hand == null || hand.isEmpty()) return;

        // Choose a random card from the opponent's hand
        List<String> mutableHand = new ArrayList<>(hand);
        int randomIndex = (int) (Math.random() * mutableHand.size());
        String chosen = mutableHand.remove(randomIndex);

        // Shuffle the chosen card back into the opponent's deck
        List<String> deck = new ArrayList<>(
                opponent.getDeck() != null ? opponent.getDeck() : new ArrayList<>());
        deck.add(chosen);
        Collections.shuffle(deck);

        opponent.setHand(mutableHand);
        opponent.setDeck(deck);
    }
}