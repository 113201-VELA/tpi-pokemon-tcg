package com.pokemon.tcg.domain.engine.attack.steps;

import com.pokemon.tcg.domain.engine.attack.AttackChain;
import com.pokemon.tcg.domain.engine.attack.AttackContext;
import com.pokemon.tcg.domain.engine.attack.AttackStep;
import org.springframework.stereotype.Component;

@Component
public class EnergyCheckStep implements AttackStep {

    @Override
    public void execute(AttackContext ctx, AttackChain chain) {
        chain.next(ctx);
    }
}
