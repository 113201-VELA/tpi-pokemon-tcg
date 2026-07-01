package com.pokemon.tcg.engine;

import com.pokemon.tcg.domain.model.game.CoinResult;
import com.pokemon.tcg.domain.model.game.GameEvent;
import com.pokemon.tcg.domain.model.game.GameEventType;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Random;

@Component
public class CoinFlipService {

    private final Random random = new Random();

    public CoinResult flip() {
        return random.nextBoolean() ? CoinResult.HEADS : CoinResult.TAILS;
    }

    public CoinResult flip(boolean forceHeads) {
        return forceHeads ? CoinResult.HEADS : CoinResult.TAILS;
    }

    public CoinResult flipAndEmit(AttackContext ctx, String playerId) {
        CoinResult result = flip();

        GameEvent event = GameEvent.builder()
                .type(GameEventType.COIN_FLIP)
                .gameId(ctx.getBoardState().getGameId())
                .playerId(playerId)
                .turnNumber(ctx.getBoardState().getTurnNumber())
                .data(Map.of("result", result.name()))
                .occurredAt(Instant.now())
                .build();

        ctx.addEvent(event);
        return result;
    }
}
