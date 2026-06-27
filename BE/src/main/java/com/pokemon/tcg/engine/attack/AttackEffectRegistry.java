package com.pokemon.tcg.engine.attack;

import com.pokemon.tcg.domain.attack.WeedleEffect;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class AttackEffectRegistry {

    private final Map<String, AttackEffect> effects;

    public AttackEffectRegistry(WeedleEffect weedleEffect) {
        this.effects = Map.ofEntries(
                Map.entry("weedle|poison sting", weedleEffect)
        );
    }

    public Optional<AttackEffect> findEffect(String cardName, String attackName) {
        if (cardName == null || attackName == null) return Optional.empty();
        String key = cardName.toLowerCase() + "|" + attackName.toLowerCase();
        return Optional.ofNullable(effects.get(key));
    }
}
