package com.pokemon.tcg.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateCardQuantityRequest(
        @Min(1) @Max(60)
        int quantity
) {}
