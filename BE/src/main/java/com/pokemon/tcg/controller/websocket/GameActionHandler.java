package com.pokemon.tcg.controller.websocket;

import com.pokemon.tcg.service.GameService;
import com.pokemon.tcg.domain.model.game.BoardState;
import com.pokemon.tcg.domain.model.game.EngineResult;
import com.pokemon.tcg.domain.model.game.GameAction;
import com.pokemon.tcg.domain.model.game.GameEvent;
import com.pokemon.tcg.domain.model.game.PlayerState;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Controller
public class GameActionHandler {

    private final GameService gameService;
    private final GameEventPublisher eventPublisher;

    public GameActionHandler(GameService gameService, GameEventPublisher eventPublisher) {
        this.gameService = gameService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Receives a game action from a player via WebSocket,
     * processes it through the engine and publishes the resulting
     * board state and events to all subscribers.
     */
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

        EngineResult result = gameService.processAction(
                UUID.fromString(gameId), playerId, actionWithPlayer);

        if (result == null || result.newState() == null) return;

        // Instead of publishing the entire board state, publish only public information
        eventPublisher.publishBoardState(
                gameId,
                sanitizeForPublic(result.newState())
        );

        // Publish private state to each player separately
        eventPublisher.publishPrivateState(
                gameId,
                result.newState().getPlayer1State().getPlayerId(),
                result.newState().getPlayer1State());

        eventPublisher.publishPrivateState(
                gameId,
                result.newState().getPlayer2State().getPlayerId(),
                result.newState().getPlayer2State());

        // Publish each event
        if (result.events() != null) {
            for (GameEvent event : result.events()) {
                eventPublisher.publishEvent(gameId, event);
            }
        }
    }

    /**
     * Removes sensitive information (hand, deck contents, prizes)
     * from the board state before broadcasting to all subscribers.
     * Only public information (active Pokémon, bench, counts) is sent.
     */
    private BoardState sanitizeForPublic(BoardState state) {

        PlayerState p1 = PlayerState.builder()
                .playerId(state.getPlayer1State().getPlayerId())
                .activePokemon(state.getPlayer1State().getActivePokemon())
                .bench(state.getPlayer1State().getBench())
                .discard(state.getPlayer1State().getDiscard())
                .hand(List.of())
                .deck(List.of())
                .prizes(List.of())
                .totalMulligans(state.getPlayer1State().getTotalMulligans())
                .mulliganBonusDraws(state.getPlayer1State().getMulliganBonusDraws())
                .setupConfirmed(state.getPlayer1State().isSetupConfirmed())
                .build();

        PlayerState p2 = PlayerState.builder()
                .playerId(state.getPlayer2State().getPlayerId())
                .activePokemon(state.getPlayer2State().getActivePokemon())
                .bench(state.getPlayer2State().getBench())
                .discard(state.getPlayer2State().getDiscard())
                .hand(List.of())
                .deck(List.of())
                .prizes(List.of())
                .totalMulligans(state.getPlayer2State().getTotalMulligans())
                .mulliganBonusDraws(state.getPlayer2State().getMulliganBonusDraws())
                .setupConfirmed(state.getPlayer2State().isSetupConfirmed())
                .build();

        return BoardState.builder()
                .gameId(state.getGameId())
                .gameState(state.getGameState())
                .turnPhase(state.getTurnPhase())
                .currentPlayerId(state.getCurrentPlayerId())
                .turnNumber(state.getTurnNumber())
                .player1State(p1)
                .player2State(p2)
                .activeStadiumCardId(state.getActiveStadiumCardId())
                .turnFlags(state.getTurnFlags())
                .pendingEvents(state.getPendingEvents())
                .bonusDrawPending(state.isBonusDrawPending())
                .build();
    }
}