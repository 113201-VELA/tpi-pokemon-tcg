package com.pokemon.tcg.controller.dto.response;

import java.util.List;
import java.util.UUID;

public record DeckResponseDTO(
        UUID id,
        String name,
        String cardBack,
        String coin,
        String featuredCardId,
        boolean valid,
        List<DeckCardResponseDTO> cards,
        int totalCardCount
) {}
