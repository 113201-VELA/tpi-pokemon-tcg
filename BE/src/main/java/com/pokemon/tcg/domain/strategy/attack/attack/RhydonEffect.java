package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import com.pokemon.tcg.engine.CoinFlipService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * xy1-61 Rhydon
 *
 * Horn Drill: 50 damage. No additional effect.
 * Mad Mountain: Flip 2 coins. If both are heads, discard the top card of the
 *               opponent's deck for each damage counter on this Pokémon.
 */
@Component
public class RhydonEffect implements AttackEffect {

    private static final String HORN_DRILL   = "horn drill";
    private static final String MAD_MOUNTAIN = "mad mountain";

    private final CoinFlipService coinFlipService;

    public RhydonEffect(CoinFlipService coinFlipService) {
        this.coinFlipService = coinFlipService;
    }

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("rhydon|horn drill", "rhydon|mad mountain");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        switch (attackName) {
            case HORN_DRILL   -> { } // 50 damage handled by pipeline — no extra effect
            case MAD_MOUNTAIN -> applyMadMountain(ctx);
            default           -> { }
        }
    }

    /**
     * Mad Mountain: flip 2 coins. If both are heads, discard one card from
     * the top of the opponent's deck for each damage counter on Rhydon.
     * If the opponent's deck runs out, stop discarding.
     */
    private void applyMadMountain(AttackContext ctx) {
        CoinResult first  = coinFlipService.flipAndEmit(ctx, ctx.getAction().getPlayerId());
        CoinResult second = coinFlipService.flipAndEmit(ctx, ctx.getAction().getPlayerId());

        if (first != CoinResult.HEADS || second != CoinResult.HEADS) return;

        String attackerId    = ctx.getAction().getPlayerId();
        PlayerState attacker = ctx.getBoardState().getStateFor(attackerId);
        PlayerState opponent = ctx.getBoardState().getOpponentState(attackerId);

        ActivePokemon rhydon = attacker.getActivePokemon();
        if (rhydon == null) return;

        int counters = rhydon.getDamageCounters();
        if (counters == 0) return;

        List<String> deck = new ArrayList<>(
                opponent.getDeck() != null ? opponent.getDeck() : new ArrayList<>());
        List<String> discard = new ArrayList<>(
                opponent.getDiscard() != null ? opponent.getDiscard() : new ArrayList<>());

        int toDiscard = Math.min(counters, deck.size());
        for (int i = 0; i < toDiscard; i++) {
            discard.add(deck.remove(0));
        }

        opponent.setDeck(deck);
        opponent.setDiscard(discard);
    }
}