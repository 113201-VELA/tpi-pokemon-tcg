package com.pokemon.tcg.api.dto.response;

import com.pokemon.tcg.domain.model.game.GameState;

import java.time.Instant;
import java.util.UUID;

public record GameResponseDTO(
        UUID id,
        GameState state,
        String creatorUsername,
        int playerCount,
        Instant createdAt
) {}
