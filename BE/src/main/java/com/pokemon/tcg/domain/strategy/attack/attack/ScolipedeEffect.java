package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import com.pokemon.tcg.domain.strategy.attack.DamageModifier;
import com.pokemon.tcg.engine.CoinFlipService;
import com.pokemon.tcg.engine.StatusEffectManager;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ScolipedeEffect implements AttackEffect {

    private static final String RANDOM_PECK  = "random peck";
    private static final String POISON_RING  = "poison ring";
    private static final int    COIN_FLIPS   = 2;
    private static final int    DAMAGE_PER_HEAD = 20;

    private final CoinFlipService coinFlipService;
    private final StatusEffectManager statusEffectManager;

    public ScolipedeEffect(CoinFlipService coinFlipService, StatusEffectManager statusEffectManager) {
        this.coinFlipService = coinFlipService;
        this.statusEffectManager = statusEffectManager;
    }

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("scolipede|random peck", "scolipede|poison ring");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        switch (attackName) {
            case RANDOM_PECK -> applyRandomPeck(ctx);
            case POISON_RING -> applyPoisonRing(ctx);
            default          -> { }
        }
    }

    /** Random Peck: flip 2 coins, deal 20 extra damage per heads. */
    private void applyRandomPeck(AttackContext ctx) {
        int heads = 0;
        for (int i = 0; i < COIN_FLIPS; i++) {
            if (coinFlipService.flipAndEmit(ctx, ctx.getAction().getPlayerId()) == CoinResult.HEADS) heads++;
        }

        if (heads > 0) {
            List<DamageModifier> modifiers = new ArrayList<>(
                    ctx.getModifiers() != null ? ctx.getModifiers() : new ArrayList<>());
            modifiers.add(new DamageModifier(
                    "random-peck-heads", heads * DAMAGE_PER_HEAD, true));
            ctx.setModifiers(modifiers);
        }
    }

    /**
     * Poison Ring: Poison the opponent's Active Pokémon and prevent it
     * from retreating during the opponent's next turn.
     */
    private void applyPoisonRing(AttackContext ctx) {
        String attackerId    = ctx.getAction().getPlayerId();
        PlayerState opponent = ctx.getBoardState().getOpponentState(attackerId);
        ActivePokemon defender = opponent.getActivePokemon();

        if (defender == null) return;

        statusEffectManager.applyCondition(defender, SpecialCondition.POISONED);

        List<PokemonEffect> effects = new ArrayList<>(
                defender.getActiveEffects() != null
                        ? defender.getActiveEffects() : new ArrayList<>());
        if (!effects.contains(PokemonEffect.CANT_RETREAT)) {
            effects.add(PokemonEffect.CANT_RETREAT);
        }
        defender.setActiveEffects(effects);
    }
}