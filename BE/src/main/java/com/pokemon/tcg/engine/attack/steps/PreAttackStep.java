package com.pokemon.tcg.engine.attack.steps;

import com.pokemon.tcg.engine.CoinFlipService;
import com.pokemon.tcg.engine.attack.AttackChain;
import com.pokemon.tcg.engine.attack.AttackContext;
import com.pokemon.tcg.engine.attack.AttackStep;
import org.springframework.stereotype.Component;

@Component
public class PreAttackStep implements AttackStep {

    private final CoinFlipService coinFlipService;

    public PreAttackStep(CoinFlipService coinFlipService) {
        this.coinFlipService = coinFlipService;
    }

    @Override
    public void execute(AttackContext ctx, AttackChain chain) {
        chain.next(ctx);
    }
}
