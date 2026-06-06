package com.pokemon.tcg.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record AddCardRequest(
        @NotBlank
        String cardId,

        @Min(1) @Max(60)
        int quantity
) {}
