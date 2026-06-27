package com.pokemon.tcg.engine.attack;

import com.pokemon.tcg.domain.attack.BeedrillEffect;
import com.pokemon.tcg.domain.attack.ChespinEffect;
import com.pokemon.tcg.domain.attack.ChesnaughtEffect;
import com.pokemon.tcg.domain.attack.GogoatEffect;
import com.pokemon.tcg.domain.attack.IllumiseEffect;
import com.pokemon.tcg.domain.attack.KakunaEffect;
import com.pokemon.tcg.domain.attack.LedianEffect;
import com.pokemon.tcg.domain.attack.MagcargoEffect;
import com.pokemon.tcg.domain.attack.PansageEffect;
import com.pokemon.tcg.domain.attack.PansearEffect;
import com.pokemon.tcg.domain.attack.QuilladinEffect;
import com.pokemon.tcg.domain.attack.SimisageEffect;
import com.pokemon.tcg.domain.attack.SimisearEffect;
import com.pokemon.tcg.domain.attack.SkiddoEffect;
import com.pokemon.tcg.domain.attack.SlugmaEffect;
import com.pokemon.tcg.domain.attack.SpewpaEffect;
import com.pokemon.tcg.domain.attack.VivillonEffect;
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
                                IllumiseEffect illumiseEffect,
                                PansageEffect pansageEffect,
                                SimisageEffect simisageEffect,
                                ChespinEffect chespinEffect,
                                QuilladinEffect quilladinEffect,
                                ChesnaughtEffect chesnaughtEffect,
                                SpewpaEffect spewpaEffect,
                                VivillonEffect vivillonEffect,
                                SkiddoEffect skiddoEffect,
                                GogoatEffect gogoatEffect,
                                SlugmaEffect slugmaEffect,
                                MagcargoEffect magcargoEffect,
                                PansearEffect pansearEffect,
                                SimisearEffect simisearEffect) {
        this.effects = Map.ofEntries(
                Map.entry("weedle|poison sting",          weedleEffect),
                Map.entry("kakuna|harden",                 kakunaEffect),
                Map.entry("beedrill|poison jab",           beedrillEffect),
                Map.entry("beedrill|flash needle",         beedrillEffect),
                Map.entry("ledian|mach punch",             ledianEffect),
                Map.entry("volbeat|luring glow",           volbeatEffect),
                Map.entry("volbeat|signal beam",           volbeatEffect),
                Map.entry("illumise|pheromation",          illumiseEffect),
                Map.entry("illumise|quick attack",         illumiseEffect),
                Map.entry("pansage|leech seed",            pansageEffect),
                Map.entry("simisage|torment",              simisageEffect),
                Map.entry("chespin|pin missile",           chespinEffect),
                Map.entry("quilladin|scrunch",             quilladinEffect),
                Map.entry("quilladin|wood hammer",         quilladinEffect),
                Map.entry("chesnaught|touchdown",          chesnaughtEffect),
                Map.entry("spewpa|stun spore",             spewpaEffect),
                Map.entry("vivillon|conversion powder",    vivillonEffect),
                Map.entry("vivillon|colorful wind",        vivillonEffect),
                Map.entry("skiddo|lead",                   skiddoEffect),
                Map.entry("gogoat|lead",                   gogoatEffect),
                Map.entry("gogoat|charge dash",            gogoatEffect),
                Map.entry("slugma|flamethrower",           slugmaEffect),
                Map.entry("magcargo|magma mantle",         magcargoEffect),
                Map.entry("pansear|fireworks",             pansearEffect),
                Map.entry("simisear|yawn",                 simisearEffect),
                Map.entry("simisear|flamethrower",         simisearEffect)
        );
    }

    public Optional<AttackEffect> findEffect(String cardName, String attackName) {
        if (cardName == null || attackName == null) return Optional.empty();
        String key = cardName.toLowerCase() + "|" + attackName.toLowerCase();
        return Optional.ofNullable(effects.get(key));
    }
}
