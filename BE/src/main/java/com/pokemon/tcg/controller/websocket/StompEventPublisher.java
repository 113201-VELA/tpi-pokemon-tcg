package com.pokemon.tcg.controller.websocket;

import com.pokemon.tcg.controller.dto.response.OwnPlayerStateResponseDTO;
import com.pokemon.tcg.controller.dto.response.PublicBoardStateDTO;
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
    public void publishBoardStateDTO(String gameId, PublicBoardStateDTO dto) {
        messagingTemplate.convertAndSend(
                "/topic/game/" + gameId + "/state", dto);
    }

    @Override
    public void publishPrivateState(String gameId, String playerId, PlayerState playerState) {
        System.out.println("Publishing private state to playerId: " + playerId);

        messagingTemplate.convertAndSendToUser(
            playerId,
            "/queue/game/" + gameId + "/player",
            playerState);
    }

    @Override
    public void publishPrivateStateDTO(String gameId, String playerId, OwnPlayerStateResponseDTO dto) {
        messagingTemplate.convertAndSendToUser(
                playerId,
                "/queue/game/" + gameId + "/player",
                dto);
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
