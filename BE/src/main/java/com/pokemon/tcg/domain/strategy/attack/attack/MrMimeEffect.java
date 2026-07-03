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
 * xy1-91 Mr. Mime
 *
 * Massage: heal 60 damage from 1 of your Benched Pokémon.
 * Slap Down: 40+ damage. Flip 2 coins. This attack does 20 more damage
 *            for each heads.
 */
@Component
public class MrMimeEffect implements AttackEffect {

    private static final String MASSAGE   = "massage";
    private static final String SLAP_DOWN = "slap down";
    private static final int    COIN_FLIPS = 2;
    private static final int    DAMAGE_PER_HEAD = 20;
    private static final int    HEAL_AMOUNT   = 60;
    private static final int    COUNTERS_HEAL = HEAL_AMOUNT / 10;

    private final CoinFlipService coinFlipService;

    public MrMimeEffect(CoinFlipService coinFlipService) {
        this.coinFlipService = coinFlipService;
    }

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("mr. mime|massage", "mr. mime|slap down");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        switch (attackName) {
            case MASSAGE   -> applyMassage(ctx);
            case SLAP_DOWN -> applySlapDown(ctx);
            default        -> { }
        }
    }

    /**
     * Heal 60 damage (6 counters) from one of the attacker's own Benched
     * Pokémon, specified via {@code targetInstanceId}. Minimum 0 counters,
     * same pattern as SuperPotionEffect / ChesnaughtEffect.
     */
    private void applyMassage(AttackContext ctx) {
        String attackerId    = ctx.getAction().getPlayerId();
        PlayerState attacker = ctx.getBoardState().getStateFor(attackerId);
        String targetInstanceId = ctx.getAction().getPayloadString("targetInstanceId");

        if (targetInstanceId == null || attacker.getBench() == null) return;

        attacker.getBench().stream()
                .filter(b -> b.getInstanceId().equals(targetInstanceId))
                .findFirst()
                .ifPresent(b -> {
                    int newCounters = Math.max(0, b.getDamageCounters() - COUNTERS_HEAL);
                    b.setDamageCounters(newCounters);
                });
    }

    /**
     * Flip 2 coins; add 20 damage per heads result on top of the printed
     * 40 base damage.
     */
    private void applySlapDown(AttackContext ctx) {
        String attackerId = ctx.getAction().getPlayerId();

        int heads = 0;
        for (int i = 0; i < COIN_FLIPS; i++) {
            if (coinFlipService.flipAndEmit(ctx, attackerId) == CoinResult.HEADS) heads++;
        }

        if (heads == 0) return;

        List<DamageModifier> modifiers = new ArrayList<>(
                ctx.getModifiers() != null ? ctx.getModifiers() : new ArrayList<>());
        modifiers.add(new DamageModifier("slap-down-heads", heads * DAMAGE_PER_HEAD, true));
        ctx.setModifiers(modifiers);
    }
}