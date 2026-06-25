package com.pokemon.tcg.engine;

import com.pokemon.tcg.domain.model.game.*;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class SetupManager {

    private final CoinFlipService coinFlipService;

    public SetupManager(CoinFlipService coinFlipService) {
        this.coinFlipService = coinFlipService;
    }

    /**
     * Draws 7 cards from the player's deck into their hand.
     * Returns true if the hand contains at least one Basic Pokémon.
     */
    public boolean drawInitialHand(PlayerState playerState) {
        List<String> deck = new ArrayList<>(playerState.getDeck());
        List<String> hand = new ArrayList<>();

        for (int i = 0; i < 7 && !deck.isEmpty(); i++) {
            hand.add(deck.remove(0));
        }

        playerState.setDeck(deck);
        playerState.setHand(hand);
        return true; // Basic Pokémon check handled by caller with card data
    }

    /**
     * Separates the first 6 cards from the deck as prize cards.
     */
    public PlayerState setupPrizes(PlayerState playerState) {
        List<String> deck = new ArrayList<>(playerState.getDeck());
        List<String> prizes = new ArrayList<>();

        for (int i = 0; i < 6 && !deck.isEmpty(); i++) {
            prizes.add(deck.remove(0));
        }

        playerState.setDeck(deck);
        playerState.setPrizes(prizes);
        return playerState;
    }

    /**
     * Randomly determines which player goes first using a coin flip.
     */
    public String determineFirstPlayer(String player1Id, String player2Id) {
        return coinFlipService.flip() == CoinResult.HEADS ? player1Id : player2Id;
    }

    /**
     * Shuffles the player's deck randomly.
     */
    public void shuffleDeck(PlayerState playerState) {
        List<String> deck = new ArrayList<>(playerState.getDeck());
        Collections.shuffle(deck);
        playerState.setDeck(deck);
    }

    /**
     * Handles mulligan: if no Basic Pokémon in hand, shuffle back and redraw.
     */
    public BoardState handleMulligan(BoardState state) {
        return state;
    }
}