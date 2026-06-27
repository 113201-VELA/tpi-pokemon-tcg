package com.pokemon.tcg.domain.strategy.trainer.supporter;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.trainer.TrainerEffect;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Strategy implementation for Shauna (xy1-127).
 * Effect: Shuffle your hand into your deck, then draw 5 cards.
 *
 * <p>This is a Supporter card — the once-per-turn Supporter limit
 * is enforced by RuleValidator, not here.
 */
@Component
public class ShaunaEffect implements TrainerEffect {

    private static final int CARDS_TO_DRAW = 5;

    @Override
    public ValidationResult canApply(BoardState state, GameAction action) {
        PlayerState ps = state.getStateFor(action.getPlayerId());
        if (ps.getDeck() == null || ps.getDeck().isEmpty()) {
            return ValidationResult.fail("Your deck is empty.");
        }
        return ValidationResult.ok();
    }

    @Override
    public EngineResult apply(BoardState state, GameAction action) {
        PlayerState ps = state.getStateFor(action.getPlayerId());

        // Shuffle hand into deck
        List<String> deck = new ArrayList<>(ps.getDeck() != null ? ps.getDeck() : new ArrayList<>());
        if (ps.getHand() != null) {
            deck.addAll(ps.getHand());
        }
        java.util.Collections.shuffle(deck);

        // Draw up to 5 cards; draw fewer if deck has less than 5
        List<String> newHand = new ArrayList<>();
        int cardsToDraw = Math.min(CARDS_TO_DRAW, deck.size());
        for (int i = 0; i < cardsToDraw; i++) {
            newHand.add(deck.remove(0));
        }
        ps.setHand(newHand);
        ps.setDeck(deck);

        // Mark Supporter as played this turn
        state.getTurnFlags().setSupporterPlayedThisTurn(true);

        GameEvent event = GameEvent.builder()
                .type(GameEventType.TRAINER_PLAYED)
                .gameId(state.getGameId())
                .playerId(action.getPlayerId())
                .turnNumber(state.getTurnNumber())
                .data(Map.of(
                        "cardId", action.getPayloadString("cardId"),
                        "cardsDrawn", cardsToDraw))
                .occurredAt(Instant.now())
                .build();

        return EngineResult.of(state, List.of(event));
    }
}
