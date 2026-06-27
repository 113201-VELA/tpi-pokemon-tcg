package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * xy1-58 Diglett
 *
 * Mine: Look at the top card of your opponent's deck. Then, you may have
 *       your opponent shuffle his or her deck.
 * Mud-Slap: 20 damage. No additional effect.
 */
@Component
public class DiglettEffect implements AttackEffect {

    private static final String MINE      = "mine";
    private static final String MUD_SLAP  = "mud-slap";

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("diglett|mine", "diglett|mud-slap");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        switch (attackName) {
            case MINE     -> applyMine(ctx);
            case MUD_SLAP -> { } // 20 damage handled by pipeline — no extra effect
            default       -> { }
        }
    }

    /**
     * Mine: reveal the top card of the opponent's deck via pendingDeckSelectionCardIds.
     * The attacker then decides whether to shuffle the opponent's deck — modeled as
     * a pending attack selection so the player can confirm via ACCEPT or skip.
     * If the opponent's deck is empty, nothing happens.
     */
    private void applyMine(AttackContext ctx) {
        String attackerId    = ctx.getAction().getPlayerId();
        PlayerState opponent = ctx.getBoardState().getOpponentState(attackerId);

        List<String> deck = opponent.getDeck();
        if (deck == null || deck.isEmpty()) return;

        String topCard = deck.get(0);

        // Expose top card and set pending selection so the attacker can decide
        // whether to shuffle the opponent's deck
        ctx.setBoardState(ctx.getBoardState().toBuilder()
                .pendingAttackSelectionKey("diglett|mine")
                .pendingAttackSelectionPlayerId(attackerId)
                .pendingAttackSelectionMaxCards(1)
                .pendingAttackSelectionType(AttackSelectionType.PICK)
                .pendingDeckSelectionCardIds(new ArrayList<>(List.of(topCard)))
                .build());
    }

    /**
     * Shuffles the opponent's deck. Called externally when the attacker
     * confirms the shuffle after Mine resolves.
     */
    public static void shuffleOpponentDeck(PlayerState opponent) {
        List<String> deck = opponent.getDeck();
        if (deck == null || deck.isEmpty()) return;
        List<String> mutable = new ArrayList<>(deck);
        Collections.shuffle(mutable);
        opponent.setDeck(mutable);
    }
}