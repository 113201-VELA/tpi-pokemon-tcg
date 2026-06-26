package com.pokemon.tcg.domain.model.game;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BenchPokemon {
    private String instanceId;
    private String cardId;
    private List<String> attachedEnergyIds;
    private String attachedToolId;
    private List<String> evolutionStack;
    private int damageCounters;
    private boolean enteredThisTurn;
}