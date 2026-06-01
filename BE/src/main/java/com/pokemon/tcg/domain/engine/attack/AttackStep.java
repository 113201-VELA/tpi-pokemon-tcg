package com.pokemon.tcg.domain.engine.attack;

public interface AttackStep {
    void execute(AttackContext ctx, AttackChain chain);
}
