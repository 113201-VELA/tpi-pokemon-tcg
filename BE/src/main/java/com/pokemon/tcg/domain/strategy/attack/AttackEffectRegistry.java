package com.pokemon.tcg.domain.strategy.attack;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class AttackEffectRegistry {

    private final Map<String, AttackEffect> effects;

    public AttackEffectRegistry(List<AttackEffect> allEffects) {
        this.effects = new HashMap<>();
        for (AttackEffect effect : allEffects) {
            for (String key : effect.getSupportedAttacks()) {
                this.effects.put(key.toLowerCase(), effect);
            }
        }
    }

    public Optional<AttackEffect> findEffect(String cardName, String attackName) {
        if (cardName == null || attackName == null) return Optional.empty();
        String key = cardName.toLowerCase() + "|" + attackName.toLowerCase();
        return Optional.ofNullable(effects.get(key));
    }
}
