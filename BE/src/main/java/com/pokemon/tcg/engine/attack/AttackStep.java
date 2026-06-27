package com.pokemon.tcg.engine.attack;

import com.pokemon.tcg.domain.strategy.attack.AttackContext;

public interface AttackStep {
    void execute(AttackContext ctx, AttackChain chain);
}
