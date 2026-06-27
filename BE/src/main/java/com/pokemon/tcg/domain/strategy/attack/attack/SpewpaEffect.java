package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.engine.CoinFlipService;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import org.springframework.stereotype.Component;

@Component
public class SpewpaEffect implements AttackEffect {

    private final CoinFlipService coinFlipService;

    public SpewpaEffect(CoinFlipService coinFlipService) {
        this.coinFlipService = coinFlipService;
    }

    @Override
    public void apply(AttackContext ctx) {
        if (coinFlipService.flip() != CoinResult.HEADS) return;

        String attackerId     = ctx.getAction().getPlayerId();
        PlayerState opponent  = ctx.getBoardState().getOpponentState(attackerId);
        ActivePokemon defender = opponent.getActivePokemon();

        if (defender == null) return;

        defender.getConditions().remove(SpecialCondition.ASLEEP);
        defender.getConditions().remove(SpecialCondition.CONFUSED);
        defender.getConditions().add(SpecialCondition.PARALYZED);
    }
}
