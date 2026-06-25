package com.pokemon.tcg.engine;

import com.pokemon.tcg.domain.model.game.ActivePokemon;
import com.pokemon.tcg.domain.model.game.CoinResult;
import com.pokemon.tcg.domain.model.game.SpecialCondition;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Manages special condition effects applied between turns:
 * Poison, Burn, Sleep and Paralysis according to official TCG rules.
 */
@Component
public class StatusEffectManager {

    private final CoinFlipService coinFlipService;

    public StatusEffectManager(CoinFlipService coinFlipService) {
        this.coinFlipService = coinFlipService;
    }

    /**
     * Processes all between-turn effects for the given Pokémon.
     * Order: Poison → Burn → Sleep → Paralysis (auto-cure).
     */
    public ActivePokemon processBetweenTurns(ActivePokemon pokemon) {
        if (pokemon == null) return null;

        if (pokemon.hasCondition(SpecialCondition.POISONED)) {
            pokemon.setDamageCounters(pokemon.getDamageCounters() + 1);
        }

        if (pokemon.hasCondition(SpecialCondition.BURNED)) {
            CoinResult flip = coinFlipService.flip();
            if (flip == CoinResult.TAILS) {
                pokemon.setDamageCounters(pokemon.getDamageCounters() + 2);
            }
        }

        if (pokemon.hasCondition(SpecialCondition.ASLEEP)) {
            CoinResult flip = coinFlipService.flip();
            if (flip == CoinResult.HEADS) {
                removeCondition(pokemon, SpecialCondition.ASLEEP);
            }
        }

        if (pokemon.hasCondition(SpecialCondition.PARALYZED)) {
            removeCondition(pokemon, SpecialCondition.PARALYZED);
        }

        return pokemon;
    }

    /**
     * Applies a special condition to the Pokémon.
     * ASLEEP, CONFUSED and PARALYZED are mutually exclusive — the newest replaces the previous.
     */
    public ActivePokemon applyCondition(ActivePokemon pokemon, SpecialCondition condition) {
        if (pokemon == null) return null;

        Set<SpecialCondition> conditions = new HashSet<>(
                pokemon.getConditions() != null ? pokemon.getConditions() : new HashSet<>());

        // Mutually exclusive conditions — remove previous rotation conditions
        if (condition == SpecialCondition.ASLEEP
                || condition == SpecialCondition.CONFUSED
                || condition == SpecialCondition.PARALYZED) {
            conditions.remove(SpecialCondition.ASLEEP);
            conditions.remove(SpecialCondition.CONFUSED);
            conditions.remove(SpecialCondition.PARALYZED);
        }

        conditions.add(condition);
        pokemon.setConditions(conditions);
        return pokemon;
    }

    /**
     * Clears all special conditions (e.g. when a Pokémon goes to the bench or evolves).
     */
    public ActivePokemon clearAllConditions(ActivePokemon pokemon) {
        if (pokemon == null) return null;
        pokemon.setConditions(new HashSet<>());
        return pokemon;
    }

    private void removeCondition(ActivePokemon pokemon, SpecialCondition condition) {
        if (pokemon.getConditions() != null) {
            Set<SpecialCondition> conditions = new HashSet<>(pokemon.getConditions());
            conditions.remove(condition);
            pokemon.setConditions(conditions);
        }
    }
}