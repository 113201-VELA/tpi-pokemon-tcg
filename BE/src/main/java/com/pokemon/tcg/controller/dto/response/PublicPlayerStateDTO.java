package com.pokemon.tcg.controller.dto.response;

import java.util.List;

public record PublicPlayerStateDTO(
        String playerId,
        String playerName,
        String cardBack,
        String coin,
        ActivePokemonDTO active,
        List<BenchPokemonDTO> bench,
        List<CardResponseDTO> discardPile,
        int cardsInHand,
        int deckCount,
        int prizesCount,
        int totalMulligans,
        int mulliganBonusDraws,
        boolean setupConfirmed,
        int benchCount
) {}
