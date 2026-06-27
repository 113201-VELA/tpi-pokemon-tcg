package com.pokemon.tcg.domain.strategy.ability;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class PassiveAbilityRegistry {

    private final Map<String, PassiveAbilityEffect> abilities;

    public PassiveAbilityRegistry(List<PassiveAbilityEffect> allAbilities) {
        this.abilities = new HashMap<>();
        for (PassiveAbilityEffect ability : allAbilities) {
            this.abilities.put(ability.getIdentifier().toLowerCase(), ability);
        }
    }

    public Optional<PassiveAbilityEffect> findAbility(String cardName) {
        if (cardName == null) return Optional.empty();
        return Optional.ofNullable(abilities.get(cardName.toLowerCase()));
    }
}
