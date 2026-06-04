package com.pokemon.tcg.api.dto;

public record DeckValidationResult(
        boolean valid,
        int totalCards,
        boolean exactly60,
        boolean noExcessCopies,
        boolean hasBasicPokemon
) {}