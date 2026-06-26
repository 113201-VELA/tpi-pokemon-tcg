package com.pokemon.tcg.controller.dto.response;

import java.util.List;

public record OwnPlayerStateResponseDTO(
        String playerId,
        String playerName,
        String cardBack,
        String coin,
        ActivePokemonDTO active,
        List<BenchPokemonDTO> bench,
        List<CardResponseDTO> hand,
        int deckCount,
        List<CardResponseDTO> prizes,
        List<CardResponseDTO> discardPile
) {}
