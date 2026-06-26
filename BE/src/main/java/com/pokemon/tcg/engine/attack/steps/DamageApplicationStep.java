package com.pokemon.tcg.engine.attack.steps;

import com.pokemon.tcg.engine.DamageCalculator;
import com.pokemon.tcg.engine.attack.AttackChain;
import com.pokemon.tcg.engine.attack.AttackContext;
import com.pokemon.tcg.engine.attack.AttackStep;
import com.pokemon.tcg.domain.model.game.ActivePokemon;
import com.pokemon.tcg.domain.model.game.GameEvent;
import com.pokemon.tcg.domain.model.game.GameEventType;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;

/**
 * Step 6 — Calculates and applies damage to the defending Pokémon.
 * Uses DamageCalculator which applies weakness, resistance and modifiers.
 */
@Component
public class DamageApplicationStep implements AttackStep {

    private final DamageCalculator damageCalculator;

    public DamageApplicationStep(DamageCalculator damageCalculator) {
        this.damageCalculator = damageCalculator;
    }

    @Override
    public void execute(AttackContext ctx, AttackChain chain) {
        if (ctx.getAttack() == null) {
            chain.next(ctx);
            return;
        }

        int baseDamage = ctx.getAttack().getBaseDamage();
        if (baseDamage <= 0) {
            chain.next(ctx);
            return;
        }

        String attackerId = ctx.getAction().getPlayerId();
        String defenderId = ctx.getBoardState()
                .getOpponentState(attackerId)
                .getPlayerId();

        ActivePokemon attacker = ctx.getBoardState()
                .getStateFor(attackerId).getActivePokemon();
        ActivePokemon defender = ctx.getBoardState()
                .getStateFor(defenderId).getActivePokemon();

        if (defender == null) {
            chain.next(ctx);
            return;
        }

        int finalDamage = damageCalculator.calculate(
                attacker,
                defender,
                baseDamage,
                ctx.getModifiers() != null ? ctx.getModifiers() : new ArrayList<>(),
                ctx.getBoardState().getActiveStadiumCardId()
        );

        int counters = damageCalculator.toCounters(finalDamage);
        defender.setDamageCounters(defender.getDamageCounters() + counters);
        ctx.setDamageToApply(finalDamage);

        ctx.addEvent(GameEvent.builder()
                .type(GameEventType.DAMAGE_APPLIED)
                .gameId(ctx.getBoardState().getGameId())
                .playerId(attackerId)
                .turnNumber(ctx.getBoardState().getTurnNumber())
                .data(Map.of(
                        "damage", finalDamage,
                        "counters", counters,
                        "defenderId", defenderId))
                .occurredAt(Instant.now())
                .build());

        chain.next(ctx);
    }
}