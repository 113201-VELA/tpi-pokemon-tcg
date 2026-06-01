package com.pokemon.tcg.api.websocket;

import com.pokemon.tcg.application.GameService;
import com.pokemon.tcg.domain.model.game.GameAction;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

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
    }
}
