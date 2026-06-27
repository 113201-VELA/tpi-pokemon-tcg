package com.pokemon.tcg.domain.strategy.ability.active;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.ability.ActiveAbilityEffect;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DelphoxAbility implements ActiveAbilityEffect {

    private static final int    TARGET_HAND_SIZE = 6;
    private static final String IDENTIFIER       = "delphox|mystical fire";

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public ValidationResult canApply(BoardState state, GameAction action) {
        PlayerState ps = state.getStateFor(action.getPlayerId());

        if (ps.getHand() != null && ps.getHand().size() >= TARGET_HAND_SIZE) {
            return ValidationResult.fail(
                    "You already have 6 or more cards in hand.");
        }
        if (ps.getDeck() == null || ps.getDeck().isEmpty()) {
            return ValidationResult.fail(
                    "Your deck is empty.");
        }
        return ValidationResult.ok();
    }

    @Override
    public BoardState apply(BoardState state, GameAction action) {
        PlayerState ps = state.getStateFor(action.getPlayerId());

        List<String> hand = new ArrayList<>(
                ps.getHand() != null ? ps.getHand() : new ArrayList<>());
        List<String> deck = new ArrayList<>(
                ps.getDeck() != null ? ps.getDeck() : new ArrayList<>());

        while (hand.size() < TARGET_HAND_SIZE && !deck.isEmpty()) {
            hand.add(deck.remove(0));
        }

        ps.setHand(hand);
        ps.setDeck(deck);

        String instanceId  = action.getPayloadString("instanceId");
        String abilityName = action.getPayloadString("abilityName");
        state.getTurnFlags().markAbilityUsed(instanceId, abilityName);

        return state;
    }
}
