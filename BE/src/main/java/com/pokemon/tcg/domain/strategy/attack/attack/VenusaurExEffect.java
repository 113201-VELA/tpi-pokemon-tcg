package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class VenusaurExEffect implements AttackEffect {

    private static final String POISON_POWDER  = "poison powder";
    private static final String JUNGLE_HAMMER  = "jungle hammer";
    private static final int    HEAL_COUNTERS  = 3;

    @Override
    public List<String> getSupportedAttacks() {
        return List.of(
                "venusaur-ex|poison powder",
                "venusaur-ex|jungle hammer"
        );
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        switch (attackName) {
            case POISON_POWDER -> applyPoisonPowder(ctx);
            case JUNGLE_HAMMER -> applyJungleHammer(ctx);
            default            -> { }
        }
    }

    private void applyPoisonPowder(AttackContext ctx) {
        String attackerId     = ctx.getAction().getPlayerId();
        PlayerState opponent  = ctx.getBoardState().getOpponentState(attackerId);
        ActivePokemon defender = opponent.getActivePokemon();

        if (defender == null) return;

        defender.getConditions().add(SpecialCondition.POISONED);
    }

    private void applyJungleHammer(AttackContext ctx) {
        String attackerId         = ctx.getAction().getPlayerId();
        PlayerState attackerState = ctx.getBoardState().getStateFor(attackerId);
        ActivePokemon venusaur    = attackerState.getActivePokemon();

        if (venusaur == null) return;

        int current = venusaur.getDamageCounters();
        venusaur.setDamageCounters(Math.max(0, current - HEAL_COUNTERS));
    }
}
