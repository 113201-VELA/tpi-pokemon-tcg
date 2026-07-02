package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import com.pokemon.tcg.domain.strategy.attack.DamageModifier;
import com.pokemon.tcg.domain.model.game.CoinResult;
import com.pokemon.tcg.engine.CoinFlipService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * xy1-84 Doublade
 *
 * Dual Blades: flip 2 coins. This attack does 30 damage times the number
 *              of heads. Printed base damage is 0 — the whole damage
 *              comes from this variable modifier (same pattern used for
 *              Rhyperior's Rock Blast).
 */
@Component
public class DoubladeEffect implements AttackEffect {

    private static final String DUAL_BLADES = "dual blades";
    private static final int    COIN_FLIPS  = 2;
    private static final int    DAMAGE_PER_HEAD = 30;

    private final CoinFlipService coinFlipService;

    public DoubladeEffect(CoinFlipService coinFlipService) {
        this.coinFlipService = coinFlipService;
    }

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("doublade|dual blades");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        if (DUAL_BLADES.equals(attackName)) {
            applyDualBlades(ctx);
        }
    }

    /**
     * Flip 2 coins; add 30 damage per heads result via a damage modifier.
     */
    private void applyDualBlades(AttackContext ctx) {
        String attackerId = ctx.getAction().getPlayerId();

        int heads = 0;
        for (int i = 0; i < COIN_FLIPS; i++) {
            if (coinFlipService.flipAndEmit(ctx, attackerId) == CoinResult.HEADS) heads++;
        }

        if (heads == 0) return;

        List<DamageModifier> modifiers = new ArrayList<>(
                ctx.getModifiers() != null ? ctx.getModifiers() : new ArrayList<>());
        modifiers.add(new DamageModifier("dual-blades-heads", heads * DAMAGE_PER_HEAD, true));
        ctx.setModifiers(modifiers);
    }
}