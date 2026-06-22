package com.pokemon.tcg.controller.websocket;

import com.pokemon.tcg.domain.model.game.BoardState;
import com.pokemon.tcg.domain.model.game.GameEvent;
import com.pokemon.tcg.domain.model.game.PlayerState;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class StompEventPublisher implements GameEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public StompEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void publishBoardState(String gameId, BoardState state) {
        messagingTemplate.convertAndSend(
            "/topic/game/" + gameId + "/state", state);
    }

    @Override
    public void publishPrivateState(String gameId, String playerId, PlayerState playerState) {
        messagingTemplate.convertAndSendToUser(
            playerId,
            "/queue/game/" + gameId + "/player",
            playerState);
    }

    @Override
    public void publishEvent(String gameId, GameEvent event) {
        messagingTemplate.convertAndSend(
            "/topic/game/" + gameId + "/events", event);
    }

    @Override
    public void publishLobbyUpdate(GameEvent event) {
        messagingTemplate.convertAndSend("/topic/lobby", event);
    }
}
