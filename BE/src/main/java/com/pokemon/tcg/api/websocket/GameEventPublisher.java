package com.pokemon.tcg.api.websocket;

import com.pokemon.tcg.domain.model.game.BoardState;
import com.pokemon.tcg.domain.model.game.GameEvent;
import com.pokemon.tcg.domain.model.game.PlayerState;

public interface GameEventPublisher {

    void publishBoardState(String gameId, BoardState state);

    void publishPrivateState(String gameId, String playerId, PlayerState playerState);

    void publishEvent(String gameId, GameEvent event);

    void publishLobbyUpdate(GameEvent event);
}
