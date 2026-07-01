package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class Inkay75Effect implements AttackEffect {

    private static final String PUNCTURE = "puncture";

    @Override
    public List<String> getSupportedAttacks() {
        // Tackle has no special text; base damage is resolved by the pipeline directly.
        return List.of("inkay|puncture");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        if (PUNCTURE.equals(attackName)) {
            ctx.setIgnoreResistance(true);
        }
    }
}