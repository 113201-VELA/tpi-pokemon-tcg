package com.pokemon.tcg.domain.engine.attack;

import com.pokemon.tcg.domain.engine.attack.steps.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AttackPipeline {

    private final EnergyCheckStep      energyCheckStep;
    private final ConfusionCheckStep   confusionCheckStep;
    private final SelectionStep        selectionStep;
    private final PreAttackStep        preAttackStep;
    private final AttackModifierStep   attackModifierStep;
    private final DamageApplicationStep damageApplicationStep;
    private final PostDamageEffectStep postDamageEffectStep;

    public AttackPipeline(EnergyCheckStep energyCheckStep,
                          ConfusionCheckStep confusionCheckStep,
                          SelectionStep selectionStep,
                          PreAttackStep preAttackStep,
                          AttackModifierStep attackModifierStep,
                          DamageApplicationStep damageApplicationStep,
                          PostDamageEffectStep postDamageEffectStep) {
        this.energyCheckStep       = energyCheckStep;
        this.confusionCheckStep    = confusionCheckStep;
        this.selectionStep         = selectionStep;
        this.preAttackStep         = preAttackStep;
        this.attackModifierStep    = attackModifierStep;
        this.damageApplicationStep = damageApplicationStep;
        this.postDamageEffectStep  = postDamageEffectStep;
    }

    public AttackContext execute(AttackContext ctx) {
        List<AttackStep> steps = List.of(
            energyCheckStep,
            confusionCheckStep,
            selectionStep,
            preAttackStep,
            attackModifierStep,
            damageApplicationStep,
            postDamageEffectStep
        );
        new AttackChain(steps).next(ctx);
        return ctx;
    }
}
