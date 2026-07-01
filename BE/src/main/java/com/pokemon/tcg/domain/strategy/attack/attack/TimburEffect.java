package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.CoinResult;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import com.pokemon.tcg.domain.strategy.attack.DamageModifier;
import com.pokemon.tcg.engine.CoinFlipService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * xy1-65 Timburr — "Pummel"
 * Flip a coin. If heads, this attack does 20 more damage.
 */
@Component
public class TimburEffect implements AttackEffect {

    private final CoinFlipService coinFlipService;

    public TimburEffect(CoinFlipService coinFlipService) {
        this.coinFlipService = coinFlipService;
    }

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("timburr|pummel");
    }

    @Override
    public void apply(AttackContext ctx) {
        if (coinFlipService.flipAndEmit(ctx, ctx.getAction().getPlayerId()) != CoinResult.HEADS) return;

        List<DamageModifier> modifiers = new ArrayList<>(
                ctx.getModifiers() != null ? ctx.getModifiers() : new ArrayList<>());
        modifiers.add(new DamageModifier("pummel-heads", 20, true));
        ctx.setModifiers(modifiers);
    }
}