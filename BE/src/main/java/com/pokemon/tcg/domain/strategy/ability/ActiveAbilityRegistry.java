package com.pokemon.tcg.domain.strategy.ability;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class ActiveAbilityRegistry {

    private final Map<String, ActiveAbilityEffect> abilities;

    public ActiveAbilityRegistry(List<ActiveAbilityEffect> allAbilities) {
        this.abilities = new HashMap<>();
        for (ActiveAbilityEffect ability : allAbilities) {
            this.abilities.put(ability.getIdentifier().toLowerCase(), ability);
        }
    }

    public Optional<ActiveAbilityEffect> findAbility(String cardName, String abilityName) {
        if (cardName == null || abilityName == null) return Optional.empty();
        String key = cardName.toLowerCase() + "|" + abilityName.toLowerCase();
        return Optional.ofNullable(abilities.get(key));
    }
}
