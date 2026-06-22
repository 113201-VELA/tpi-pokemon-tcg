package com.pokemon.tcg.engine.attack.steps;

import com.pokemon.tcg.engine.attack.AttackChain;
import com.pokemon.tcg.engine.attack.AttackContext;
import com.pokemon.tcg.engine.attack.AttackStep;
import org.springframework.stereotype.Component;

@Component
public class AttackModifierStep implements AttackStep {

    @Override
    public void execute(AttackContext ctx, AttackChain chain) {
        chain.next(ctx);
    }
}
