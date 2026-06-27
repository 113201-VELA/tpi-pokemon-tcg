package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import com.pokemon.tcg.domain.strategy.attack.DamageModifier;
import com.pokemon.tcg.engine.CardLookupPort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class StarmieEffect implements AttackEffect {

    private static final String RECOVER     = "recover";
    private static final String CORE_SPLASH = "core splash";

    private static final String PSYCHIC_TYPE       = EnergyType.PSYCHIC.name();
    private static final int    PSYCHIC_BONUS       = 30;

    private final CardLookupPort cardLookupPort;

    public StarmieEffect(CardLookupPort cardLookupPort) {
        this.cardLookupPort = cardLookupPort;
    }

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("starmie|recover", "starmie|core splash");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        switch (attackName) {
            case RECOVER     -> applyRecover(ctx);
            case CORE_SPLASH -> applyCoreSplash(ctx);
            default          -> { }
        }
    }

    /**
     * Recover: discard one Energy attached to Starmie and heal all damage from it
     * (reset damageCounters to 0).
     *
     * <p>The player specifies which energy to discard via {@code energyToDiscardId}
     * in the action payload. If no energy is specified or the specified energy is
     * not attached, the attack does nothing.
     */
    private void applyRecover(AttackContext ctx) {
        String energyToDiscardId = ctx.getAction().getPayloadString("energyToDiscardId");
        if (energyToDiscardId == null) return;

        String attackerId    = ctx.getAction().getPlayerId();
        PlayerState attacker = ctx.getBoardState().getStateFor(attackerId);
        ActivePokemon starmie = attacker.getActivePokemon();

        if (starmie == null) return;
        if (starmie.getAttachedEnergyIds() == null
                || !starmie.getAttachedEnergyIds().contains(energyToDiscardId)) return;

        // Discard the specified energy
        List<String> energies = new ArrayList<>(starmie.getAttachedEnergyIds());
        energies.remove(energyToDiscardId);
        starmie.setAttachedEnergyIds(energies);

        List<String> discard = new ArrayList<>(
                attacker.getDiscard() != null ? attacker.getDiscard() : new ArrayList<>());
        discard.add(energyToDiscardId);
        attacker.setDiscard(discard);

        // Heal all damage
        starmie.setDamageCounters(0);
    }

    /**
     * Core Splash: if Starmie has any Psychic Energy attached, this attack does
     * 30 more damage.
     */
    private void applyCoreSplash(AttackContext ctx) {
        String attackerId    = ctx.getAction().getPlayerId();
        PlayerState attacker = ctx.getBoardState().getStateFor(attackerId);
        ActivePokemon starmie = attacker.getActivePokemon();

        if (starmie == null) return;
        if (starmie.getAttachedEnergyIds() == null
                || starmie.getAttachedEnergyIds().isEmpty()) return;

        boolean hasPsychicEnergy = starmie.getAttachedEnergyIds().stream()
                .anyMatch(this::isPsychicEnergy);

        if (hasPsychicEnergy) {
            List<DamageModifier> modifiers = new ArrayList<>(
                    ctx.getModifiers() != null ? ctx.getModifiers() : new ArrayList<>());
            modifiers.add(new DamageModifier("core-splash-psychic", PSYCHIC_BONUS, true));
            ctx.setModifiers(modifiers);
        }
    }

    private boolean isPsychicEnergy(String cardId) {
        return cardLookupPort.findCardById(cardId)
                .map(card -> card.getTypes() != null
                        && card.getTypes().contains(PSYCHIC_TYPE))
                .orElse(false);
    }
}