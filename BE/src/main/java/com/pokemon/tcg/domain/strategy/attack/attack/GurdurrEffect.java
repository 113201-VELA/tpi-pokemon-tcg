package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import com.pokemon.tcg.domain.strategy.attack.DamageModifier;
import com.pokemon.tcg.engine.CoinFlipService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * xy1-66 Gurdurr
 *
 * Pummel: flip a coin. If heads, this attack does 20 more damage.
 * Hammer Arm: 60 damage. Discard the top card of the opponent's deck.
 */
@Component
public class GurdurrEffect implements AttackEffect {

    private static final String PUMMEL     = "pummel";
    private static final String HAMMER_ARM = "hammer arm";

    private final CoinFlipService coinFlipService;

    public GurdurrEffect(CoinFlipService coinFlipService) {
        this.coinFlipService = coinFlipService;
    }

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("gurdurr|pummel", "gurdurr|hammer arm");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        switch (attackName) {
            case PUMMEL     -> applyPummel(ctx);
            case HAMMER_ARM -> applyHammerArm(ctx);
            default         -> { }
        }
    }

    /**
     * Pummel: flip a coin; on heads add 20 damage via a pre-weakness modifier.
     */
    private void applyPummel(AttackContext ctx) {
        if (coinFlipService.flip() != CoinResult.HEADS) return;

        List<DamageModifier> modifiers = new ArrayList<>(
                ctx.getModifiers() != null ? ctx.getModifiers() : new ArrayList<>());
        modifiers.add(new DamageModifier("pummel-heads", 20, true));
        ctx.setModifiers(modifiers);
    }

    /**
     * Hammer Arm: discard the top card of the opponent's deck.
     * If the deck is empty, nothing happens.
     */
    private void applyHammerArm(AttackContext ctx) {
        String attackerId    = ctx.getAction().getPlayerId();
        PlayerState opponent = ctx.getBoardState().getOpponentState(attackerId);

        List<String> deck = new ArrayList<>(
                opponent.getDeck() != null ? opponent.getDeck() : new ArrayList<>());
        if (deck.isEmpty()) return;

        List<String> discard = new ArrayList<>(
                opponent.getDiscard() != null ? opponent.getDiscard() : new ArrayList<>());

        discard.add(deck.remove(0));
        opponent.setDeck(deck);
        opponent.setDiscard(discard);
    }
}