package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.engine.CoinFlipService;
import com.pokemon.tcg.domain.strategy.attack.DamageModifier;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class BeedrillEffect implements AttackEffect {

    private static final String POISON_JAB   = "poison jab";
    private static final String FLASH_NEEDLE = "flash needle";
    private static final int    FLASH_DAMAGE = 40;
    private static final int    COIN_FLIPS   = 3;

    private final CoinFlipService coinFlipService;

    public BeedrillEffect(CoinFlipService coinFlipService) {
        this.coinFlipService = coinFlipService;
    }

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("beedrill|poison jab", "beedrill|flash needle");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        switch (attackName) {
            case POISON_JAB   -> applyPoisonJab(ctx);
            case FLASH_NEEDLE -> applyFlashNeedle(ctx);
            default           -> { }
        }
    }

    private void applyPoisonJab(AttackContext ctx) {
        String attackerId     = ctx.getAction().getPlayerId();
        PlayerState opponent  = ctx.getBoardState().getOpponentState(attackerId);
        ActivePokemon defender = opponent.getActivePokemon();

        if (defender == null) return;

        defender.getConditions().add(SpecialCondition.POISONED);
    }

    private void applyFlashNeedle(AttackContext ctx) {
        int heads = 0;
        for (int i = 0; i < COIN_FLIPS; i++) {
            if (coinFlipService.flip() == CoinResult.HEADS) {
                heads++;
            }
        }

        if (heads > 0) {
            List<DamageModifier> modifiers = new ArrayList<>(
                    ctx.getModifiers() != null ? ctx.getModifiers() : new ArrayList<>());
            modifiers.add(new DamageModifier("flash-needle-heads", heads * FLASH_DAMAGE, true));
            ctx.setModifiers(modifiers);
        }

        if (heads == COIN_FLIPS) {
            String attackerId      = ctx.getAction().getPlayerId();
            PlayerState attackerState = ctx.getBoardState().getStateFor(attackerId);
            ActivePokemon beedrill = attackerState.getActivePokemon();

            if (beedrill != null) {
                List<PokemonEffect> effects = new ArrayList<>(
                        beedrill.getActiveEffects() != null
                                ? beedrill.getActiveEffects()
                                : new ArrayList<>());
                if (!effects.contains(PokemonEffect.INVULNERABLE)) {
                    effects.add(PokemonEffect.INVULNERABLE);
                }
                beedrill.setActiveEffects(effects);
            }
        }
    }
}
