package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import com.pokemon.tcg.engine.CoinFlipService;
import com.pokemon.tcg.engine.StatusEffectManager;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FrogadierEffect implements AttackEffect {

    private static final String LICK = "lick";

    private final CoinFlipService coinFlipService;
    private final StatusEffectManager statusEffectManager;

    public FrogadierEffect(CoinFlipService coinFlipService, StatusEffectManager statusEffectManager) {
        this.coinFlipService = coinFlipService;
        this.statusEffectManager = statusEffectManager;
    }

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("frogadier|lick");
    }

    @Override
    public void apply(AttackContext ctx) {
        if (!LICK.equals(ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase() : "")) return;

        if (coinFlipService.flipAndEmit(ctx, ctx.getAction().getPlayerId()) != CoinResult.HEADS) return;

        String attackerId    = ctx.getAction().getPlayerId();
        PlayerState opponent = ctx.getBoardState().getOpponentState(attackerId);
        ActivePokemon defender = opponent.getActivePokemon();

        if (defender == null) return;

        statusEffectManager.applyCondition(defender, SpecialCondition.PARALYZED);
    }
}