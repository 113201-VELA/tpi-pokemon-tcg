package com.pokemon.tcg.engine;

public record DamageModifier(
    String source,
    int amount,
    boolean beforeWeakness
) {}
