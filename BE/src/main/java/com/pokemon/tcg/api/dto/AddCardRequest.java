package com.pokemon.tcg.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for adding or updating a card in a deck.
 * Separated from CreateDeckRequest and UpdateDeckRequest because
 * adding a card is a distinct operation that requires a card ID
 * and a quantity, which are unrelated to deck metadata (name, description).
 */
public record AddCardRequest(
        @NotBlank
        String cardId,

        @Min(1) @Max(60)
        int quantity
) {}