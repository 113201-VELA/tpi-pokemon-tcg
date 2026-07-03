package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import com.pokemon.tcg.engine.CardLookupPort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * xy1-87 Jigglypuff
 *
 * Heartfelt Song: discard a Darkness Energy attached to the opponent's
 *                 Active Pokémon.
 * Rollout: 10 damage, no additional text — plain attack, no effect needed.
 */
@Component
public class Jigglypuff87Effect implements AttackEffect {

    private static final String HEARTFELT_SONG = "heartfelt song";
    private static final String DARKNESS_TYPE  = EnergyType.DARKNESS.name();

    private final CardLookupPort cardLookupPort;

    public Jigglypuff87Effect(CardLookupPort cardLookupPort) {
        this.cardLookupPort = cardLookupPort;
    }

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("jigglypuff|heartfelt song");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        if (HEARTFELT_SONG.equals(attackName)) {
            applyHeartfeltSong(ctx);
        }
    }

    /**
     * Discard a Darkness Energy attached to the opponent's Active Pokémon.
     * If the attacker specifies which Energy to discard via
     * {@code energyCardId} and it is a Darkness Energy actually attached,
     * that one is discarded; otherwise the first attached Darkness Energy
     * found is discarded. If no Darkness Energy is attached, nothing happens.
     */
    private void applyHeartfeltSong(AttackContext ctx) {
        String attackerId       = ctx.getAction().getPlayerId();
        PlayerState opponent    = ctx.getBoardState().getOpponentState(attackerId);
        ActivePokemon defender  = opponent.getActivePokemon();

        if (defender == null
                || defender.getAttachedEnergyIds() == null
                || defender.getAttachedEnergyIds().isEmpty()) return;

        List<String> energies = new ArrayList<>(defender.getAttachedEnergyIds());
        String requestedEnergyId = ctx.getAction().getPayloadString("energyCardId");

        String chosen;
        if (requestedEnergyId != null
                && energies.contains(requestedEnergyId)
                && isDarknessEnergy(requestedEnergyId)) {
            chosen = requestedEnergyId;
        } else {
            chosen = energies.stream()
                    .filter(this::isDarknessEnergy)
                    .findFirst()
                    .orElse(null);
        }

        if (chosen == null) return;

        energies.remove(chosen);
        defender.setAttachedEnergyIds(energies);

        List<String> discard = new ArrayList<>(
                opponent.getDiscard() != null ? opponent.getDiscard() : new ArrayList<>());
        discard.add(chosen);
        opponent.setDiscard(discard);
    }

    private boolean isDarknessEnergy(String cardId) {
        return cardLookupPort.findCardById(cardId)
                .map(card -> card.getTypes() != null && card.getTypes().contains(DARKNESS_TYPE))
                .orElse(false);
    }
}