package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import com.pokemon.tcg.domain.strategy.attack.DamageModifier;
import com.pokemon.tcg.engine.CoinFlipService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PanpourEffect implements AttackEffect {

    private static final String WATER_SPLASH = "water splash";
    private static final int    HEADS_BONUS  = 20;

    private final CoinFlipService coinFlipService;

    public PanpourEffect(CoinFlipService coinFlipService) {
        this.coinFlipService = coinFlipService;
    }

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("panpour|water splash");
    }

    @Override
    public void apply(AttackContext ctx) {
        if (!WATER_SPLASH.equals(ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase() : "")) return;

        if (coinFlipService.flipAndEmit(ctx, ctx.getAction().getPlayerId()) == CoinResult.HEADS) {
            List<DamageModifier> modifiers = new ArrayList<>(
                    ctx.getModifiers() != null ? ctx.getModifiers() : new ArrayList<>());
            modifiers.add(new DamageModifier("water-splash-heads", HEADS_BONUS, true));
            ctx.setModifiers(modifiers);
        }
    }
}