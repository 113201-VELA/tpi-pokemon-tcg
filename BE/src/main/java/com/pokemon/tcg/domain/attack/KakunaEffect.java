package com.pokemon.tcg.domain.attack;

import com.pokemon.tcg.domain.model.game.ActivePokemon;
import com.pokemon.tcg.domain.model.game.PlayerState;
import com.pokemon.tcg.domain.model.game.PokemonEffect;
import com.pokemon.tcg.engine.attack.AttackContext;
import com.pokemon.tcg.engine.attack.AttackEffect;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class KakunaEffect implements AttackEffect {

    @Override
    public void apply(AttackContext ctx) {
        String attackerId = ctx.getAction().getPlayerId();
        PlayerState attackerState = ctx.getBoardState().getStateFor(attackerId);
        ActivePokemon attacker = attackerState.getActivePokemon();

        if (attacker == null) return;

        List<PokemonEffect> effects = new ArrayList<>(
                attacker.getActiveEffects() != null
                        ? attacker.getActiveEffects()
                        : new ArrayList<>());
        if (!effects.contains(PokemonEffect.HARDEN)) {
            effects.add(PokemonEffect.HARDEN);
        }
        attacker.setActiveEffects(effects);
    }
}
