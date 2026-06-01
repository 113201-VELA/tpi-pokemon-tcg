package com.pokemon.tcg.domain.engine;

public record DamageModifier(
    String source,
    int amount,
    boolean beforeWeakness
) {}
