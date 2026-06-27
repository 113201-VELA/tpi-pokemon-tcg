package com.pokemon.tcg.domain.model.game;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TurnFlags {
    private boolean energyAttachedThisTurn;
    private boolean retreatedThisTurn;
    private boolean supporterPlayedThisTurn;
    private boolean stadiumPlayedThisTurn;
    private boolean attackedThisTurn;
    private boolean isFirstTurnOfGame;
    @Builder.Default
    private Set<String> evolvedThisTurn = new HashSet<>();

    /**
     * Tracks which active abilities have been used this turn.
     * Each entry is a composite key: "instanceId|abilityName" (lowercase).
     * Allows multiple Pokémon to use their abilities independently in the same turn.
     * Cleared automatically when TurnFlags.fresh() is called at the start of each turn.
     */
    @Builder.Default
    private Set<String> abilitiesUsedThisTurn = new HashSet<>();

    public static TurnFlags fresh() {
        return TurnFlags.builder().build();
    }

    /**
     * Returns true if the ability identified by the given instanceId and abilityName
     * has already been used this turn.
     */
    public boolean isAbilityUsed(String instanceId, String abilityName) {
        String key = instanceId.toLowerCase() + "|" + abilityName.toLowerCase();
        return abilitiesUsedThisTurn.contains(key);
    }

    /**
     * Marks the ability identified by the given instanceId and abilityName
     * as used this turn.
     */
    public void markAbilityUsed(String instanceId, String abilityName) {
        String key = instanceId.toLowerCase() + "|" + abilityName.toLowerCase();
        abilitiesUsedThisTurn.add(key);
    }

    public boolean hasEvolvedThisTurn(String instanceId) {
        return evolvedThisTurn != null && evolvedThisTurn.contains(instanceId);
    }
    public void markEvolved(String instanceId) {
        if (evolvedThisTurn == null) evolvedThisTurn = new HashSet<>();
        evolvedThisTurn.add(instanceId);
    }
}