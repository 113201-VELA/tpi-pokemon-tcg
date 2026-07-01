package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class Aegislash86Effect implements AttackEffect {

    private static final String KINGS_SHIELD = "king's shield";

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("aegislash|king's shield");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        if (KINGS_SHIELD.equals(attackName)) {
            applyKingsShield(ctx);
        }
    }

    private void applyKingsShield(AttackContext ctx) {
        String attackerId = ctx.getAction().getPlayerId();
        PlayerState attackerState = ctx.getBoardState().getStateFor(attackerId);
        ActivePokemon aegislash = attackerState.getActivePokemon();

        if (aegislash == null) return;

        List<PokemonEffect> effects = new ArrayList<>(
                aegislash.getActiveEffects() != null
                        ? aegislash.getActiveEffects()
                        : new ArrayList<>());
        if (!effects.contains(PokemonEffect.INVULNERABLE)) {
            effects.add(PokemonEffect.INVULNERABLE);
        }
        aegislash.setActiveEffects(effects);

        aegislash.setBlockedAttackName(KINGS_SHIELD);
    }
}