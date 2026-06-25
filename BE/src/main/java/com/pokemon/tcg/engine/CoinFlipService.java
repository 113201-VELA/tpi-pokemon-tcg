package com.pokemon.tcg.engine;

import com.pokemon.tcg.domain.model.game.CoinResult;
import org.springframework.stereotype.Component;

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
}
