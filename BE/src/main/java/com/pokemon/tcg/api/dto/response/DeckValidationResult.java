package com.pokemon.tcg.api.dto.response;

public record DeckValidationResult(
        boolean valid,
        int totalCards,
        boolean exactly60,
        boolean noExcessCopies,
        boolean hasBasicPokemon
) {}
