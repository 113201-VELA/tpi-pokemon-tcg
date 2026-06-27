package com.pokemon.tcg.controller.websocket;

import com.pokemon.tcg.controller.dto.response.OwnPlayerStateResponseDTO;
import com.pokemon.tcg.controller.dto.response.PublicBoardStateDTO;
import com.pokemon.tcg.domain.model.game.BoardState;
import com.pokemon.tcg.domain.model.game.GameEvent;
import com.pokemon.tcg.domain.model.game.PlayerState;

public interface GameEventPublisher {

    void publishBoardState(String gameId, BoardState state);

    void publishBoardStateDTO(String gameId, PublicBoardStateDTO dto);

    void publishPrivateState(String gameId, String playerId, PlayerState playerState);

    void publishPrivateStateDTO(String gameId, String playerId, OwnPlayerStateResponseDTO dto);

    void publishEvent(String gameId, GameEvent event);

    void publishLobbyUpdate(GameEvent event);
}
