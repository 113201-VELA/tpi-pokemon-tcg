package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import com.pokemon.tcg.domain.strategy.attack.DamageModifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SkarmoryExEffect implements AttackEffect {

    private static final String JOUST                   = "joust";
    private static final String TAILSPIN_PILEDRIVER      = "tailspin piledriver";
    private static final int    TAILSPIN_BONUS_DAMAGE    = 40;

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("skarmory-ex|joust", "skarmory-ex|tailspin piledriver");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        switch (attackName) {
            case JOUST                -> applyJoust(ctx);
            case TAILSPIN_PILEDRIVER  -> applyTailspinPiledriver(ctx);
            default                   -> { }
        }
    }

    private void applyJoust(AttackContext ctx) {
        String attackerId = ctx.getAction().getPlayerId();
        PlayerState defenderState = ctx.getBoardState().getOpponentState(attackerId);

        ActivePokemon defender = defenderState.getActivePokemon();
        if (defender == null || defender.getAttachedToolId() == null) return;

        String toolId = defender.getAttachedToolId();
        defender.setAttachedToolId(null);

        List<String> discard = new ArrayList<>(
                defenderState.getDiscard() != null ? defenderState.getDiscard() : new ArrayList<>());
        discard.add(toolId);
        defenderState.setDiscard(discard);
    }

    private void applyTailspinPiledriver(AttackContext ctx) {
        String attackerId = ctx.getAction().getPlayerId();
        PlayerState defenderState = ctx.getBoardState().getOpponentState(attackerId);

        ActivePokemon defender = defenderState.getActivePokemon();
        if (defender == null || defender.getDamageCounters() <= 0) return;

        List<DamageModifier> modifiers = new ArrayList<>(
                ctx.getModifiers() != null ? ctx.getModifiers() : new ArrayList<>());
        modifiers.add(new DamageModifier("tailspin-piledriver-bonus", TAILSPIN_BONUS_DAMAGE, true));
        ctx.setModifiers(modifiers);
    }
}