package com.pokemon.tcg.engine;

import com.pokemon.tcg.domain.model.game.ActivePokemon;
import com.pokemon.tcg.domain.model.game.BoardState;
import com.pokemon.tcg.domain.model.game.GameEvent;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class VictoryConditionChecker {

    public Optional<GameEvent> check(BoardState state) {
        return Optional.empty();
    }

    public boolean isKnockedOut(ActivePokemon pokemon, int maxHp) {
        return pokemon.getDamageCounters() * 10 >= maxHp;
    }
}
