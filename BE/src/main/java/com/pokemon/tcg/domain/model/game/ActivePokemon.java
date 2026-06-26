package com.pokemon.tcg.domain.model.game;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.card.TypeModifier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
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

    @Builder.Default
    private List<TypeModifier> weaknesses = new ArrayList<>();

    @Builder.Default
    private List<TypeModifier> resistances = new ArrayList<>();

    @Builder.Default
    private List<EnergyType> types = new ArrayList<>();

    public int getCurrentHp(int maxHp) {
        return Math.max(0, maxHp - damageCounters * 10);
    }

    public boolean isKnockedOut(int maxHp) {
        return damageCounters * 10 >= maxHp;
    }

    public boolean hasCondition(SpecialCondition condition) {
        return conditions != null && conditions.contains(condition);
    }

    @JsonIgnore
    public boolean canAttack() {
        return !hasCondition(SpecialCondition.ASLEEP)
                && !hasCondition(SpecialCondition.PARALYZED);
    }

    @JsonIgnore
    public boolean canRetreat() {
        return !hasCondition(SpecialCondition.ASLEEP)
                && !hasCondition(SpecialCondition.PARALYZED);
    }
}