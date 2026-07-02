package com.pokemon.tcg.domain.strategy.ability.active;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.ability.ActiveAbilityEffect;
import com.pokemon.tcg.engine.CardLookupPort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Aegislash (xy1-85 / xy1-86) — Stance Change (Active Ability).
 *
 * <p>Once during your turn (before your attack), you may switch this Pokémon
 * with an Aegislash in your hand. Any cards attached to this Pokémon, damage
 * counters, Special Conditions, turns in play, and any other effects
 * (including blockedAttackName and pending self-buffs) remain on the new
 * Pokémon. Works from either the Active spot or the Bench.
 *
 * <p>Payload expected:
 * <ul>
 *   <li>{@code instanceId} — instanceId of the Aegislash using the ability.</li>
 *   <li>{@code abilityName} — "Stance Change".</li>
 *   <li>{@code cardId} — ID of the other Aegislash card in hand to switch into
 *       (e.g. "xy1-85" or "xy1-86").</li>
 * </ul>
 */
@Component
public class AegislashAbility implements ActiveAbilityEffect {

    private static final String IDENTIFIER     = "aegislash|stance change";
    private static final String AEGISLASH_NAME = "aegislash";

    private final CardLookupPort cardLookupPort;

    public AegislashAbility(CardLookupPort cardLookupPort) {
        this.cardLookupPort = cardLookupPort;
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public ValidationResult canApply(BoardState state, GameAction action) {
        String instanceId = action.getPayloadString("instanceId");
        String cardId      = action.getPayloadString("cardId");

        if (instanceId == null || cardId == null) {
            return ValidationResult.fail("You must specify the Aegislash card to switch into.");
        }

        PlayerState ps = state.getStateFor(action.getPlayerId());

        String currentCardId = resolveCurrentCardId(ps, instanceId);
        if (currentCardId == null) {
            return ValidationResult.fail("Aegislash is not in play.");
        }

        if (ps.getHand() == null || !ps.getHand().contains(cardId)) {
            return ValidationResult.fail("That card is not in your hand.");
        }

        boolean isAegislash = cardLookupPort.findCardById(cardId)
                .map(card -> AEGISLASH_NAME.equalsIgnoreCase(card.getName()))
                .orElse(false);
        if (!isAegislash) {
            return ValidationResult.fail("You can only switch into an Aegislash card.");
        }

        return ValidationResult.ok();
    }

    @Override
    public BoardState apply(BoardState state, GameAction action) {
        String instanceId = action.getPayloadString("instanceId");
        String cardId      = action.getPayloadString("cardId");
        PlayerState ps     = state.getStateFor(action.getPlayerId());

        ActivePokemon activeMatch = findActive(ps, instanceId);
        if (activeMatch != null) {
            switchCardId(activeMatch, cardId);
        } else {
            BenchPokemon benchMatch = findBench(ps, instanceId);
            if (benchMatch != null) {
                switchCardId(benchMatch, cardId);
            }
        }

        // Remove the new Aegislash form from hand — the old form is not
        // returned to hand per the card text (this is a form change, not
        // a card exchange), consistent with how EVOLVE_POKEMON only
        // removes the evolution card from hand.
        List<String> hand = new ArrayList<>(ps.getHand());
        hand.remove(cardId);
        ps.setHand(hand);

        state.getTurnFlags().markAbilityUsed(instanceId, action.getPayloadString("abilityName"));

        return state;
    }

    private void switchCardId(ActivePokemon pokemon, String newCardId) {
        pokemon.setCardId(newCardId);
        List<String> stack = new ArrayList<>(
                pokemon.getEvolutionStack() != null ? pokemon.getEvolutionStack() : new ArrayList<>());
        if (!stack.isEmpty()) stack.remove(stack.size() - 1);
        stack.add(newCardId);
        pokemon.setEvolutionStack(stack);
        // damageCounters, conditions, activeEffects, blockedAttackName,
        // attachedEnergyIds, attachedToolId, enteredThisTurn,
        // pendingAttackDamageBoost* are intentionally left untouched.
    }

    private void switchCardId(BenchPokemon pokemon, String newCardId) {
        pokemon.setCardId(newCardId);
        List<String> stack = new ArrayList<>(
                pokemon.getEvolutionStack() != null ? pokemon.getEvolutionStack() : new ArrayList<>());
        if (!stack.isEmpty()) stack.remove(stack.size() - 1);
        stack.add(newCardId);
        pokemon.setEvolutionStack(stack);
    }

    /** Resolves the current cardId of the Aegislash matching instanceId, Active or Bench. */
    private String resolveCurrentCardId(PlayerState ps, String instanceId) {
        ActivePokemon active = findActive(ps, instanceId);
        if (active != null) return active.getCardId();

        BenchPokemon bench = findBench(ps, instanceId);
        if (bench != null) return bench.getCardId();

        return null;
    }

    private ActivePokemon findActive(PlayerState ps, String instanceId) {
        if (ps.getActivePokemon() != null
                && ps.getActivePokemon().getInstanceId().equals(instanceId)) {
            return ps.getActivePokemon();
        }
        return null;
    }

    private BenchPokemon findBench(PlayerState ps, String instanceId) {
        if (ps.getBench() == null) return null;
        return ps.getBench().stream()
                .filter(b -> b.getInstanceId().equals(instanceId))
                .findFirst()
                .orElse(null);
    }
}