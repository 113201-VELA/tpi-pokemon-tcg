package com.pokemon.tcg.domain.strategy.ability;

import com.pokemon.tcg.domain.strategy.ability.ability.ChesnaughtAbility;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class PassiveAbilityRegistry {

    private final Map<String, PassiveAbilityEffect> abilities;

    public PassiveAbilityRegistry(ChesnaughtAbility chesnaughtAbility) {
        this.abilities = Map.ofEntries(
                Map.entry("chesnaught", chesnaughtAbility)
        );
    }

    public Optional<PassiveAbilityEffect> findAbility(String cardName) {
        if (cardName == null) return Optional.empty();
        return Optional.ofNullable(abilities.get(cardName.toLowerCase()));
    }
}
