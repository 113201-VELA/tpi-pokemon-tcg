package com.pokemon.tcg.domain.strategy.trainer.supporter;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.trainer.TrainerEffect;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Strategy implementation for Cassius (xy1-115).
 * Effect: Shuffle 1 of your Pokémon and all cards attached to it into your deck.
 *
 * <p>The player chooses the target Pokémon via {@code targetInstanceId} in the payload.
 * The target can be the Active Pokémon or any Bench Pokémon.
 * All attached Energy and Tool cards are returned to the deck along with the Pokémon.
 * The deck is shuffled after the cards are added.
 *
 * <p>This is a Supporter card — the once-per-turn Supporter limit
 * is enforced by RuleValidator, not here.
 */
@Component
public class CassiusEffect implements TrainerEffect {

    @Override
    public ValidationResult canApply(BoardState state, GameAction action) {
        String targetId = action.getPayloadString("targetInstanceId");
        PlayerState ps  = state.getStateFor(action.getPlayerId());

        if (targetId == null) {
            return ValidationResult.fail("No target Pokémon specified for Cassius.");
        }
        if (!isInPlay(ps, targetId)) {
            return ValidationResult.fail("Target Pokémon is not in play.");
        }
        return ValidationResult.ok();
    }

    @Override
    public EngineResult apply(BoardState state, GameAction action) {
        String targetId = action.getPayloadString("targetInstanceId");
        PlayerState ps  = state.getStateFor(action.getPlayerId());

        List<String> deck = new ArrayList<>(ps.getDeck() != null ? ps.getDeck() : new ArrayList<>());

        // Collect the Pokémon card and all attached cards, then remove it from play
        if (isActive(ps, targetId)) {
            returnActiveToDeck(ps, deck);
        } else {
            returnBenchPokemonToDeck(ps, targetId, deck);
        }

        // Shuffle the deck after adding the returned cards
        Collections.shuffle(deck);
        ps.setDeck(deck);

        // Mark Supporter as played this turn
        state.getTurnFlags().setSupporterPlayedThisTurn(true);

        GameEvent event = GameEvent.builder()
                .type(GameEventType.TRAINER_PLAYED)
                .gameId(state.getGameId())
                .playerId(action.getPlayerId())
                .turnNumber(state.getTurnNumber())
                .data(Map.of(
                        "cardId",             action.getPayloadString("cardId"),
                        "returnedInstanceId", targetId))
                .occurredAt(Instant.now())
                .build();

        return EngineResult.of(state, List.of(event));
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    /** Returns true if any Pokémon in play (Active or Bench) matches the given instanceId. */
    private boolean isInPlay(PlayerState ps, String instanceId) {
        return isActive(ps, instanceId) || isOnBench(ps, instanceId);
    }

    /** Returns true if the Active Pokémon matches the given instanceId. */
    private boolean isActive(PlayerState ps, String instanceId) {
        return ps.getActivePokemon() != null
                && ps.getActivePokemon().getInstanceId().equals(instanceId);
    }

    /** Returns true if any Bench Pokémon matches the given instanceId. */
    private boolean isOnBench(PlayerState ps, String instanceId) {
        return ps.getBench() != null
                && ps.getBench().stream().anyMatch(b -> b.getInstanceId().equals(instanceId));
    }

    /**
     * Adds the Active Pokémon's card and all attached cards to the deck,
     * then clears the Active slot.
     */
    private void returnActiveToDeck(PlayerState ps, List<String> deck) {
        ActivePokemon active = ps.getActivePokemon();
        deck.add(active.getCardId());
        if (active.getAttachedEnergyIds() != null) {
            deck.addAll(active.getAttachedEnergyIds());
        }
        if (active.getAttachedToolId() != null) {
            deck.add(active.getAttachedToolId());
        }
        ps.setActivePokemon(null);
    }

    /**
     * Adds the target Bench Pokémon's card and all attached cards to the deck,
     * then removes it from the bench.
     */
    private void returnBenchPokemonToDeck(PlayerState ps, String instanceId, List<String> deck) {
        List<BenchPokemon> bench = new ArrayList<>(ps.getBench());
        BenchPokemon target = bench.stream()
                .filter(b -> b.getInstanceId().equals(instanceId))
                .findFirst()
                .orElse(null);

        if (target == null) return;

        deck.add(target.getCardId());
        if (target.getAttachedEnergyIds() != null) {
            deck.addAll(target.getAttachedEnergyIds());
        }
        if (target.getAttachedToolId() != null) {
            deck.add(target.getAttachedToolId());
        }

        bench.remove(target);
        ps.setBench(bench);
    }
}
