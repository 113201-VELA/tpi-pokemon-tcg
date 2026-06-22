package com.pokemon.tcg.controller.dto.response;

import com.pokemon.tcg.domain.model.game.ActivePokemon;
import com.pokemon.tcg.domain.model.game.BenchPokemon;

import java.util.List;

public record OwnPlayerStateResponseDTO(
        String playerId,
        String playerName,
        ActivePokemon active,
        List<BenchPokemon> bench,
        List<CardResponseDTO> hand,
        int deckCount,
        List<CardResponseDTO> prizes,
        List<CardResponseDTO> discardPile
) {}
