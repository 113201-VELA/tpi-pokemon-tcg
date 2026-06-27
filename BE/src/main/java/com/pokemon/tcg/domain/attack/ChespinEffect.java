package com.pokemon.tcg.domain.attack;

import com.pokemon.tcg.domain.model.game.CoinResult;
import com.pokemon.tcg.engine.CoinFlipService;
import com.pokemon.tcg.engine.DamageModifier;
import com.pokemon.tcg.engine.attack.AttackContext;
import com.pokemon.tcg.engine.attack.AttackEffect;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ChespinEffect implements AttackEffect {

    private static final int COIN_FLIPS       = 4;
    private static final int DAMAGE_PER_HEAD  = 10;

    private final CoinFlipService coinFlipService;

    public ChespinEffect(CoinFlipService coinFlipService) {
        this.coinFlipService = coinFlipService;
    }

    @Override
    public void apply(AttackContext ctx) {
        int heads = 0;
        for (int i = 0; i < COIN_FLIPS; i++) {
            if (coinFlipService.flip() == CoinResult.HEADS) {
                heads++;
            }
        }

        if (heads > 0) {
            List<DamageModifier> modifiers = new ArrayList<>(
                    ctx.getModifiers() != null ? ctx.getModifiers() : new ArrayList<>());
            modifiers.add(new DamageModifier("pin-missile-heads",
                    heads * DAMAGE_PER_HEAD, true));
            ctx.setModifiers(modifiers);
        }
    }
}
