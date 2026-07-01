package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.PlayerState;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ZoruaEffect implements AttackEffect {

    private static final String NASTY_PLOT = "nasty plot";
    private static final String ATTACK_KEY = "zorua|nasty plot";

    @Override
    public List<String> getSupportedAttacks() {
        // Scratch has no special text; base damage is resolved by the pipeline directly.
        return List.of(ATTACK_KEY);
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        if (NASTY_PLOT.equals(attackName)) {
            applyNastyPlot(ctx);
        }
    }

    private void applyNastyPlot(AttackContext ctx) {
        String attackerId = ctx.getAction().getPlayerId();
        PlayerState ps    = ctx.getBoardState().getStateFor(attackerId);

        List<String> deck = ps.getDeck() != null ? ps.getDeck() : List.of();

        // Unlike Pheromation, Nasty Plot has no type restriction: any card in the deck is a valid target.
        if (deck.isEmpty()) return;

        ctx.setBoardState(ctx.getBoardState().toBuilder()
                .pendingAttackSelectionKey(ATTACK_KEY)
                .pendingAttackSelectionPlayerId(attackerId)
                .pendingDeckSelectionCardIds(new ArrayList<>(deck))
                .build());
    }
}