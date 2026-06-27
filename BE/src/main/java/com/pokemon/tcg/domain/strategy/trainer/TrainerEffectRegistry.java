package com.pokemon.tcg.domain.strategy.trainer;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Registry that maps card names to their {@link TrainerEffect} implementations.
 *
 * <p>Uses card name (case-insensitive) as the key instead of card ID,
 * so the same effect applies to reprints of the same card across different sets.
 *
 * <p>To add support for a new Trainer card, implement {@link TrainerEffect}
 * and register it here — no other class needs to change.
 */
@Component
public class TrainerEffectRegistry {

    private final Map<String, TrainerEffect> effects;

    public TrainerEffectRegistry(List<TrainerEffect> allEffects) {
        this.effects = new HashMap<>();
        for (TrainerEffect effect : allEffects) {
            this.effects.put(effect.getCardIdentifier().toLowerCase(), effect);
        }
    }

    /**
     * Returns the effect for the given card name, if one is registered.
     *
     * @param cardName the name of the Trainer card (case-insensitive)
     * @return an Optional containing the matching TrainerEffect, or empty if not supported
     */
    public Optional<TrainerEffect> findEffect(String cardName) {
        if (cardName == null) return Optional.empty();
        return Optional.ofNullable(effects.get(cardName.toLowerCase()));
    }
}
