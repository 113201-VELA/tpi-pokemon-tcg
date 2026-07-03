package com.pokemon.tcg.engine.attack.steps;

import com.pokemon.tcg.engine.CoinFlipService;
import com.pokemon.tcg.engine.attack.AttackChain;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.engine.attack.AttackStep;
import com.pokemon.tcg.domain.model.game.ActivePokemon;
import com.pokemon.tcg.domain.model.game.CoinResult;
import com.pokemon.tcg.domain.model.game.GameEvent;
import com.pokemon.tcg.domain.model.game.GameEventType;
import com.pokemon.tcg.domain.model.game.SpecialCondition;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/**
 * Step 2 — If the attacking Pokémon is Confused, flips a coin.
 * Tails: attack fails and 3 damage counters are placed on the attacker.
 * Heads: attack proceeds normally.
 *
 * <p>Independently, if the attacking Pokémon has a pending attack-fail
 * chance set by a previous opponent's attack (e.g. Malamar's Mental Panic),
 * flips a coin. Tails: attack fails, no extra damage. The flag is consumed
 * either way. This check is independent from Confusion — a Pokémon could
 * be affected by both in the same turn.
 */
@Component
public class ConfusionCheckStep implements AttackStep {

    private final CoinFlipService coinFlipService;

    public ConfusionCheckStep(CoinFlipService coinFlipService) {
        this.coinFlipService = coinFlipService;
    }

    @Override
    public void execute(AttackContext ctx, AttackChain chain) {
        ActivePokemon attacker = ctx.getBoardState()
                .getStateFor(ctx.getAction().getPlayerId())
                .getActivePokemon();

        if (attacker != null && attacker.hasCondition(SpecialCondition.CONFUSED)) {
            CoinResult flip = coinFlipService.flip();

            ctx.addEvent(GameEvent.builder()
                    .type(GameEventType.COIN_FLIP)
                    .gameId(ctx.getBoardState().getGameId())
                    .playerId(ctx.getAction().getPlayerId())
                    .turnNumber(ctx.getBoardState().getTurnNumber())
                    .data(Map.of("result", flip.name()))
                    .occurredAt(Instant.now())
                    .build());

            if (flip == CoinResult.TAILS) {
                // Apply 3 damage counters to attacker (30 damage)
                attacker.setDamageCounters(attacker.getDamageCounters() + 3);
                ctx.setConfusionSelfDamage(true);
                ctx.setDamageToApply(30);
                // Do NOT cancel — continue pipeline so PostDamageEffectStep
                // can check if the attacker KO'd themselves
                chain.next(ctx);
                return;
            }
        }

        if (attacker != null && attacker.isPendingAttackFailChance()) {
            attacker.setPendingAttackFailChance(false); // consumed regardless of outcome

            CoinResult flip = coinFlipService.flip();

            ctx.addEvent(GameEvent.builder()
                    .type(GameEventType.COIN_FLIP)
                    .gameId(ctx.getBoardState().getGameId())
                    .playerId(ctx.getAction().getPlayerId())
                    .turnNumber(ctx.getBoardState().getTurnNumber())
                    .data(Map.of("result", flip.name()))
                    .occurredAt(Instant.now())
                    .build());

            if (flip == CoinResult.TAILS) {
                ctx.cancel("Mental Panic — attack failed (tails)");
                return;
            }
        }

        chain.next(ctx);
    }
}