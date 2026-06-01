package com.pokemon.tcg.domain.model.game;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Set;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ActivePokemon {
    private String instanceId;
    private String cardId;
    private List<String> attachedEnergyIds;
    private String attachedToolId;
    private List<String> evolutionStack;
    private int damageCounters;
    private Set<SpecialCondition> conditions;
    private boolean enteredThisTurn;

    public int getCurrentHp(int maxHp) {
        return Math.max(0, maxHp - damageCounters * 10);
    }

    public boolean isKnockedOut(int maxHp) {
        return damageCounters * 10 >= maxHp;
    }

    public boolean hasCondition(SpecialCondition condition) {
        return conditions != null && conditions.contains(condition);
    }

    public boolean canAttack() {
        return !hasCondition(SpecialCondition.ASLEEP)
            && !hasCondition(SpecialCondition.PARALYZED);
    }

    public boolean canRetreat() {
        return !hasCondition(SpecialCondition.ASLEEP)
            && !hasCondition(SpecialCondition.PARALYZED);
    }
}
