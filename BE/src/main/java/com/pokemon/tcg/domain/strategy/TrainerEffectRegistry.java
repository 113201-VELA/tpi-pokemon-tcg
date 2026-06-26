package com.pokemon.tcg.domain.strategy;

import com.pokemon.tcg.domain.strategy.item.EvosodaEffect;
import com.pokemon.tcg.domain.strategy.item.ProfessorsLetterEffect;
import com.pokemon.tcg.domain.strategy.item.SuperPotionEffect;
import com.pokemon.tcg.domain.strategy.supporter.CassiusEffect;
import com.pokemon.tcg.domain.strategy.supporter.ProfessorSycamoreEffect;
import com.pokemon.tcg.domain.strategy.supporter.ShaunaEffect;
import com.pokemon.tcg.domain.strategy.supporter.TeamFlareGruntEffect;
import org.springframework.stereotype.Component;

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

    public TrainerEffectRegistry(SuperPotionEffect superPotionEffect,
                                 ProfessorSycamoreEffect professorSycamoreEffect,
                                 ShaunaEffect shaunaEffect,
                                 CassiusEffect cassiusEffect,
                                 TeamFlareGruntEffect teamFlareGruntEffect,
                                 ProfessorsLetterEffect professorsLetterEffect,
                                 EvosodaEffect evosodaEffect) {
        this.effects = Map.ofEntries(
                Map.entry("super potion",        superPotionEffect),
                Map.entry("professor sycamore",  professorSycamoreEffect),
                Map.entry("shauna",              shaunaEffect),
                Map.entry("cassius",             cassiusEffect),
                Map.entry("team flare grunt",    teamFlareGruntEffect),
                Map.entry("professor's letter",  professorsLetterEffect),
                Map.entry("evosoda",             evosodaEffect)
        );
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
