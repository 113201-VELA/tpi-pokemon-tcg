package com.pokemon.tcg.domain.strategy.trainer.item;

import com.pokemon.tcg.domain.model.card.CardType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.trainer.TrainerEffect;
import com.pokemon.tcg.engine.CardLookupPort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

@Component
public class GreatBallEffect implements TrainerEffect {

    private static final int TOP_N_CARDS = 7;

    private final CardLookupPort cardLookupPort;

    public GreatBallEffect(CardLookupPort cardLookupPort) {
        this.cardLookupPort = cardLookupPort;
    }

    @Override
    public String getCardIdentifier() {
        return "great ball";
    }

    @Override
    public ValidationResult canApply(BoardState state, GameAction action) {
        PlayerState ps = state.getStateFor(action.getPlayerId());
        List<String> deck = ps.getDeck() != null ? ps.getDeck() : List.of();

        if (deck.isEmpty()) {
            return ValidationResult.fail("Your deck is empty.");
        }

        int peekCount = Math.min(TOP_N_CARDS, deck.size());
        boolean hasPokemon = deck.subList(0, peekCount).stream()
                .anyMatch(id -> cardLookupPort.findCardById(id)
                        .map(card -> card.getSupertype() == CardType.POKEMON)
                        .orElse(false));

        if (!hasPokemon) {
            return ValidationResult.fail("No Pokémon found in the top " + TOP_N_CARDS + " cards of your deck.");
        }

        return ValidationResult.ok();
    }

    @Override
    public EngineResult apply(BoardState state, GameAction action) {
        PlayerState ps = state.getStateFor(action.getPlayerId());
        List<String> deck = new ArrayList<>(ps.getDeck() != null ? ps.getDeck() : new ArrayList<>());
        int peekCount = Math.min(TOP_N_CARDS, deck.size());
        List<String> topCards = new ArrayList<>(deck.subList(0, peekCount));

        // Remove the top cards from the deck
        deck.subList(0, peekCount).clear();
        ps.setDeck(deck);

        // Set pending deck selection state
        state.setPendingDeckSelectionPlayerId(action.getPlayerId());
        state.setPendingDeckSelectionCardIds(topCards);

        GameEvent event = GameEvent.builder()
                .type(GameEventType.TRAINER_PLAYED)
                .gameId(state.getGameId())
                .playerId(action.getPlayerId())
                .turnNumber(state.getTurnNumber())
                .data(Map.of(
                        "cardId", action.getPayloadString("cardId"),
                        "topCards", topCards))
                .occurredAt(Instant.now())
                .build();

        return EngineResult.of(state, List.of(event));
    }
}
