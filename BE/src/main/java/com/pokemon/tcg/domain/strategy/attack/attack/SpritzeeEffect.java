package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * xy1-92 Spritzee
 *
 * Sweet Scent: heal 20 damage from 1 of your Pokémon (Active or Bench,
 *              no energy discard required).
 * Flop: 20 damage, no additional text — plain attack, no effect needed.
 */
@Component
public class SpritzeeEffect implements AttackEffect {

    private static final String SWEET_SCENT   = "sweet scent";
    private static final int    HEAL_AMOUNT   = 20;
    private static final int    COUNTERS_HEAL = HEAL_AMOUNT / 10;

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("spritzee|sweet scent");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        if (SWEET_SCENT.equals(attackName)) {
            applySweetScent(ctx);
        }
    }

    /**
     * Heal 20 damage (2 counters) from one of the attacker's own Pokémon,
     * Active or Bench, specified via {@code targetInstanceId}. Minimum 0
     * counters.
     */
    private void applySweetScent(AttackContext ctx) {
        String attackerId       = ctx.getAction().getPlayerId();
        PlayerState attacker    = ctx.getBoardState().getStateFor(attackerId);
        String targetInstanceId = ctx.getAction().getPayloadString("targetInstanceId");

        if (targetInstanceId == null) return;

        if (attacker.getActivePokemon() != null
                && attacker.getActivePokemon().getInstanceId().equals(targetInstanceId)) {
            ActivePokemon active = attacker.getActivePokemon();
            active.setDamageCounters(Math.max(0, active.getDamageCounters() - COUNTERS_HEAL));
            return;
        }

        if (attacker.getBench() != null) {
            attacker.getBench().stream()
                    .filter(b -> b.getInstanceId().equals(targetInstanceId))
                    .findFirst()
                    .ifPresent(b -> b.setDamageCounters(
                            Math.max(0, b.getDamageCounters() - COUNTERS_HEAL)));
        }
    }
}