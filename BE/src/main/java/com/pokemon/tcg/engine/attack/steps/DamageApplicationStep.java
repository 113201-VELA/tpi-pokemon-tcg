package com.pokemon.tcg.engine.attack.steps;

import com.pokemon.tcg.engine.DamageCalculator;
import com.pokemon.tcg.engine.attack.AttackChain;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.engine.attack.AttackStep;
import com.pokemon.tcg.domain.model.game.ActivePokemon;
import com.pokemon.tcg.domain.model.game.GameEvent;
import com.pokemon.tcg.domain.model.game.GameEventType;
import com.pokemon.tcg.domain.model.game.PokemonEffect;
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

        // Mist Slash and similar attacks ignore weakness, resistance and defender effects
        ActivePokemon attackerForCalc = ctx.isIgnoreDefenderEffects()
                ? stripModifiers(attacker) : attacker;
        ActivePokemon defenderForCalc = ctx.isIgnoreDefenderEffects()
                ? stripModifiers(defender) : defender;

        int finalDamage = damageCalculator.calculate(
                attackerForCalc,
                defenderForCalc,
                baseDamage,
                ctx.getModifiers() != null ? ctx.getModifiers() : new ArrayList<>(),
                ctx.getBoardState().getActiveStadiumCardId()
        );

        if (!ctx.isIgnoreDefenderEffects()) {
            finalDamage = applyActiveEffects(defender, finalDamage);
        }

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

    /**
     * Returns a copy of the Pokémon with empty weaknesses, resistances and
     * active effects, used when an attack ignores all defender modifiers
     * (e.g. Mist Slash).
     */
    private ActivePokemon stripModifiers(ActivePokemon pokemon) {
        return ActivePokemon.builder()
                .instanceId(pokemon.getInstanceId())
                .cardId(pokemon.getCardId())
                .attachedEnergyIds(pokemon.getAttachedEnergyIds())
                .attachedToolId(pokemon.getAttachedToolId())
                .evolutionStack(pokemon.getEvolutionStack())
                .damageCounters(pokemon.getDamageCounters())
                .conditions(pokemon.getConditions())
                .types(pokemon.getTypes())
                .weaknesses(new java.util.ArrayList<>())
                .resistances(new java.util.ArrayList<>())
                .activeEffects(new java.util.ArrayList<>())
                .build();
    }

    private int applyActiveEffects(ActivePokemon defender, int finalDamage) {
        if (defender.getActiveEffects() == null || defender.getActiveEffects().isEmpty()) {
            return finalDamage;
        }
        if (defender.getActiveEffects().contains(PokemonEffect.INVULNERABLE)) {
            return 0;
        }
        if (defender.getActiveEffects().contains(PokemonEffect.HARDEN) && finalDamage <= 60) {
            return 0;
        }
        return finalDamage;
    }
}