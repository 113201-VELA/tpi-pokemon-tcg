package com.pokemon.tcg.engine.attack;

public interface AttackStep {
    void execute(AttackContext ctx, AttackChain chain);
}
