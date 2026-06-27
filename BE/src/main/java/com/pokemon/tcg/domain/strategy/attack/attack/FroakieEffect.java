package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import com.pokemon.tcg.engine.CoinFlipService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Component
public class FroakieEffect implements AttackEffect {

    private static final String BOUNCE = "bounce";

    private final CoinFlipService coinFlipService;

    public FroakieEffect(CoinFlipService coinFlipService) {
        this.coinFlipService = coinFlipService;
    }

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("froakie|bounce");
    }

    @Override
    public void apply(AttackContext ctx) {
        if (!BOUNCE.equals(ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase() : "")) return;

        if (coinFlipService.flip() != CoinResult.HEADS) return;

        String attackerId    = ctx.getAction().getPlayerId();
        PlayerState attacker = ctx.getBoardState().getStateFor(attackerId);

        List<BenchPokemon> bench = attacker.getBench();
        if (bench == null || bench.isEmpty()) return;

        String replacementInstanceId =
                ctx.getAction().getPayloadString("replacementInstanceId");

        // Find the chosen bench Pokémon — fall back to first bench slot if not specified
        BenchPokemon replacement = null;
        if (replacementInstanceId != null) {
            replacement = bench.stream()
                    .filter(b -> b.getInstanceId().equals(replacementInstanceId))
                    .findFirst().orElse(null);
        }
        if (replacement == null) {
            replacement = bench.get(0);
        }

        ActivePokemon oldActive = attacker.getActivePokemon();

        BenchPokemon newBench = BenchPokemon.builder()
                .instanceId(oldActive.getInstanceId())
                .cardId(oldActive.getCardId())
                .attachedEnergyIds(oldActive.getAttachedEnergyIds())
                .attachedToolId(oldActive.getAttachedToolId())
                .evolutionStack(oldActive.getEvolutionStack())
                .damageCounters(oldActive.getDamageCounters())
                .enteredThisTurn(false)
                .build();

        ActivePokemon newActive = ActivePokemon.builder()
                .instanceId(replacement.getInstanceId())
                .cardId(replacement.getCardId())
                .attachedEnergyIds(replacement.getAttachedEnergyIds())
                .attachedToolId(replacement.getAttachedToolId())
                .evolutionStack(replacement.getEvolutionStack())
                .damageCounters(replacement.getDamageCounters())
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .enteredThisTurn(false)
                .build();

        List<BenchPokemon> newBenchList = new ArrayList<>(bench);
        newBenchList.remove(replacement);
        newBenchList.add(newBench);

        attacker.setBench(newBenchList);
        attacker.setActivePokemon(newActive);
    }
}