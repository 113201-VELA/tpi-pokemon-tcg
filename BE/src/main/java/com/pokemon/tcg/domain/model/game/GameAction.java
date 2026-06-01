package com.pokemon.tcg.domain.model.game;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GameAction {
    private GameActionType type;
    private String playerId;
    private Map<String, Object> payload;

    public String getPayloadString(String key) {
        return payload != null ? (String) payload.get(key) : null;
    }

    public Integer getPayloadInt(String key) {
        return payload != null ? (Integer) payload.get(key) : null;
    }
}
