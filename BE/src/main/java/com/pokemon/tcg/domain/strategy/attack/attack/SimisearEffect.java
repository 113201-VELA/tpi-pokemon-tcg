package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.engine.CardLookupPort;
import com.pokemon.tcg.engine.StatusEffectManager;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * xy1-23 Simisear
 *
 * Yawn: opponent's Active Pokémon is now Asleep.
 * Flamethrower: 90 damage. Discard a Fire Energy attached to this Pokémon
 *               (mandatory cost — always discards, defaulting to the first
 *               attached Fire Energy if none is explicitly specified).
 */
@Component
public class SimisearEffect implements AttackEffect {

    private static final String YAWN         = "yawn";
    private static final String FLAMETHROWER = "flamethrower";
    private static final String FIRE_TYPE    = EnergyType.FIRE.name();

    private final CardLookupPort cardLookupPort;
    private final StatusEffectManager statusEffectManager;

    public SimisearEffect(CardLookupPort cardLookupPort, StatusEffectManager statusEffectManager) {
        this.cardLookupPort = cardLookupPort;
        this.statusEffectManager = statusEffectManager;
    }

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("simisear|yawn", "simisear|flamethrower");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        switch (attackName) {
            case YAWN         -> applyYawn(ctx);
            case FLAMETHROWER -> applyFlamethrower(ctx);
            default           -> { }
        }
    }

    private void applyYawn(AttackContext ctx) {
        String attackerId      = ctx.getAction().getPlayerId();
        PlayerState opponent   = ctx.getBoardState().getOpponentState(attackerId);
        ActivePokemon defender = opponent.getActivePokemon();

        if (defender == null) return;

        statusEffectManager.applyCondition(defender, SpecialCondition.ASLEEP);
    }

    /**
     * Mandatory cost: discard a Fire Energy attached to Simisear. If the
     * attacker specifies which Energy via {@code energyToDiscardId} and it
     * is actually a Fire Energy attached to Simisear, that one is
     * discarded; otherwise falls back to the first attached Fire Energy
     * found, since this discard is not optional per the card text.
     */
    private void applyFlamethrower(AttackContext ctx) {
        String attackerId         = ctx.getAction().getPlayerId();
        PlayerState attackerState = ctx.getBoardState().getStateFor(attackerId);
        ActivePokemon simisear    = attackerState.getActivePokemon();

        if (simisear == null
                || simisear.getAttachedEnergyIds() == null
                || simisear.getAttachedEnergyIds().isEmpty()) return;

        List<String> energies = new ArrayList<>(simisear.getAttachedEnergyIds());
        String requestedEnergyId = ctx.getAction().getPayloadString("energyToDiscardId");

        String chosen;
        if (requestedEnergyId != null
                && energies.contains(requestedEnergyId)
                && isFireEnergy(requestedEnergyId)) {
            chosen = requestedEnergyId;
        } else {
            chosen = energies.stream()
                    .filter(this::isFireEnergy)
                    .findFirst()
                    .orElse(null);
        }

        if (chosen == null) return; // no Fire Energy attached — shouldn't normally happen

        energies.remove(chosen);
        simisear.setAttachedEnergyIds(energies);

        List<String> discard = new ArrayList<>(
                attackerState.getDiscard() != null
                        ? attackerState.getDiscard() : new ArrayList<>());
        discard.add(chosen);
        attackerState.setDiscard(discard);
    }

    private boolean isFireEnergy(String cardId) {
        return cardLookupPort.findCardById(cardId)
                .map(card -> card.getTypes() != null
                        && card.getTypes().contains(FIRE_TYPE))
                .orElse(false);
    }
}