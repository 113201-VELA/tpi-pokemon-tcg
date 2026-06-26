package com.pokemon.tcg.domain.strategy.item;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.TrainerEffect;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class RedCardEffect implements TrainerEffect {

    private static final int CARDS_TO_DRAW = 4;

    @Override
    public ValidationResult canApply(BoardState state, GameAction action) {
        return ValidationResult.ok();
    }

    @Override
    public EngineResult apply(BoardState state, GameAction action) {
        String opponentId    = state.getOpponentState(action.getPlayerId()).getPlayerId();
        PlayerState opponent = state.getStateFor(opponentId);

        List<String> deck = new ArrayList<>(
                opponent.getDeck() != null ? opponent.getDeck() : new ArrayList<>());
        if (opponent.getHand() != null) {
            deck.addAll(opponent.getHand());
        }
        Collections.shuffle(deck);

        List<String> newHand = new ArrayList<>();
        int cardsToDraw = Math.min(CARDS_TO_DRAW, deck.size());
        for (int i = 0; i < cardsToDraw; i++) {
            newHand.add(deck.remove(0));
        }

        opponent.setDeck(deck);
        opponent.setHand(newHand);

        GameEvent event = GameEvent.builder()
                .type(GameEventType.TRAINER_PLAYED)
                .gameId(state.getGameId())
                .playerId(action.getPlayerId())
                .turnNumber(state.getTurnNumber())
                .data(Map.of(
                        "cardId",     action.getPayloadString("cardId"),
                        "cardsDrawn", cardsToDraw))
                .occurredAt(Instant.now())
                .build();

        return EngineResult.of(state, List.of(event));
    }
}
