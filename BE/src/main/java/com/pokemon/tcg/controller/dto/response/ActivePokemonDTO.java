package com.pokemon.tcg.controller.dto.response;

import com.pokemon.tcg.domain.model.game.SpecialCondition;

import java.util.List;
import java.util.Set;

public record ActivePokemonDTO(
        String instanceId,
        String cardId,
        CardResponseDTO card,
        List<CardResponseDTO> attachedEnergies,
        CardResponseDTO attachedTool,
        List<CardResponseDTO> evolutionStack,
        int damageCounters,
        int currentHp,
        int maxHp,
        Set<SpecialCondition> conditions,
        boolean enteredThisTurn,
        boolean canAttack,
        boolean canRetreat
) {}
