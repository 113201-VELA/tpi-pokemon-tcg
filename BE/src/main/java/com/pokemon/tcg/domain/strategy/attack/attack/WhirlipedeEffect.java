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
public class WhirlipedeEffect implements AttackEffect {

    private static final String CONTINUOUS_TUMBLE  = "continuous tumble";
    private static final int    DAMAGE_PER_HEAD     = 30;

    private final CoinFlipService coinFlipService;

    public WhirlipedeEffect(CoinFlipService coinFlipService) {
        this.coinFlipService = coinFlipService;
    }

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("whirlipede|continuous tumble");
    }

    @Override
    public void apply(AttackContext ctx) {
        if (!CONTINUOUS_TUMBLE.equals(ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase() : "")) return;

        int heads = 0;
        while (coinFlipService.flipAndEmit(ctx, ctx.getAction().getPlayerId()) == CoinResult.HEADS) {
            heads++;
        }

        if (heads > 0) {
            List<DamageModifier> modifiers = new ArrayList<>(
                    ctx.getModifiers() != null ? ctx.getModifiers() : new ArrayList<>());
            modifiers.add(new DamageModifier(
                    "continuous-tumble-heads", heads * DAMAGE_PER_HEAD, true));
            ctx.setModifiers(modifiers);
        }
    }
}