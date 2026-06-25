package com.pokemon.tcg.engine;

import com.pokemon.tcg.domain.model.game.*;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class SetupManager {

    private final CoinFlipService coinFlipService;
    private final CardLookupPort cardLookupPort;

    public SetupManager(CoinFlipService coinFlipService, CardLookupPort cardLookupPort) {
        this.coinFlipService = coinFlipService;
        this.cardLookupPort  = cardLookupPort;
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
        return hasBasicPokemon(hand);
    }

    /**
     * Separates the first 6 cards from the deck as prize cards.
     */
    public PlayerState setupPrizes(PlayerState playerState) {
        List<String> deck   = new ArrayList<>(playerState.getDeck());
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
     * Checks whether a player's current hand contains at least one Basic Pokémon.
     * Used to determine if a mulligan is required.
     */
    public boolean hasBasicPokemonInHand(PlayerState playerState) {
        return hasBasicPokemon(playerState.getHand());
    }

    /**
     * Handles a mulligan for the given player:
     * - Shuffles the hand back into the deck
     * - Redraws 7 cards
     * - Increments the opponent's mulligan bonus draw counter
     *
     * <p>The opponent's bonus cards are drawn immediately when they confirm
     * setup (SETUP_PLACE_ACTIVE), not here, to keep this method stateless
     * regarding the opponent's actions.
     *
     * <p>Returns the updated BoardState with the mulliganCount incremented
     * for the opponent so the engine knows how many bonus cards to award.
     */
    public BoardState handleMulligan(BoardState state, String playerId) {
        PlayerState ps = state.getStateFor(playerId);

        // Return hand to deck and reshuffle
        List<String> deck = new ArrayList<>(ps.getDeck());
        if (ps.getHand() != null) {
            deck.addAll(ps.getHand());
        }
        ps.setHand(new ArrayList<>());
        ps.setDeck(deck);
        shuffleDeck(ps);

        // Redraw 7 cards
        drawInitialHand(ps);

        // Increment mulligan bonus counter on the opponent's state
        PlayerState opponent = state.getOpponentState(playerId);
        opponent.setMulliganBonusDraws(
                opponent.getMulliganBonusDraws() + 1);

        return state;
    }

    /**
     * Awards bonus cards to a player based on accumulated mulligan draws.
     * Called when a player confirms their setup (SETUP_PLACE_ACTIVE).
     * Resets the counter after drawing.
     */
    public void applyMulliganBonusDraws(PlayerState playerState) {
        int bonus = playerState.getMulliganBonusDraws();
        if (bonus <= 0) return;

        List<String> deck = new ArrayList<>(playerState.getDeck());
        List<String> hand = new ArrayList<>(playerState.getHand());

        int cardsToDraw = Math.min(bonus, deck.size());
        for (int i = 0; i < cardsToDraw; i++) {
            hand.add(deck.remove(0));
        }

        playerState.setDeck(deck);
        playerState.setHand(hand);
        playerState.setMulliganBonusDraws(0);
    }

    // ─── HELPERS ──────────────────────────────────────────────────────────────

    private boolean hasBasicPokemon(List<String> cardIds) {
        if (cardIds == null || cardIds.isEmpty()) return false;
        return cardIds.stream().anyMatch(id -> {
            var card = cardLookupPort.findCardById(id);
            if (card.isEmpty()) return false;
            var subtypes = card.get().getSubtypes();
            return card.get().getSupertype() ==
                    com.pokemon.tcg.domain.model.card.CardType.POKEMON
                    && subtypes != null && subtypes.contains("Basic");
        });
    }
}