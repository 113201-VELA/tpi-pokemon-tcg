package com.pokemon.tcg.controller.dto.response;

import java.util.List;

public record OpponentPlayerStateResponseDTO(
        String playerId,
        String playerName,
        ActivePokemonDTO active,
        List<BenchPokemonDTO> bench,
        int cardsInHand,
        int deckCount,
        int prizesCount,
        List<CardResponseDTO> discardPile
) {}
