package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.engine.CoinFlipService;
import com.pokemon.tcg.engine.StatusEffectManager;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SpewpaEffect implements AttackEffect {

    private final CoinFlipService coinFlipService;

    private final StatusEffectManager statusEffectManager;

    public SpewpaEffect(CoinFlipService coinFlipService, StatusEffectManager statusEffectManager) {
        this.coinFlipService = coinFlipService;
        this.statusEffectManager = statusEffectManager;
    }

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("spewpa|stun spore");
    }

    @Override
    public void apply(AttackContext ctx) {
        if (coinFlipService.flipAndEmit(ctx, ctx.getAction().getPlayerId()) != CoinResult.HEADS) return;

        String attackerId     = ctx.getAction().getPlayerId();
        PlayerState opponent  = ctx.getBoardState().getOpponentState(attackerId);
        ActivePokemon defender = opponent.getActivePokemon();

        if (defender == null) return;

        statusEffectManager.applyCondition(defender, SpecialCondition.PARALYZED);
    }
}
