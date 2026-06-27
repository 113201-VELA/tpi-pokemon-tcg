package com.pokemon.tcg.domain.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.engine.attack.AttackContext;
import com.pokemon.tcg.engine.attack.AttackEffect;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SlugmaEffect implements AttackEffect {

    @Override
    public void apply(AttackContext ctx) {
        String energyToDiscardId = ctx.getAction().getPayloadString("energyToDiscardId");
        if (energyToDiscardId == null) return;

        String attackerId         = ctx.getAction().getPlayerId();
        PlayerState attackerState = ctx.getBoardState().getStateFor(attackerId);
        ActivePokemon slugma      = attackerState.getActivePokemon();

        if (slugma == null) return;
        if (slugma.getAttachedEnergyIds() == null
                || !slugma.getAttachedEnergyIds().contains(energyToDiscardId)) return;

        List<String> energies = new ArrayList<>(slugma.getAttachedEnergyIds());
        energies.remove(energyToDiscardId);
        slugma.setAttachedEnergyIds(energies);

        List<String> discard = new ArrayList<>(
                attackerState.getDiscard() != null
                        ? attackerState.getDiscard() : new ArrayList<>());
        discard.add(energyToDiscardId);
        attackerState.setDiscard(discard);
    }
}
