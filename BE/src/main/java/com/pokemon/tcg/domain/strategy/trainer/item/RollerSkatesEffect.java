package com.pokemon.tcg.domain.strategy.trainer.item;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.trainer.TrainerEffect;
import com.pokemon.tcg.engine.CoinFlipService;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class RollerSkatesEffect implements TrainerEffect {

    private static final int CARDS_TO_DRAW = 3;

    private final CoinFlipService coinFlipService;

    public RollerSkatesEffect(CoinFlipService coinFlipService) {
        this.coinFlipService = coinFlipService;
    }

    @Override
    public ValidationResult canApply(BoardState state, GameAction action) {
        return ValidationResult.ok();
    }

    @Override
    public EngineResult apply(BoardState state, GameAction action) {
        PlayerState ps     = state.getStateFor(action.getPlayerId());
        CoinResult  flip   = coinFlipService.flip();
        boolean     heads  = flip == CoinResult.HEADS;
        int         drawn  = 0;

        if (heads) {
            List<String> deck = new ArrayList<>(ps.getDeck() != null ? ps.getDeck() : new ArrayList<>());
            List<String> hand = new ArrayList<>(ps.getHand() != null ? ps.getHand() : new ArrayList<>());
            drawn = Math.min(CARDS_TO_DRAW, deck.size());
            for (int i = 0; i < drawn; i++) {
                hand.add(deck.remove(0));
            }
            ps.setDeck(deck);
            ps.setHand(hand);
        }

        GameEvent event = GameEvent.builder()
                .type(GameEventType.TRAINER_PLAYED)
                .gameId(state.getGameId())
                .playerId(action.getPlayerId())
                .turnNumber(state.getTurnNumber())
                .data(Map.of(
                        "cardId",     action.getPayloadString("cardId"),
                        "coinResult", flip.name(),
                        "cardsDrawn", drawn))
                .occurredAt(Instant.now())
                .build();

        return EngineResult.of(state, List.of(event));
    }
}
