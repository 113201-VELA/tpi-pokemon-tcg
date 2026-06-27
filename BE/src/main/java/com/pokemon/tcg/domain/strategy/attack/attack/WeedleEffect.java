package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.ActivePokemon;
import com.pokemon.tcg.domain.model.game.PlayerState;
import com.pokemon.tcg.domain.strategy.attack.DamageModifier;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class WeedleEffect implements AttackEffect {

    private static final int GRASS_BONUS = 20;

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("weedle|poison sting");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackerId = ctx.getAction().getPlayerId();
        PlayerState defenderState = ctx.getBoardState().getOpponentState(attackerId);
        ActivePokemon defender = defenderState.getActivePokemon();

        if (defender == null) return;

        if (isGrassType(defender)) {
            List<DamageModifier> modifiers = new ArrayList<>(
                    ctx.getModifiers() != null ? ctx.getModifiers() : new ArrayList<>());
            modifiers.add(new DamageModifier("weedle-grass-bonus", GRASS_BONUS, true));
            ctx.setModifiers(modifiers);
        }
    }

    private boolean isGrassType(ActivePokemon pokemon) {
        return pokemon.getTypes() != null
                && pokemon.getTypes().contains(EnergyType.GRASS);
    }
}
