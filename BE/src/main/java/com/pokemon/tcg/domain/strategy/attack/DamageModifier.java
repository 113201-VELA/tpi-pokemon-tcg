package com.pokemon.tcg.domain.strategy.attack;

public record DamageModifier(
    String source,
    int amount,
    boolean beforeWeakness
) {}
