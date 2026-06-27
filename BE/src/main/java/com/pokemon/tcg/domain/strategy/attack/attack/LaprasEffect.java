package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import com.pokemon.tcg.domain.strategy.attack.DamageModifier;
import com.pokemon.tcg.engine.CardLookupPort;
import com.pokemon.tcg.engine.CoinFlipService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class LaprasEffect implements AttackEffect {

    private static final String SEAFARING  = "seafaring";
    private static final String HYDRO_PUMP = "hydro pump";

    private static final String WATER_TYPE         = EnergyType.WATER.name();
    private static final int    SEAFARING_FLIPS    = 3;
    private static final int    HYDRO_PUMP_BONUS   = 20;

    private final CoinFlipService coinFlipService;
    private final CardLookupPort cardLookupPort;

    public LaprasEffect(CoinFlipService coinFlipService, CardLookupPort cardLookupPort) {
        this.coinFlipService = coinFlipService;
        this.cardLookupPort  = cardLookupPort;
    }

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("lapras|seafaring", "lapras|hydro pump");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        switch (attackName) {
            case SEAFARING  -> applySeafaring(ctx);
            case HYDRO_PUMP -> applyHydroPump(ctx);
            default         -> { }
        }
    }

    /**
     * Seafaring: flip 3 coins. For each heads, attach one Water Energy from
     * the player's discard pile to any of their Benched Pokémon.
     *
     * <p>The player specifies the distribution via the {@code benchTargets} payload
     * key — a list of maps each containing {@code instanceId} and {@code energyCardId}.
     * If no targets are specified or fewer targets than heads are provided,
     * remaining energies are attached to the first available Bench Pokémon automatically.
     */
    private void applySeafaring(AttackContext ctx) {
        int heads = 0;
        for (int i = 0; i < SEAFARING_FLIPS; i++) {
            if (coinFlipService.flip() == CoinResult.HEADS) heads++;
        }

        if (heads == 0) return;

        String attackerId    = ctx.getAction().getPlayerId();
        PlayerState attacker = ctx.getBoardState().getStateFor(attackerId);

        List<BenchPokemon> bench = attacker.getBench();
        if (bench == null || bench.isEmpty()) return;

        // Collect Water Energies from discard
        List<String> discard = new ArrayList<>(
                attacker.getDiscard() != null ? attacker.getDiscard() : new ArrayList<>());
        List<String> waterEnergiesInDiscard = discard.stream()
                .filter(this::isWaterEnergy)
                .toList();

        if (waterEnergiesInDiscard.isEmpty()) return;

        // How many we can actually attach
        int toAttach = Math.min(heads, waterEnergiesInDiscard.size());

        // Try to read player-specified targets from payload
        List<Map<String, String>> benchTargets = getBenchTargets(ctx.getAction());

        for (int i = 0; i < toAttach; i++) {
            String energyId = waterEnergiesInDiscard.get(i);

            // Find target bench Pokémon — use player's choice if available, else first bench
            BenchPokemon target = null;
            if (benchTargets != null && i < benchTargets.size()) {
                String targetInstanceId = benchTargets.get(i).get("instanceId");
                target = bench.stream()
                        .filter(b -> b.getInstanceId().equals(targetInstanceId))
                        .findFirst()
                        .orElse(null);
            }
            if (target == null) {
                target = bench.get(0);
            }

            // Attach energy to target
            List<String> energies = new ArrayList<>(
                    target.getAttachedEnergyIds() != null
                            ? target.getAttachedEnergyIds() : new ArrayList<>());
            energies.add(energyId);
            target.setAttachedEnergyIds(energies);

            // Remove from discard
            discard.remove(energyId);
        }

        attacker.setDiscard(discard);
    }

    /**
     * Hydro Pump: does 20 more damage for each Water Energy attached to Lapras.
     */
    private void applyHydroPump(AttackContext ctx) {
        String attackerId    = ctx.getAction().getPlayerId();
        PlayerState attacker = ctx.getBoardState().getStateFor(attackerId);
        ActivePokemon lapras = attacker.getActivePokemon();

        if (lapras == null) return;
        if (lapras.getAttachedEnergyIds() == null
                || lapras.getAttachedEnergyIds().isEmpty()) return;

        long waterCount = lapras.getAttachedEnergyIds().stream()
                .filter(this::isWaterEnergy)
                .count();

        if (waterCount > 0) {
            List<DamageModifier> modifiers = new ArrayList<>(
                    ctx.getModifiers() != null ? ctx.getModifiers() : new ArrayList<>());
            modifiers.add(new DamageModifier(
                    "hydro-pump-water", (int) waterCount * HYDRO_PUMP_BONUS, true));
            ctx.setModifiers(modifiers);
        }
    }

    private boolean isWaterEnergy(String cardId) {
        return cardLookupPort.findCardById(cardId)
                .map(card -> card.getTypes() != null
                        && card.getTypes().contains(WATER_TYPE))
                .orElse(false);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> getBenchTargets(GameAction action) {
        Object raw = action.getPayload() != null
                ? action.getPayload().get("benchTargets")
                : null;
        if (raw instanceof List<?> list) {
            return (List<Map<String, String>>) list;
        }
        return null;
    }
}