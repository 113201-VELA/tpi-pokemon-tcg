package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.engine.CoinFlipService;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class QuilladinEffect implements AttackEffect {

    private static final String SCRUNCH     = "scrunch";
    private static final String WOOD_HAMMER = "wood hammer";
    private static final int    RECOIL_COUNTERS = 1;

    private final CoinFlipService coinFlipService;

    public QuilladinEffect(CoinFlipService coinFlipService) {
        this.coinFlipService = coinFlipService;
    }

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("quilladin|scrunch", "quilladin|wood hammer");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        switch (attackName) {
            case SCRUNCH     -> applyScrunch(ctx);
            case WOOD_HAMMER -> applyWoodHammer(ctx);
            default          -> { }
        }
    }

    private void applyScrunch(AttackContext ctx) {
        if (coinFlipService.flipAndEmit(ctx, ctx.getAction().getPlayerId()) != CoinResult.HEADS) return;

        String attackerId         = ctx.getAction().getPlayerId();
        PlayerState attackerState = ctx.getBoardState().getStateFor(attackerId);
        ActivePokemon quilladin   = attackerState.getActivePokemon();

        if (quilladin == null) return;

        List<PokemonEffect> effects = new ArrayList<>(
                quilladin.getActiveEffects() != null
                        ? quilladin.getActiveEffects()
                        : new ArrayList<>());
        if (!effects.contains(PokemonEffect.INVULNERABLE)) {
            effects.add(PokemonEffect.INVULNERABLE);
        }
        quilladin.setActiveEffects(effects);
    }

    private void applyWoodHammer(AttackContext ctx) {
        String attackerId         = ctx.getAction().getPlayerId();
        PlayerState attackerState = ctx.getBoardState().getStateFor(attackerId);
        ActivePokemon quilladin   = attackerState.getActivePokemon();

        if (quilladin == null) return;

        quilladin.setDamageCounters(quilladin.getDamageCounters() + RECOIL_COUNTERS);
    }
}
