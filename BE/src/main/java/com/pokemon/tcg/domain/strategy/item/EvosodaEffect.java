package com.pokemon.tcg.domain.strategy.item;

import com.pokemon.tcg.domain.model.card.Card;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.TrainerEffect;
import com.pokemon.tcg.engine.CardLookupPort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

@Component
public class EvosodaEffect implements TrainerEffect {

    private final CardLookupPort cardLookupPort;

    public EvosodaEffect(CardLookupPort cardLookupPort) {
        this.cardLookupPort = cardLookupPort;
    }

    @Override
    public ValidationResult canApply(BoardState state, GameAction action) {
        PlayerState ps = state.getStateFor(action.getPlayerId());
        String targetId = action.getPayloadString("targetPokemonInstanceId");
        String evolutionId = action.getPayloadString("evolutionCardId");

        if (targetId == null || evolutionId == null) return ValidationResult.fail("Missing required IDs.");

        // Use the new method in PlayerState
        Optional<String> targetCardId = ps.findCardIdByInstanceId(targetId);
        if (targetCardId.isEmpty()) return ValidationResult.fail("Target Pokémon not found.");

        List<String> deck = ps.getDeck() != null ? ps.getDeck() : List.of();
        if (!deck.contains(evolutionId)) return ValidationResult.fail("Evolution not in deck.");

        Card target = cardLookupPort.findCardById(targetCardId.get()).orElseThrow();
        Card evolution = cardLookupPort.findCardById(evolutionId).orElseThrow();

        if (evolution.getEvolvesFrom() == null ||
            !evolution.getEvolvesFrom().equalsIgnoreCase(target.getName())) {
            return ValidationResult.fail("Invalid evolution path.");
        }

        return ValidationResult.ok();
    }

    @Override
    public EngineResult apply(BoardState state, GameAction action) {
        PlayerState ps = state.getStateFor(action.getPlayerId());
        List<String> deck = new ArrayList<>(ps.getDeck());

        deck.remove(action.getPayloadString("evolutionCardId"));
        Collections.shuffle(deck);
        ps.setDeck(deck);

        // Emit event
        GameEvent event = GameEvent.builder()
                .type(GameEventType.TRAINER_PLAYED)
                .gameId(state.getGameId())
                .playerId(action.getPlayerId())
                .data(Map.of("cardId", action.getPayloadString("cardId"),
                             "targetInstanceId", action.getPayloadString("targetPokemonInstanceId"),
                             "evolutionId", action.getPayloadString("evolutionCardId")))
                .occurredAt(Instant.now())
                .build();

        return EngineResult.of(state, List.of(event));
    }
}
