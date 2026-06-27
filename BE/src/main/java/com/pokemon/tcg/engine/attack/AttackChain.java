package com.pokemon.tcg.engine.attack;

import com.pokemon.tcg.domain.strategy.attack.AttackContext;

import java.util.List;

public class AttackChain {

    private final List<AttackStep> steps;
    private int currentIndex = 0;

    public AttackChain(List<AttackStep> steps) {
        this.steps = steps;
    }

    public void next(AttackContext ctx) {
        if (!ctx.isCancelled() && currentIndex < steps.size()) {
            AttackStep step = steps.get(currentIndex++);
            step.execute(ctx, this);
        }
    }
}
