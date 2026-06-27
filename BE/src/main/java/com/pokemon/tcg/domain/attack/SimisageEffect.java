package com.pokemon.tcg.domain.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.engine.attack.AttackContext;
import com.pokemon.tcg.engine.attack.AttackEffect;
import org.springframework.stereotype.Component;

@Component
public class SimisageEffect implements AttackEffect {

    @Override
    public void apply(AttackContext ctx) {
        String blockedAttackName = ctx.getAction().getPayloadString("blockedAttackName");
        if (blockedAttackName == null || blockedAttackName.isBlank()) return;

        String attackerId     = ctx.getAction().getPlayerId();
        PlayerState opponent  = ctx.getBoardState().getOpponentState(attackerId);
        ActivePokemon defender = opponent.getActivePokemon();

        if (defender == null) return;

        defender.setBlockedAttackName(blockedAttackName);
    }
}
