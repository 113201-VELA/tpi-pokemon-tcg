package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PansearEffect implements AttackEffect {

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("pansear|fireworks");
    }

    @Override
    public void apply(AttackContext ctx) {
        String energyToDiscardId = ctx.getAction().getPayloadString("energyToDiscardId");
        if (energyToDiscardId == null) return;

        String attackerId         = ctx.getAction().getPlayerId();
        PlayerState attackerState = ctx.getBoardState().getStateFor(attackerId);
        ActivePokemon pansear     = attackerState.getActivePokemon();

        if (pansear == null) return;
        if (pansear.getAttachedEnergyIds() == null
                || !pansear.getAttachedEnergyIds().contains(energyToDiscardId)) return;

        List<String> energies = new ArrayList<>(pansear.getAttachedEnergyIds());
        energies.remove(energyToDiscardId);
        pansear.setAttachedEnergyIds(energies);

        List<String> discard = new ArrayList<>(
                attackerState.getDiscard() != null
                        ? attackerState.getDiscard() : new ArrayList<>());
        discard.add(energyToDiscardId);
        attackerState.setDiscard(discard);
    }
}
