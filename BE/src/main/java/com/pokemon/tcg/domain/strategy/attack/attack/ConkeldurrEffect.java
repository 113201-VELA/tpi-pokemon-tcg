package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import com.pokemon.tcg.domain.strategy.attack.DamageModifier;
import com.pokemon.tcg.engine.CoinFlipService;
import com.pokemon.tcg.engine.StatusEffectManager;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * xy1-67 Conkeldurr
 *
 * Wake-Up Slap: 30 damage + 60 more if the opponent's Active Pokémon has a
 *               Special Condition. Then remove all Special Conditions from it.
 * Dynamic Punch: 60 damage. Flip a coin; if heads, 40 more damage and the
 *                opponent's Active Pokémon is now Confused.
 */
@Component
public class ConkeldurrEffect implements AttackEffect {

    private static final String WAKE_UP_SLAP   = "wake-up slap";
    private static final String DYNAMIC_PUNCH  = "dynamic punch";

    private final CoinFlipService coinFlipService;
    private final StatusEffectManager statusEffectManager;

    public ConkeldurrEffect(CoinFlipService coinFlipService, StatusEffectManager statusEffectManager) {
        this.coinFlipService = coinFlipService;
        this.statusEffectManager = statusEffectManager;
    }

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("conkeldurr|wake-up slap", "conkeldurr|dynamic punch");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        switch (attackName) {
            case WAKE_UP_SLAP  -> applyWakeUpSlap(ctx);
            case DYNAMIC_PUNCH -> applyDynamicPunch(ctx);
            default            -> { }
        }
    }

    /**
     * Wake-Up Slap: if the defender has any Special Condition, add 60 damage
     * via a pre-weakness modifier, then clear all its Special Conditions.
     */
    private void applyWakeUpSlap(AttackContext ctx) {
        String attackerId    = ctx.getAction().getPlayerId();
        PlayerState opponent = ctx.getBoardState().getOpponentState(attackerId);
        ActivePokemon defender = opponent.getActivePokemon();

        if (defender == null) return;

        boolean hasCondition = defender.getConditions() != null
                && !defender.getConditions().isEmpty();

        if (hasCondition) {
            List<DamageModifier> modifiers = new ArrayList<>(
                    ctx.getModifiers() != null ? ctx.getModifiers() : new ArrayList<>());
            modifiers.add(new DamageModifier("wake-up-slap-condition", 60, true));
            ctx.setModifiers(modifiers);

            defender.setConditions(new HashSet<>());
        }
    }

    /**
     * Dynamic Punch: flip a coin; on heads add 40 damage and apply CONFUSED
     * to the opponent's Active Pokémon.
     */
    private void applyDynamicPunch(AttackContext ctx) {
        if (coinFlipService.flipAndEmit(ctx, ctx.getAction().getPlayerId()) != CoinResult.HEADS) return;

        String attackerId    = ctx.getAction().getPlayerId();
        PlayerState opponent = ctx.getBoardState().getOpponentState(attackerId);
        ActivePokemon defender = opponent.getActivePokemon();

        if (defender == null) return;

        List<DamageModifier> modifiers = new ArrayList<>(
                ctx.getModifiers() != null ? ctx.getModifiers() : new ArrayList<>());
        modifiers.add(new DamageModifier("dynamic-punch-heads", 40, true));
        ctx.setModifiers(modifiers);

        statusEffectManager.applyCondition(defender, SpecialCondition.CONFUSED);
    }
}