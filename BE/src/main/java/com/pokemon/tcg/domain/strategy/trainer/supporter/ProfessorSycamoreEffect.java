package com.pokemon.tcg.domain.strategy.trainer.supporter;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.trainer.TrainerEffect;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Strategy implementation for Professor Sycamore (xy1-122).
 * Effect: Discard your hand and draw 7 cards from your deck.
 *
 * <p>This is a Supporter card — the once-per-turn Supporter limit
 * is enforced by RuleValidator, not here.
 */
@Component
public class ProfessorSycamoreEffect implements TrainerEffect {

    @Override
    public String getCardIdentifier() {
        return "professor sycamore";
    }

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

        // Discard entire hand
        List<String> discard = new ArrayList<>(
                ps.getDiscard() != null ? ps.getDiscard() : new ArrayList<>());
        if (ps.getHand() != null) {
            discard.addAll(ps.getHand());
        }
        ps.setDiscard(discard);

        // Draw up to 7 cards from deck
        List<String> deck = new ArrayList<>(ps.getDeck());
        List<String> newHand = new ArrayList<>();
        int cardsToDraw = Math.min(7, deck.size());
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
