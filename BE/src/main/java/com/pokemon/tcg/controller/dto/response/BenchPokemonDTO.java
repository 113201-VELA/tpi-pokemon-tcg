package com.pokemon.tcg.controller.dto.response;

import java.util.List;

public record BenchPokemonDTO(
        String instanceId,
        String cardId,
        CardResponseDTO card,
        List<CardResponseDTO> attachedEnergies,
        CardResponseDTO attachedTool,
        List<CardResponseDTO> evolutionStack,
        int damageCounters,
        int currentHp,
        int maxHp,
        boolean enteredThisTurn
) {}
