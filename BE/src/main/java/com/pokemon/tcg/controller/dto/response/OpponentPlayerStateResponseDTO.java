package com.pokemon.tcg.controller.dto.response;

import com.pokemon.tcg.domain.model.game.ActivePokemon;
import com.pokemon.tcg.domain.model.game.BenchPokemon;

import java.util.List;

public record OpponentPlayerStateResponseDTO(
        String playerId,
        String playerName,
        ActivePokemon active,
        List<BenchPokemon> bench,
        int cardsInHand,
        int deckCount,
        int prizesCount,
        List<CardResponseDTO> discardPile
) {}
