package com.pokemon.tcg.domain.strategy.ability.active;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.ability.ActiveAbilityEffect;
import com.pokemon.tcg.engine.CardLookupPort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Greninja — Water Shuriken (Active Ability).
 *
 * <p>Once during your turn (before your attack), you may discard a Water
 * Energy card from your hand. If you do, put 3 damage counters on 1 of
 * your opponent's Pokémon.
 *
 * <p>Payload expected:
 * <ul>
 *   <li>{@code instanceId} — instanceId of Greninja using the ability.</li>
 *   <li>{@code abilityName} — "Water Shuriken".</li>
 *   <li>{@code energyCardId} — ID of the Water Energy card in hand to discard.</li>
 *   <li>{@code targetInstanceId} — instanceId of the opponent's Pokémon to damage
 *       (Active or Bench).</li>
 * </ul>
 */
@Component
public class GreninjaAbility implements ActiveAbilityEffect {

    private static final String IDENTIFIER       = "greninja|water shuriken";
    private static final String WATER_TYPE       = EnergyType.WATER.name();
    private static final int    DAMAGE_COUNTERS  = 3;

    private final CardLookupPort cardLookupPort;

    public GreninjaAbility(CardLookupPort cardLookupPort) {
        this.cardLookupPort = cardLookupPort;
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public ValidationResult canApply(BoardState state, GameAction action) {
        String energyCardId = action.getPayloadString("energyCardId");
        if (energyCardId == null) {
            return ValidationResult.fail("You must specify a Water Energy card to discard.");
        }

        PlayerState ps = state.getStateFor(action.getPlayerId());
        if (ps.getHand() == null || !ps.getHand().contains(energyCardId)) {
            return ValidationResult.fail("The specified Energy card is not in your hand.");
        }

        boolean isWater = cardLookupPort.findCardById(energyCardId)
                .map(card -> card.getTypes() != null
                        && card.getTypes().contains(WATER_TYPE))
                .orElse(false);
        if (!isWater) {
            return ValidationResult.fail("You must discard a Water Energy card.");
        }

        String targetInstanceId = action.getPayloadString("targetInstanceId");
        if (targetInstanceId == null) {
            return ValidationResult.fail("You must specify a target Pokémon.");
        }

        PlayerState opponent = state.getOpponentState(action.getPlayerId());
        boolean targetExists = isTargetInPlay(opponent, targetInstanceId);
        if (!targetExists) {
            return ValidationResult.fail("The target Pokémon is not in play.");
        }

        return ValidationResult.ok();
    }

    @Override
    public BoardState apply(BoardState state, GameAction action) {
        String energyCardId     = action.getPayloadString("energyCardId");
        String targetInstanceId = action.getPayloadString("targetInstanceId");
        String playerId         = action.getPlayerId();

        PlayerState attacker = state.getStateFor(playerId);
        PlayerState opponent  = state.getOpponentState(playerId);

        // Discard the Water Energy from hand
        List<String> hand = new ArrayList<>(attacker.getHand());
        hand.remove(energyCardId);
        attacker.setHand(hand);

        List<String> discard = new ArrayList<>(
                attacker.getDiscard() != null ? attacker.getDiscard() : new ArrayList<>());
        discard.add(energyCardId);
        attacker.setDiscard(discard);

        // Place 3 damage counters on the target
        applyDamage(opponent, targetInstanceId);

        // Mark ability as used this turn
        String instanceId  = action.getPayloadString("instanceId");
        String abilityName = action.getPayloadString("abilityName");
        state.getTurnFlags().markAbilityUsed(instanceId, abilityName);

        return state;
    }

    private void applyDamage(PlayerState opponent, String targetInstanceId) {
        // Check Active Pokémon
        if (opponent.getActivePokemon() != null
                && opponent.getActivePokemon().getInstanceId().equals(targetInstanceId)) {
            opponent.getActivePokemon().setDamageCounters(
                    opponent.getActivePokemon().getDamageCounters() + DAMAGE_COUNTERS);
            return;
        }
        // Check Bench
        if (opponent.getBench() != null) {
            opponent.getBench().stream()
                    .filter(b -> b.getInstanceId().equals(targetInstanceId))
                    .findFirst()
                    .ifPresent(b -> b.setDamageCounters(b.getDamageCounters() + DAMAGE_COUNTERS));
        }
    }

    private boolean isTargetInPlay(PlayerState opponent, String targetInstanceId) {
        if (opponent.getActivePokemon() != null
                && opponent.getActivePokemon().getInstanceId().equals(targetInstanceId)) {
            return true;
        }
        return opponent.getBench() != null && opponent.getBench().stream()
                .anyMatch(b -> b.getInstanceId().equals(targetInstanceId));
    }
}