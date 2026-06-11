package com.pokemon.tcg.api.dto.response;

import java.util.List;
import java.util.UUID;

public record DeckResponseDTO(
        UUID id,
        String name,
        String cardBack,
        String coin,
        boolean valid,
        List<DeckCardResponseDTO> cards,
        int totalCardCount
) {}
