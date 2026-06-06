package com.pokemon.tcg.api.dto.response;

import com.pokemon.tcg.domain.model.card.Ability;
import com.pokemon.tcg.domain.model.card.Attack;
import com.pokemon.tcg.domain.model.card.CardType;
import com.pokemon.tcg.domain.model.card.TypeModifier;

import java.util.List;

public record CardResponseDTO(
        String id,
        String setId,
        String name,
        CardType supertype,
        List<String> subtypes,
        Integer hp,
        List<String> types,
        String evolvesFrom,
        List<Attack> attacks,
        List<TypeModifier> weaknesses,
        List<TypeModifier> resistances,
        List<String> retreatCost,
        List<Ability> abilities,
        boolean basicEnergy,
        String imageSmall,
        String imageLarge,
        String rarity,
        String number
) {}
