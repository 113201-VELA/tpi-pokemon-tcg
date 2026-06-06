package com.pokemon.tcg.api.dto.response;

import java.util.UUID;

public record DeckCardResponseDTO(
        UUID id,
        CardResponseDTO card,
        int quantity
) {}
