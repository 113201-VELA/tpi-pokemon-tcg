package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Trevenant — Tree Slam (60 damage).
 * Also does 20 damage to 2 of the opponent's Benched Pokémon.
 * Weakness and Resistance don't apply to Benched Pokémon.
 */
@Component
public class TrevenantEffect implements AttackEffect {

    private static final String TREE_SLAM           = "tree slam";
    private static final int    BENCH_DAMAGE_COUNTERS = 2; // 20 damage = 2 counters
    private static final int    MAX_BENCH_TARGETS    = 2;

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("trevenant|tree slam");
    }

    @Override
    public void apply(AttackContext ctx) {
        if (!TREE_SLAM.equals(ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase() : "")) return;

        String attackerId    = ctx.getAction().getPlayerId();
        PlayerState opponent = ctx.getBoardState().getOpponentState(attackerId);

        List<BenchPokemon> bench = opponent.getBench();
        if (bench == null || bench.isEmpty()) return;

        int targets = Math.min(bench.size(), MAX_BENCH_TARGETS);
        for (int i = 0; i < targets; i++) {
            BenchPokemon target = bench.get(i);
            target.setDamageCounters(target.getDamageCounters() + BENCH_DAMAGE_COUNTERS);
        }
    }
}