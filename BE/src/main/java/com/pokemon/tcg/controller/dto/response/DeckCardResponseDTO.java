package com.pokemon.tcg.controller.dto.response;

import java.util.UUID;

public record DeckCardResponseDTO(
        UUID id,
        CardResponseDTO card,
        int quantity
) {}
