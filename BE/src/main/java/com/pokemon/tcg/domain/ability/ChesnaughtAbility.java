package com.pokemon.tcg.domain.ability;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.engine.ability.PassiveAbilityEffect;
import com.pokemon.tcg.engine.attack.AttackContext;
import org.springframework.stereotype.Component;

@Component
public class ChesnaughtAbility implements PassiveAbilityEffect {

    private static final int SPIKY_SHIELD_COUNTERS = 3;

    @Override
    public void onDamageReceived(AttackContext ctx, ActivePokemon defender) {
        if (ctx.getDamageToApply() <= 0) return;

        String attackerId         = ctx.getAction().getPlayerId();
        PlayerState attackerState = ctx.getBoardState().getStateFor(attackerId);
        ActivePokemon attacker    = attackerState.getActivePokemon();

        if (attacker == null) return;

        attacker.setDamageCounters(attacker.getDamageCounters() + SPIKY_SHIELD_COUNTERS);
    }
}
