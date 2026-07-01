package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class Aegislash85Effect implements AttackEffect {

    private static final String BUSTER_SWING = "buster swing";

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("aegislash|buster swing");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        if (BUSTER_SWING.equals(attackName)) {
            ctx.setIgnoreResistance(true);
        }
    }
}