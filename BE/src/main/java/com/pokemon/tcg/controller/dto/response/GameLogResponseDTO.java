package com.pokemon.tcg.controller.dto.response;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for a single game log entry.
 * Exposes only serializable fields — avoids lazy-loaded JPA proxies
 * for game and player relations.
 */
public record GameLogResponseDTO(
        UUID id,
        UUID gameId,
        UUID playerId,
        int turnNumber,
        String actionType,
        Map<String, Object> actionData,
        String result,
        Map<String, Object> resultData,
        Instant createdAt
) {}