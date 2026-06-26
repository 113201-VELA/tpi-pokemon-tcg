package com.pokemon.tcg.controller.mapper;

import com.pokemon.tcg.controller.dto.response.ActivePokemonDTO;
import com.pokemon.tcg.controller.dto.response.BenchPokemonDTO;
import com.pokemon.tcg.controller.dto.response.CardResponseDTO;
import com.pokemon.tcg.controller.dto.response.GameStateResponseDTO;
import com.pokemon.tcg.controller.dto.response.OpponentPlayerStateResponseDTO;
import com.pokemon.tcg.controller.dto.response.OwnPlayerStateResponseDTO;
import com.pokemon.tcg.domain.model.card.Card;
import com.pokemon.tcg.domain.model.game.ActivePokemon;
import com.pokemon.tcg.domain.model.game.BenchPokemon;
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
            String ownCardBack,
            String ownCoin,
            String opponentCardBack,
            String opponentCoin,
            Map<String, Card> cardCache
    ) {
        if (boardState == null) return null;

        PlayerState ownState      = boardState.getStateFor(requestingPlayerId);
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
                toOwnPlayerState(ownState, requestingPlayerName, ownCardBack, ownCoin, cardCache),
                toOpponentPlayerState(opponentState, opponentPlayerName, opponentCardBack, opponentCoin, cardCache)
        );
    }

    private OwnPlayerStateResponseDTO toOwnPlayerState(
            PlayerState state, String playerName,
            String cardBack, String coin,
            Map<String, Card> cardCache) {
        if (state == null) return null;
        return new OwnPlayerStateResponseDTO(
                state.getPlayerId(),
                playerName,
                cardBack,
                coin,
                toActivePokemonDTO(state.getActivePokemon(), cardCache),
                toBenchPokemonDTOList(state.getBench(), cardCache),
                resolveCards(state.getHand(), cardCache),
                state.getDeckSize(),
                resolveCards(state.getPrizes(), cardCache),
                resolveCards(state.getDiscard(), cardCache)
        );
    }

    private OpponentPlayerStateResponseDTO toOpponentPlayerState(
            PlayerState state, String playerName,
            String cardBack, String coin,
            Map<String, Card> cardCache) {
        if (state == null) return null;
        return new OpponentPlayerStateResponseDTO(
                state.getPlayerId(),
                playerName,
                cardBack,
                coin,
                toActivePokemonDTO(state.getActivePokemon(), cardCache),
                toBenchPokemonDTOList(state.getBench(), cardCache),
                state.getHandSize(),
                state.getDeckSize(),
                state.getPrizeCount(),
                resolveCards(state.getDiscard(), cardCache)
        );
    }

    /** Maps an ActivePokemon domain object to its DTO, resolving card IDs to full card data. */
    private ActivePokemonDTO toActivePokemonDTO(
            ActivePokemon ap, Map<String, Card> cardCache) {
        if (ap == null) return null;

        Card card  = cardCache != null ? cardCache.get(ap.getCardId()) : null;
        int maxHp  = card != null && card.getHp() != null ? card.getHp() : 0;

        return new ActivePokemonDTO(
                ap.getInstanceId(),
                ap.getCardId(),
                card != null ? cardMapper.toResponseDTO(card) : null,
                resolveCards(ap.getAttachedEnergyIds(), cardCache),
                resolveCard(ap.getAttachedToolId(), cardCache),
                resolveCardList(ap.getEvolutionStack(), cardCache),
                ap.getDamageCounters(),
                ap.getCurrentHp(maxHp),
                maxHp,
                ap.getConditions(),
                ap.isEnteredThisTurn(),
                ap.canAttack(),
                ap.canRetreat()
        );
    }

    /** Maps a BenchPokemon domain object to its DTO, resolving card IDs to full card data. */
    private BenchPokemonDTO toBenchPokemonDTO(
            BenchPokemon bp, Map<String, Card> cardCache) {
        if (bp == null) return null;

        Card card = cardCache != null ? cardCache.get(bp.getCardId()) : null;
        int maxHp = card != null && card.getHp() != null ? card.getHp() : 0;

        return new BenchPokemonDTO(
                bp.getInstanceId(),
                bp.getCardId(),
                card != null ? cardMapper.toResponseDTO(card) : null,
                resolveCards(bp.getAttachedEnergyIds(), cardCache),
                resolveCard(bp.getAttachedToolId(), cardCache),
                resolveCardList(bp.getEvolutionStack(), cardCache),
                bp.getDamageCounters(),
                Math.max(0, maxHp - bp.getDamageCounters() * 10),
                maxHp,
                bp.isEnteredThisTurn()
        );
    }

    private List<BenchPokemonDTO> toBenchPokemonDTOList(
            List<BenchPokemon> bench, Map<String, Card> cardCache) {
        if (bench == null) return Collections.emptyList();
        return bench.stream()
                .map(bp -> toBenchPokemonDTO(bp, cardCache))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /** Resolves a single card ID to its CardResponseDTO. */
    private CardResponseDTO resolveCard(String cardId, Map<String, Card> cardCache) {
        if (cardId == null || cardCache == null) return null;
        Card card = cardCache.get(cardId);
        return card != null ? cardMapper.toResponseDTO(card) : null;
    }

    /** Resolves a list of card IDs to their CardResponseDTOs (preserving order). */
    private List<CardResponseDTO> resolveCardList(
            List<String> cardIds, Map<String, Card> cardCache) {
        if (cardIds == null) return Collections.emptyList();
        return cardIds.stream()
                .map(id -> resolveCard(id, cardCache))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<CardResponseDTO> resolveCards(
            List<String> cardIds, Map<String, Card> cardCache) {
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
