package com.pokemon.tcg.engine.attack;

import com.pokemon.tcg.domain.attack.BeedrillEffect;
import com.pokemon.tcg.domain.attack.IllumiseEffect;
import com.pokemon.tcg.domain.attack.KakunaEffect;
import com.pokemon.tcg.domain.attack.LedianEffect;
import com.pokemon.tcg.domain.attack.VolbeatEffect;
import com.pokemon.tcg.domain.attack.WeedleEffect;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class AttackEffectRegistry {

    private final Map<String, AttackEffect> effects;

    public AttackEffectRegistry(WeedleEffect weedleEffect,
                                KakunaEffect kakunaEffect,
                                BeedrillEffect beedrillEffect,
                                LedianEffect ledianEffect,
                                VolbeatEffect volbeatEffect,
                                IllumiseEffect illumiseEffect) {
        this.effects = Map.ofEntries(
                Map.entry("weedle|poison sting",    weedleEffect),
                Map.entry("kakuna|harden",           kakunaEffect),
                Map.entry("beedrill|poison jab",     beedrillEffect),
                Map.entry("beedrill|flash needle",   beedrillEffect),
                Map.entry("ledian|mach punch",       ledianEffect),
                Map.entry("volbeat|luring glow",     volbeatEffect),
                Map.entry("volbeat|signal beam",     volbeatEffect),
                Map.entry("illumise|pheromation",    illumiseEffect),
                Map.entry("illumise|quick attack",   illumiseEffect)
        );
    }

    public Optional<AttackEffect> findEffect(String cardName, String attackName) {
        if (cardName == null || attackName == null) return Optional.empty();
        String key = cardName.toLowerCase() + "|" + attackName.toLowerCase();
        return Optional.ofNullable(effects.get(key));
    }
}
