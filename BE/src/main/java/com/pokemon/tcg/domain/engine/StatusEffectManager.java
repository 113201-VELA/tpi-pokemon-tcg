package com.pokemon.tcg.domain.engine;

import com.pokemon.tcg.domain.model.game.ActivePokemon;
import com.pokemon.tcg.domain.model.game.SpecialCondition;
import org.springframework.stereotype.Component;

@Component
public class StatusEffectManager {

    private final CoinFlipService coinFlipService;

    public StatusEffectManager(CoinFlipService coinFlipService) {
        this.coinFlipService = coinFlipService;
    }

    public ActivePokemon processBetweenTurns(ActivePokemon pokemon) {
        return pokemon;
    }

    public ActivePokemon applyCondition(ActivePokemon pokemon, SpecialCondition condition) {
        return pokemon;
    }

    public ActivePokemon clearAllConditions(ActivePokemon pokemon) {
        return pokemon;
    }
}
