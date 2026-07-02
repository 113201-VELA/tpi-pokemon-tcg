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
    private List<PokemonEffect> activeEffects = new ArrayList<>();

    @Builder.Default
    private String blockedAttackName = null;

    @Builder.Default
    private List<TypeModifier> weaknesses = new ArrayList<>();

    @Builder.Default
    private List<TypeModifier> resistances = new ArrayList<>();

    @Builder.Default
    private List<EnergyType> types = new ArrayList<>();

    /**
     * Tracks a self-buff granted by this Pokémon's own attack that applies
     * only the next time that same attack is used (e.g. Bisharp's Metal Wallop).
     * Set when the attack is used without a pending boost; consumed and cleared
     * on the following use. Not reset between turns like activeEffects.
     */
    @Builder.Default
    private String pendingAttackDamageBoostName = null;

    @Builder.Default
    private int pendingAttackDamageBoostAmount = 0;

    /**
     * Set on the Defending Pokémon by an attack like Malamar's Mental Panic:
     * the next time THIS Pokémon attempts to attack, a coin is flipped before
     * the attack resolves — tails cancels it. Consumed (cleared) on that first
     * attempt regardless of the coin result. Not reset between turns like
     * activeEffects, since it must survive through the opponent's turn.
     */
    @Builder.Default
    private boolean pendingAttackFailChance = false;

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