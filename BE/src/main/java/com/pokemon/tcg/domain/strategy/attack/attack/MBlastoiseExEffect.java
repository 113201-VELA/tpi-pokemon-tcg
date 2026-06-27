package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MBlastoiseExEffect implements AttackEffect {

    private static final String HYDRO_BOMBARD = "hydro bombard";

    // Damage dealt to each of the 2 Benched Pokémon (30 damage = 3 counters)
    private static final int BENCH_DAMAGE_COUNTERS = 3;

    // Maximum number of Benched Pokémon hit by this attack
    private static final int MAX_BENCH_TARGETS = 2;

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("m blastoise-ex|hydro bombard");
    }

    @Override
    public void apply(AttackContext ctx) {
        if (!HYDRO_BOMBARD.equals(ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase() : "")) return;

        String attackerId    = ctx.getAction().getPlayerId();
        PlayerState opponent = ctx.getBoardState().getOpponentState(attackerId);

        List<BenchPokemon> bench = opponent.getBench();
        if (bench == null || bench.isEmpty()) return;

        // Apply 30 damage (3 counters) to up to 2 Benched Pokémon.
        // Per the rulebook, Weakness and Resistance do not apply to Benched Pokémon.
        int targets = Math.min(bench.size(), MAX_BENCH_TARGETS);
        for (int i = 0; i < targets; i++) {
            BenchPokemon target = bench.get(i);
            target.setDamageCounters(target.getDamageCounters() + BENCH_DAMAGE_COUNTERS);
        }
    }
}