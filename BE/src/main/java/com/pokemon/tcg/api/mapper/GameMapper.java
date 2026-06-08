package com.pokemon.tcg.api.mapper;

import com.pokemon.tcg.api.dto.response.GameResponseDTO;
import com.pokemon.tcg.domain.model.game.Game;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GameMapper {

    public GameResponseDTO toResponseDTO(Game game) {
        if (game == null) return null;

        String creator = game.getPlayers().stream()
                .filter(gp -> gp.getPlayerNumber() == 1)
                .map(gp -> gp.getPlayer().getUsername())
                .findFirst()
                .orElse("Desconocido");

        return new GameResponseDTO(
                game.getId(),
                game.getState(),
                creator,
                game.getPlayers().size(),
                game.getCreatedAt()
        );
    }

    public List<GameResponseDTO> toResponseDTOList(List<Game> games) {
        if (games == null) return List.of();
        return games.stream()
                .map(this::toResponseDTO)
                .toList();
    }
}
