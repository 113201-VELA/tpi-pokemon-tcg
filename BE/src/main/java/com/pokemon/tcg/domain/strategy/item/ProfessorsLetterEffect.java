package com.pokemon.tcg.domain.strategy.item;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.TrainerEffect;
import com.pokemon.tcg.engine.CardLookupPort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Strategy implementation for Professor's Letter (xy1-123).
 * Effect: Search your deck for up to 2 Basic Energy cards, reveal them,
 * and put them in your hand. Shuffle your deck afterward.
 *
 * <p>The player specifies the chosen card IDs via {@code energyCardIds} in the
 * payload (a list of up to 2 card ID strings). The effect validates that each
 * chosen card exists in the player's deck and is a Basic Energy card.
 *
 * <p>Revealing the chosen cards to the opponent is handled by including their
 * IDs in the emitted {@code TRAINER_PLAYED} event, which the frontend displays
 * to both players.
 */
@Component
public class ProfessorsLetterEffect implements TrainerEffect {

    private static final int MAX_ENERGY_CARDS = 2;

    private final CardLookupPort cardLookupPort;

    public ProfessorsLetterEffect(CardLookupPort cardLookupPort) {
        this.cardLookupPort = cardLookupPort;
    }

    @Override
    public ValidationResult canApply(BoardState state, GameAction action) {
        PlayerState ps           = state.getStateFor(action.getPlayerId());
        List<String> chosenIds   = getChosenIds(action);

        if (chosenIds.isEmpty()) {
            return ValidationResult.fail("No Basic Energy cards specified for Professor's Letter.");
        }
        if (chosenIds.size() > MAX_ENERGY_CARDS) {
            return ValidationResult.fail("You may search for at most 2 Basic Energy cards.");
        }

        List<String> deck = ps.getDeck() != null ? ps.getDeck() : List.of();

        for (String cardId : chosenIds) {
            if (!deck.contains(cardId)) {
                return ValidationResult.fail("Card " + cardId + " is not in your deck.");
            }
            if (!isBasicEnergy(cardId)) {
                return ValidationResult.fail("Card " + cardId + " is not a Basic Energy.");
            }
        }

        return ValidationResult.ok();
    }

    @Override
    public EngineResult apply(BoardState state, GameAction action) {
        PlayerState ps         = state.getStateFor(action.getPlayerId());
        List<String> chosenIds = getChosenIds(action);

        List<String> deck = new ArrayList<>(ps.getDeck() != null ? ps.getDeck() : new ArrayList<>());
        List<String> hand = new ArrayList<>(ps.getHand() != null ? ps.getHand() : new ArrayList<>());

        // Move chosen Basic Energy cards from deck to hand
        for (String cardId : chosenIds) {
            deck.remove(cardId);
            hand.add(cardId);
        }

        // Shuffle deck after search
        Collections.shuffle(deck);

        ps.setDeck(deck);
        ps.setHand(hand);

        GameEvent event = GameEvent.builder()
                .type(GameEventType.TRAINER_PLAYED)
                .gameId(state.getGameId())
                .playerId(action.getPlayerId())
                .turnNumber(state.getTurnNumber())
                .data(Map.of(
                        "cardId",        action.getPayloadString("cardId"),
                        "revealedCards", chosenIds))
                .occurredAt(Instant.now())
                .build();

        return EngineResult.of(state, List.of(event));
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    /**
     * Extracts the list of chosen energy card IDs from the action payload.
     * Returns an empty list if the payload key is absent or null.
     */
    @SuppressWarnings("unchecked")
    private List<String> getChosenIds(GameAction action) {
        Object raw = action.getPayload() != null
                ? action.getPayload().get("energyCardIds")
                : null;
        if (raw instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }

    /**
     * Returns true if the card identified by the given ID is a Basic Energy card,
     * according to the card cache.
     */
    private boolean isBasicEnergy(String cardId) {
        return cardLookupPort.findCardById(cardId)
                .map(card -> Boolean.TRUE.equals(card.isBasicEnergy()))
                .orElse(false);
    }
}
