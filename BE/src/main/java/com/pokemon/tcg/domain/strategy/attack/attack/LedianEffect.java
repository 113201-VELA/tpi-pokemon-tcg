package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LedianEffect implements AttackEffect {

    private static final int BENCH_DAMAGE_COUNTERS = 1;

    @Override
    public void apply(AttackContext ctx) {
        String attackerId    = ctx.getAction().getPlayerId();
        PlayerState opponent = ctx.getBoardState().getOpponentState(attackerId);

        if (opponent.getBench() == null || opponent.getBench().isEmpty()) return;

        String benchTargetId = ctx.getAction().getPayloadString("benchTargetInstanceId");
        if (benchTargetId == null) return;

        List<BenchPokemon> bench = opponent.getBench();
        bench.stream()
                .filter(b -> b.getInstanceId().equals(benchTargetId))
                .findFirst()
                .ifPresent(b -> b.setDamageCounters(
                        b.getDamageCounters() + BENCH_DAMAGE_COUNTERS));
    }
}
