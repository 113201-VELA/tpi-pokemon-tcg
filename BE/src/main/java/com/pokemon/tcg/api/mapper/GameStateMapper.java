package com.pokemon.tcg.api.mapper;

import com.pokemon.tcg.api.dto.response.CardResponseDTO;
import com.pokemon.tcg.api.dto.response.GameStateResponseDTO;
import com.pokemon.tcg.api.dto.response.OpponentPlayerStateResponseDTO;
import com.pokemon.tcg.api.dto.response.OwnPlayerStateResponseDTO;
import com.pokemon.tcg.domain.model.card.Card;
import com.pokemon.tcg.domain.model.game.BoardState;
import com.pokemon.tcg.domain.model.game.PlayerState;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class GameStateMapper {

    private final CardMapper cardMapper;

    public GameStateMapper(CardMapper cardMapper) {
        this.cardMapper = cardMapper;
    }

    public GameStateResponseDTO toGameStateResponse(
            BoardState boardState,
            String requestingPlayerId,
            String requestingPlayerName,
            String opponentPlayerName,
            Map<String, Card> cardCache
    ) {
        if (boardState == null) return null;

        PlayerState ownState = boardState.getStateFor(requestingPlayerId);
        PlayerState opponentState = boardState.getOpponentState(requestingPlayerId);

        return new GameStateResponseDTO(
                boardState.getGameId(),
                boardState.getGameState(),
                boardState.getTurnPhase(),
                boardState.getCurrentPlayerId(),
                boardState.getTurnNumber(),
                boardState.getActiveStadiumCardId(),
                boardState.getTurnFlags(),
                safeList(boardState.getPendingEvents()),
                toOwnPlayerState(ownState, requestingPlayerName, cardCache),
                toOpponentPlayerState(opponentState, opponentPlayerName, cardCache)
        );
    }

    private OwnPlayerStateResponseDTO toOwnPlayerState(
            PlayerState state, String playerName, Map<String, Card> cardCache) {
        if (state == null) return null;
        return new OwnPlayerStateResponseDTO(
                state.getPlayerId(),
                playerName,
                state.getActivePokemon(),
                safeList(state.getBench()),
                resolveCards(state.getHand(), cardCache),
                state.getDeckSize(),
                resolveCards(state.getPrizes(), cardCache),
                resolveCards(state.getDiscard(), cardCache)
        );
    }

    private OpponentPlayerStateResponseDTO toOpponentPlayerState(
            PlayerState state, String playerName, Map<String, Card> cardCache) {
        if (state == null) return null;
        return new OpponentPlayerStateResponseDTO(
                state.getPlayerId(),
                playerName,
                state.getActivePokemon(),
                safeList(state.getBench()),
                state.getHandSize(),
                state.getDeckSize(),
                state.getPrizeCount(),
                resolveCards(state.getDiscard(), cardCache)
        );
    }

    private List<CardResponseDTO> resolveCards(List<String> cardIds, Map<String, Card> cardCache) {
        if (cardIds == null) return Collections.emptyList();
        return cardIds.stream()
                .map(id -> cardCache != null ? cardCache.get(id) : null)
                .filter(Objects::nonNull)
                .map(cardMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    private <T> List<T> safeList(List<T> list) {
        return list != null ? list : Collections.emptyList();
    }
}
