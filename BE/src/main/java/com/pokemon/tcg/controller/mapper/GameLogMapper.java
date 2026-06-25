package com.pokemon.tcg.controller.mapper;

import com.pokemon.tcg.controller.dto.response.GameLogResponseDTO;
import com.pokemon.tcg.domain.model.game.GameLogEntry;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Maps {@link GameLogEntry} JPA entities to {@link GameLogResponseDTO} response objects.
 *
 * <p>Extracting this logic from the DTO keeps the record a pure data carrier
 * and centralizes mapping concerns in the mapper layer, consistent with
 * {@link CardMapper}, {@link GameMapper}, and {@link GameStateMapper}.
 */
@Component
public class GameLogMapper {

    /**
     * Maps a single {@link GameLogEntry} to a {@link GameLogResponseDTO}.
     * Safely handles null game and player relations (lazy-loaded JPA proxies).
     */
    public GameLogResponseDTO toResponseDTO(GameLogEntry entry) {
        return new GameLogResponseDTO(
                entry.getId(),
                entry.getGame() != null ? entry.getGame().getId() : null,
                entry.getPlayer() != null ? entry.getPlayer().getId() : null,
                entry.getTurnNumber(),
                entry.getActionType(),
                entry.getActionData(),
                entry.getResult(),
                entry.getResultData(),
                entry.getCreatedAt()
        );
    }

    /**
     * Maps a list of {@link GameLogEntry} entities to a list of {@link GameLogResponseDTO}.
     */
    public List<GameLogResponseDTO> toResponseDTOList(List<GameLogEntry> entries) {
        return entries.stream()
                .map(this::toResponseDTO)
                .toList();
    }
}