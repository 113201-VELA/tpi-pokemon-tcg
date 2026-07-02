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
     */
    public boolean hasBasicPokemonInHand(PlayerState playerState) {
        return hasBasicPokemon(playerState.getHand());
    }

    /**
     * Handles a mulligan for the given player:
     * - Shuffles the hand back into the deck and redraws 7 cards
     * - Increments a raw mulligan counter on the player
     * - After the mulligan, recalculates net bonus draws for both players:
     *   if both have mulliganed, the counters cancel out (net = abs difference,
     *   awarded to the player with fewer mulligans).
     *
     * <p>Per the rulebook: if both players mulligan simultaneously in the same
     * round, neither receives a bonus. The bonus is only the net difference
     * across all mulligan rounds.
     */
    public BoardState handleMulligan(BoardState state, String playerId) {
        PlayerState ps       = state.getStateFor(playerId);
        PlayerState opponent = state.getOpponentState(playerId);

        // Shuffle hand back into deck and redraw
        List<String> deck = new ArrayList<>(ps.getDeck());
        if (ps.getHand() != null) {
            deck.addAll(ps.getHand());
        }
        ps.setHand(new ArrayList<>());
        ps.setDeck(deck);
        shuffleDeck(ps);
        drawInitialHand(ps);

        // Increment raw mulligan counter on this player
        ps.setTotalMulligans(ps.getTotalMulligans() + 1);

        // Recalculate net bonus draws for both players based on total mulligans
        // Net bonus = max(0, opponentTotalMulligans - myTotalMulligans)
        int p1Total = state.getPlayer1State().getTotalMulligans();
        int p2Total = state.getPlayer2State().getTotalMulligans();

        state.getPlayer1State().setMulliganBonusDraws(Math.max(0, p2Total - p1Total));
        state.getPlayer2State().setMulliganBonusDraws(Math.max(0, p1Total - p2Total));

        return state;
    }

    /**
     * Awards bonus cards to a player based on their choice.
     * The player chooses how many cards to draw (0 to mulliganBonusDraws).
     * Resets the bonus counter after drawing regardless of how many were taken.
     */
    public void applyMulliganBonusDraws(PlayerState playerState, int cardsToDraw) {
        int bonus = playerState.getMulliganBonusDraws();
        if (bonus <= 0 || cardsToDraw <= 0) {
            playerState.setMulliganBonusDraws(0);
            return;
        }

        int actualDraw = Math.min(cardsToDraw, Math.min(bonus, playerState.getDeck().size()));

        List<String> deck = new ArrayList<>(playerState.getDeck());
        List<String> hand = new ArrayList<>(playerState.getHand());

        for (int i = 0; i < actualDraw; i++) {
            hand.add(deck.remove(0));
        }

        playerState.setDeck(deck);
        playerState.setHand(hand);
        playerState.setMulliganBonusDraws(0);
    }

    /**
     * Handles CONFIRM_BONUS_PLACEMENT.
     * Removes the player from pendingBonusPlacement and checks if all are done.
     */
    public BoardState handleConfirmBonusPlacement(BoardState state, GameAction action) {
        Set<String> pendingPlacement = new HashSet<>(
                state.getPendingBonusPlacement() != null
                        ? state.getPendingBonusPlacement() : new HashSet<>());

        pendingPlacement.remove(action.getPlayerId());

        return checkBonusResolution(state, pendingPlacement);
    }

    public BoardState checkBonusResolution(BoardState state,
                                            Set<String> pendingPlacement) {
        if (!pendingPlacement.isEmpty()) {
            return state.toBuilder()
                    .pendingBonusPlacement(pendingPlacement)
                    .build();
        }

        if (state.hasAnyPendingBonus()) {
            return state.toBuilder()
                    .pendingBonusPlacement(pendingPlacement)
                    .build();
        }

        return state.toBuilder()
                .bonusDrawPending(false)
                .pendingBonusPlacement(new HashSet<>())
                .gameState(GameState.ACTIVE)
                .turnPhase(TurnPhase.DRAW)
                .build();
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