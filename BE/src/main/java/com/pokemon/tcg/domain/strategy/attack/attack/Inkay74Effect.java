package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class Inkay74Effect implements AttackEffect {

    private static final String CONFUSION_WAVE = "confusion wave";

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("inkay|confusion wave");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        if (CONFUSION_WAVE.equals(attackName)) {
            applyConfusionWave(ctx);
        }
    }

    private void applyConfusionWave(AttackContext ctx) {
        String attackerId = ctx.getAction().getPlayerId();
        PlayerState attackerState = ctx.getBoardState().getStateFor(attackerId);
        PlayerState defenderState = ctx.getBoardState().getOpponentState(attackerId);

        confuse(attackerState.getActivePokemon());
        confuse(defenderState.getActivePokemon());
    }

    private void confuse(ActivePokemon pokemon) {
        if (pokemon == null) return;
        Set<SpecialCondition> conditions = pokemon.getConditions() != null
                ? new HashSet<>(pokemon.getConditions())
                : new HashSet<>();
        conditions.add(SpecialCondition.CONFUSED);
        pokemon.setConditions(conditions);
    }
}