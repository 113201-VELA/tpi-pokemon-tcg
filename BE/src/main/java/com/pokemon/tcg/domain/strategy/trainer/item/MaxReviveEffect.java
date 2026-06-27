package com.pokemon.tcg.domain.strategy.trainer.item;

import com.pokemon.tcg.domain.model.card.CardType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.trainer.TrainerEffect;
import com.pokemon.tcg.engine.CardLookupPort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class MaxReviveEffect implements TrainerEffect {

    private final CardLookupPort cardLookupPort;

    public MaxReviveEffect(CardLookupPort cardLookupPort) {
        this.cardLookupPort = cardLookupPort;
    }

    @Override
    public ValidationResult canApply(BoardState state, GameAction action) {
        String chosenCardId = action.getPayloadString("chosenCardId");
        PlayerState ps      = state.getStateFor(action.getPlayerId());

        if (chosenCardId == null) {
            return ValidationResult.fail("No Pokémon specified for Max Revive.");
        }

        List<String> discard = ps.getDiscard() != null ? ps.getDiscard() : List.of();

        if (discard.isEmpty()) {
            return ValidationResult.fail("Your discard pile is empty.");
        }
        if (!discard.contains(chosenCardId)) {
            return ValidationResult.fail("Card " + chosenCardId + " is not in your discard pile.");
        }
        if (!isPokemon(chosenCardId)) {
            return ValidationResult.fail("Card " + chosenCardId + " is not a Pokémon.");
        }

        return ValidationResult.ok();
    }

    @Override
    public EngineResult apply(BoardState state, GameAction action) {
        String chosenCardId = action.getPayloadString("chosenCardId");
        PlayerState ps      = state.getStateFor(action.getPlayerId());

        List<String> discard = new ArrayList<>(ps.getDiscard());
        discard.remove(chosenCardId);
        ps.setDiscard(discard);

        List<String> deck = new ArrayList<>(ps.getDeck() != null ? ps.getDeck() : new ArrayList<>());
        deck.add(0, chosenCardId);
        ps.setDeck(deck);

        GameEvent event = GameEvent.builder()
                .type(GameEventType.TRAINER_PLAYED)
                .gameId(state.getGameId())
                .playerId(action.getPlayerId())
                .turnNumber(state.getTurnNumber())
                .data(Map.of(
                        "cardId",        action.getPayloadString("cardId"),
                        "revivedCardId", chosenCardId))
                .occurredAt(Instant.now())
                .build();

        return EngineResult.of(state, List.of(event));
    }

    private boolean isPokemon(String cardId) {
        return cardLookupPort.findCardById(cardId)
                .map(card -> card.getSupertype() == CardType.POKEMON)
                .orElse(false);
    }
}
