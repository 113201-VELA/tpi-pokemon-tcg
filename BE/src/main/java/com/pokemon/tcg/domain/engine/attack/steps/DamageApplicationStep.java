package com.pokemon.tcg.domain.engine.attack.steps;

import com.pokemon.tcg.domain.engine.DamageCalculator;
import com.pokemon.tcg.domain.engine.attack.AttackChain;
import com.pokemon.tcg.domain.engine.attack.AttackContext;
import com.pokemon.tcg.domain.engine.attack.AttackStep;
import org.springframework.stereotype.Component;

@Component
public class DamageApplicationStep implements AttackStep {

    private final DamageCalculator damageCalculator;

    public DamageApplicationStep(DamageCalculator damageCalculator) {
        this.damageCalculator = damageCalculator;
    }

    @Override
    public void execute(AttackContext ctx, AttackChain chain) {
        chain.next(ctx);
    }
}
