package com.pokemon.tcg.domain.strategy.attack;

import java.util.List;

public interface AttackEffect {

    void apply(AttackContext ctx);

    List<String> getSupportedAttacks();
}
