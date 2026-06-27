package com.pokemon.tcg.controller.websocket;

import com.pokemon.tcg.domain.model.game.GameAction;
import com.pokemon.tcg.service.GameService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Controller
public class GameActionHandler {

    private final GameService gameService;

    public GameActionHandler(GameService gameService) {
        this.gameService = gameService;
    }

    @MessageMapping("/game/{gameId}/action")
    public void handleAction(@DestinationVariable String gameId,
                             @Payload GameAction action,
                             Principal principal) {
        if (principal == null) return;

        UUID playerId = UUID.fromString(principal.getName());

        GameAction actionWithPlayer = GameAction.builder()
                .type(action.getType())
                .playerId(playerId.toString())
                .payload(action.getPayload())
                .build();

        gameService.processAction(
                UUID.fromString(gameId), playerId, actionWithPlayer);
    }
}
