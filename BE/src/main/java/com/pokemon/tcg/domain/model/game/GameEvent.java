package com.pokemon.tcg.domain.model.game;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GameEvent {
    private GameEventType type;
    private String gameId;
    private String playerId;
    private int turnNumber;
    private Map<String, Object> data;
    private Instant occurredAt;
}
