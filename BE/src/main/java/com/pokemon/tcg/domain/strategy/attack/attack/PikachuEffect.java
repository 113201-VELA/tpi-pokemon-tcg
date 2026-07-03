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
public class PikachuEffect implements AttackEffect {

    private static final String NUZZLE       = "nuzzle";
    private static final String QUICK_ATTACK = "quick attack";
    private static final int    HEADS_BONUS  = 10;

    private final CoinFlipService coinFlipService;
    private final StatusEffectManager statusEffectManager;

    public PikachuEffect(CoinFlipService coinFlipService, StatusEffectManager statusEffectManager) {
        this.coinFlipService = coinFlipService;
        this.statusEffectManager = statusEffectManager;
    }

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("pikachu|nuzzle", "pikachu|quick attack");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        switch (attackName) {
            case NUZZLE       -> applyNuzzle(ctx);
            case QUICK_ATTACK -> applyQuickAttack(ctx);
            default           -> { }
        }
    }

    /** Nuzzle: flip a coin. On heads, Paralyze the opponent's Active Pokémon. */
    private void applyNuzzle(AttackContext ctx) {
        if (coinFlipService.flipAndEmit(ctx, ctx.getAction().getPlayerId()) != CoinResult.HEADS) return;

        String attackerId    = ctx.getAction().getPlayerId();
        PlayerState opponent = ctx.getBoardState().getOpponentState(attackerId);
        ActivePokemon defender = opponent.getActivePokemon();

        if (defender == null) return;

        statusEffectManager.applyCondition(defender, SpecialCondition.PARALYZED);
    }

    /** Quick Attack: flip a coin. On heads, do 10 more damage. */
    private void applyQuickAttack(AttackContext ctx) {
        if (coinFlipService.flipAndEmit(ctx, ctx.getAction().getPlayerId()) != CoinResult.HEADS) return;

        List<DamageModifier> modifiers = new ArrayList<>(
                ctx.getModifiers() != null ? ctx.getModifiers() : new ArrayList<>());
        modifiers.add(new DamageModifier("quick-attack-heads", HEADS_BONUS, true));
        ctx.setModifiers(modifiers);
    }
}