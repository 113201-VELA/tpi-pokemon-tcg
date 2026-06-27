package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import com.pokemon.tcg.engine.CoinFlipService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ElectrodeEffect implements AttackEffect {

    private static final String EERIE_IMPULSE = "eerie impulse";

    private final CoinFlipService coinFlipService;

    public ElectrodeEffect(CoinFlipService coinFlipService) {
        this.coinFlipService = coinFlipService;
    }

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("electrode|eerie impulse");
    }

    @Override
    public void apply(AttackContext ctx) {
        if (!EERIE_IMPULSE.equals(ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase() : "")) return;

        if (coinFlipService.flip() != CoinResult.HEADS) return;

        String attackerId    = ctx.getAction().getPlayerId();
        PlayerState opponent = ctx.getBoardState().getOpponentState(attackerId);

        String targetInstanceId = ctx.getAction().getPayloadString("targetInstanceId");
        String energyCardId     = ctx.getAction().getPayloadString("energyCardId");

        if (targetInstanceId == null || energyCardId == null) return;

        // Try Active Pokémon first
        if (discardFromActive(opponent, targetInstanceId, energyCardId)) return;

        // Try Bench
        discardFromBench(opponent, targetInstanceId, energyCardId);
    }

    private boolean discardFromActive(PlayerState opponent,
                                      String targetInstanceId,
                                      String energyCardId) {
        ActivePokemon active = opponent.getActivePokemon();
        if (active == null
                || !active.getInstanceId().equals(targetInstanceId)) return false;
        if (active.getAttachedEnergyIds() == null
                || !active.getAttachedEnergyIds().contains(energyCardId)) return false;

        List<String> energies = new ArrayList<>(active.getAttachedEnergyIds());
        energies.remove(energyCardId);
        active.setAttachedEnergyIds(energies);

        addToDiscard(opponent, energyCardId);
        return true;
    }

    private void discardFromBench(PlayerState opponent,
                                  String targetInstanceId,
                                  String energyCardId) {
        if (opponent.getBench() == null) return;

        opponent.getBench().stream()
                .filter(b -> b.getInstanceId().equals(targetInstanceId))
                .findFirst()
                .ifPresent(bench -> {
                    if (bench.getAttachedEnergyIds() == null
                            || !bench.getAttachedEnergyIds().contains(energyCardId)) return;

                    List<String> energies = new ArrayList<>(bench.getAttachedEnergyIds());
                    energies.remove(energyCardId);
                    bench.setAttachedEnergyIds(energies);

                    addToDiscard(opponent, energyCardId);
                });
    }

    private void addToDiscard(PlayerState opponent, String energyCardId) {
        List<String> discard = new ArrayList<>(
                opponent.getDiscard() != null ? opponent.getDiscard() : new ArrayList<>());
        discard.add(energyCardId);
        opponent.setDiscard(discard);
    }
}