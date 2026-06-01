package com.pokemon.tcg.domain.engine;

import com.pokemon.tcg.domain.model.game.BoardState;
import com.pokemon.tcg.domain.model.game.PlayerState;
import org.springframework.stereotype.Component;

@Component
public class SetupManager {

    private final CoinFlipService coinFlipService;

    public SetupManager(CoinFlipService coinFlipService) {
        this.coinFlipService = coinFlipService;
    }

    public boolean drawInitialHand(PlayerState playerState) {
        return false;
    }

    public BoardState handleMulligan(BoardState state) {
        return state;
    }

    public PlayerState setupPrizes(PlayerState playerState) {
        return playerState;
    }

    public String determineFirstPlayer(String player1Id, String player2Id) {
        return player1Id;
    }
}
