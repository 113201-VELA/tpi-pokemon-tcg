package com.pokemon.tcg.engine.attack;

import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.engine.attack.steps.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AttackPipeline {

    private final EnergyCheckStep       energyCheckStep;
    private final ConfusionCheckStep    confusionCheckStep;
    private final SelectionStep         selectionStep;
    private final PreAttackStep         preAttackStep;
    private final AttackModifierStep    attackModifierStep;
    private final AttackEffectStep      attackEffectStep;
    private final DamageApplicationStep damageApplicationStep;
    private final PostDamageEffectStep  postDamageEffectStep;

    public AttackPipeline(EnergyCheckStep energyCheckStep,
                          ConfusionCheckStep confusionCheckStep,
                          SelectionStep selectionStep,
                          PreAttackStep preAttackStep,
                          AttackModifierStep attackModifierStep,
                          AttackEffectStep attackEffectStep,
                          DamageApplicationStep damageApplicationStep,
                          PostDamageEffectStep postDamageEffectStep) {
        this.energyCheckStep       = energyCheckStep;
        this.confusionCheckStep    = confusionCheckStep;
        this.selectionStep         = selectionStep;
        this.preAttackStep         = preAttackStep;
        this.attackModifierStep    = attackModifierStep;
        this.attackEffectStep      = attackEffectStep;
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
                attackEffectStep,
                damageApplicationStep,
                postDamageEffectStep
        );
        new AttackChain(steps).next(ctx);
        return ctx;
    }
}
