package com.pokemon.tcg.engine.attack.steps;

import com.pokemon.tcg.engine.CoinFlipService;
import com.pokemon.tcg.engine.attack.AttackChain;
import com.pokemon.tcg.engine.attack.AttackContext;
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
                    .type(GameEventType.SPECIAL_CONDITION_APPLIED)
                    .gameId(ctx.getBoardState().getGameId())
                    .playerId(ctx.getAction().getPlayerId())
                    .turnNumber(ctx.getBoardState().getTurnNumber())
                    .data(Map.of("condition", "CONFUSED", "coinResult", flip.name()))
                    .occurredAt(Instant.now())
                    .build());

            if (flip == CoinResult.TAILS) {
                // Attack fails — 3 damage counters on attacker
                attacker.setDamageCounters(attacker.getDamageCounters() + 3);
                ctx.cancel("Confused — attack failed (tails)");
                return;
            }
        }

        chain.next(ctx);
    }
}