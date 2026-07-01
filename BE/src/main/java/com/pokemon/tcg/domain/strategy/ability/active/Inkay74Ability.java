package com.pokemon.tcg.domain.strategy.ability.active;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.ability.ActiveAbilityEffect;
import com.pokemon.tcg.engine.CardLookupPort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Inkay — Upside-Down Evolution (Active Ability).
 *
 * <p>Once during your turn (before your attack), if this Pokémon is Confused,
 * you may search your deck for a card that evolves from this Pokémon and put
 * it onto this Pokémon (this counts as evolving). Shuffle your deck afterward.
 *
 * <p>Payload expected:
 * <ul>
 *   <li>{@code instanceId} — instanceId of Inkay using the ability.</li>
 *   <li>{@code abilityName} — "Upside-Down Evolution".</li>
 *   <li>{@code cardId} — ID of the evolution card in deck to evolve into.</li>
 * </ul>
 */
@Component
public class Inkay74Ability implements ActiveAbilityEffect {

    private static final String IDENTIFIER = "inkay|upside-down evolution";

    private final CardLookupPort cardLookupPort;

    public Inkay74Ability(CardLookupPort cardLookupPort) {
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
            return ValidationResult.fail("You must specify a card to evolve into.");
        }

        PlayerState ps = state.getStateFor(action.getPlayerId());
        ActivePokemon inkay = resolvePokemon(ps, instanceId);

        if (inkay == null) {
            return ValidationResult.fail("Inkay is not in play.");
        }
        if (inkay.getConditions() == null || !inkay.getConditions().contains(SpecialCondition.CONFUSED)) {
            return ValidationResult.fail("Inkay must be Confused to use this ability.");
        }
        if (ps.getDeck() == null || !ps.getDeck().contains(cardId)) {
            return ValidationResult.fail("That card is not in your deck.");
        }

        boolean evolvesFromInkay = cardLookupPort.findCardById(cardId)
                .map(card -> "Inkay".equals(card.getEvolvesFrom()))
                .orElse(false);
        if (!evolvesFromInkay) {
            return ValidationResult.fail("That card does not evolve from Inkay.");
        }

        return ValidationResult.ok();
    }

    @Override
    public BoardState apply(BoardState state, GameAction action) {
        String instanceId = action.getPayloadString("instanceId");
        String cardId      = action.getPayloadString("cardId");
        PlayerState ps     = state.getStateFor(action.getPlayerId());

        ActivePokemon inkay = resolvePokemon(ps, instanceId);
        if (inkay == null) return state;

        // Evolve in place — same effect as EVOLVE_POKEMON but self-triggered
        inkay.setCardId(cardId);
        inkay.setBlockedAttackName(null);
        if (inkay.getActiveEffects() != null) {
            inkay.getActiveEffects().clear();
        }
        inkay.setConditions(new java.util.HashSet<>());

        List<String> stack = new ArrayList<>(
                inkay.getEvolutionStack() != null ? inkay.getEvolutionStack() : new ArrayList<>());
        stack.add(cardId);
        inkay.setEvolutionStack(stack);

        // Remove the evolution card from the deck and shuffle
        List<String> deck = new ArrayList<>(ps.getDeck());
        deck.remove(cardId);
        Collections.shuffle(deck);
        ps.setDeck(deck);

        state.getTurnFlags().markAbilityUsed(instanceId, action.getPayloadString("abilityName"));

        return state;
    }

    private ActivePokemon resolvePokemon(PlayerState ps, String instanceId) {
        if (ps.getActivePokemon() != null
                && ps.getActivePokemon().getInstanceId().equals(instanceId)) {
            return ps.getActivePokemon();
        }
        return null;
    }
}