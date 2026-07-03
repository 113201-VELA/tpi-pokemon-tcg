package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.ActivePokemon;
import com.pokemon.tcg.domain.model.game.PlayerState;
import com.pokemon.tcg.domain.model.game.SpecialCondition;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import com.pokemon.tcg.engine.StatusEffectManager;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * xy1-56 Pumpkaboo — "Confuse Ray"
 * Your opponent's Active Pokémon is now Confused.
 */
@Component
public class PumpkabooEffect implements AttackEffect {

    private final StatusEffectManager statusEffectManager;

    public PumpkabooEffect(StatusEffectManager statusEffectManager) {
        this.statusEffectManager = statusEffectManager;
    }

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("pumpkaboo|confuse ray");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackerId = ctx.getAction().getPlayerId();
        PlayerState opponent = ctx.getBoardState().getOpponentState(attackerId);
        ActivePokemon defender = opponent.getActivePokemon();

        if (defender == null) return;

        statusEffectManager.applyCondition(defender, SpecialCondition.CONFUSED);
    }
}