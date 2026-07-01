package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import com.pokemon.tcg.domain.strategy.attack.DamageModifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class BisharpEffect implements AttackEffect {

    private static final String METAL_SOUND        = "metal sound";
    private static final String METAL_WALLOP        = "metal wallop";
    private static final int    METAL_WALLOP_BONUS  = 40;

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("bisharp|metal sound", "bisharp|metal wallop");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        switch (attackName) {
            case METAL_SOUND  -> applyMetalSound(ctx);
            case METAL_WALLOP -> applyMetalWallop(ctx);
            default           -> { }
        }
    }

    private void applyMetalSound(AttackContext ctx) {
        String attackerId = ctx.getAction().getPlayerId();
        PlayerState defenderState = ctx.getBoardState().getOpponentState(attackerId);

        ActivePokemon defender = defenderState.getActivePokemon();
        if (defender == null) return;

        Set<SpecialCondition> conditions = defender.getConditions() != null
                ? new HashSet<>(defender.getConditions())
                : new HashSet<>();
        conditions.add(SpecialCondition.CONFUSED);
        defender.setConditions(conditions);
    }

    private void applyMetalWallop(AttackContext ctx) {
        String attackerId = ctx.getAction().getPlayerId();
        PlayerState attackerState = ctx.getBoardState().getStateFor(attackerId);

        ActivePokemon attacker = attackerState.getActivePokemon();
        if (attacker == null) return;

        boolean hasPendingBoost = METAL_WALLOP.equals(attacker.getPendingAttackDamageBoostName());

        if (hasPendingBoost) {
            List<DamageModifier> modifiers = new ArrayList<>(
                    ctx.getModifiers() != null ? ctx.getModifiers() : new ArrayList<>());
            modifiers.add(new DamageModifier("metal-wallop-boost",
                    attacker.getPendingAttackDamageBoostAmount(), true));
            ctx.setModifiers(modifiers);

            attacker.setPendingAttackDamageBoostName(null);
            attacker.setPendingAttackDamageBoostAmount(0);
        } else {
            attacker.setPendingAttackDamageBoostName(METAL_WALLOP);
            attacker.setPendingAttackDamageBoostAmount(METAL_WALLOP_BONUS);
        }
    }
}