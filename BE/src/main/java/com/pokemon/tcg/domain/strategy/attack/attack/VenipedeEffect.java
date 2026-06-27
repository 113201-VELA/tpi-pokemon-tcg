package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class VenipedeEffect implements AttackEffect {

    private static final String POISON_STING = "poison sting";

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("venipede|poison sting");
    }

    @Override
    public void apply(AttackContext ctx) {
        if (!POISON_STING.equals(ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase() : "")) return;

        String attackerId    = ctx.getAction().getPlayerId();
        PlayerState opponent = ctx.getBoardState().getOpponentState(attackerId);
        ActivePokemon defender = opponent.getActivePokemon();

        if (defender == null) return;

        defender.getConditions().add(SpecialCondition.POISONED);
    }
}