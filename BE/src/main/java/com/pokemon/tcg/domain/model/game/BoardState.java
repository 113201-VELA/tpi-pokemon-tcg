package com.pokemon.tcg.domain.model.game;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class BoardState {
    private String gameId;
    private GameState gameState;
    private TurnPhase turnPhase;
    private String currentPlayerId;
    private int turnNumber;
    private PlayerState player1State;
    private PlayerState player2State;
    private String activeStadiumCardId;
    private TurnFlags turnFlags;
    private List<GameEvent> pendingEvents;

    /**
     * True when both players have placed their Active Pokémon and at least
     * one player has pending mulligan bonus draws to accept or decline.
     * Cleared once all bonus decisions are made and the game transitions to DRAW.
     */
    private boolean bonusDrawPending;

    /**
     * Set of playerIds that are currently in the bonus placement stage.
     * A player enters this stage after accepting > 0 bonus draws.
     * They exit by sending CONFIRM_BONUS_PLACEMENT.
     * When this set is empty and bonusDrawPending is false, the game transitions to ACTIVE.
     */
    @Builder.Default
    private Set<String> pendingBonusPlacement = new HashSet<>();

    /**
     * Set to the defending player's ID when their Active Pokémon was knocked out
     * and they still have Pokémon on the bench to choose from.
     * All actions are blocked until that player sends CHOOSE_BENCH_POKEMON.
     * Null when no bench choice is pending.
     */
    private String pendingBenchChoicePlayerId;

    /**
     * ID of the player who goes first, determined by coin flip during initialization.
     * Used to block attacks on the very first turn of the game.
     */
    private String firstPlayerId;

    /**
     * Set to the player's ID when a card effect (e.g. Great Ball) requires them
     * to select a card from a set of revealed deck cards. All other actions are
     * blocked until that player sends SELECT_FROM_DECK.
     * Null when no deck selection is pending.
     */
    private String pendingDeckSelectionPlayerId;

    /**
     * The card IDs from the top of the deck that are set aside for the player
     * to choose from. Only meaningful when pendingDeckSelectionPlayerId is set.
     */
    private List<String> pendingDeckSelectionCardIds;

    @Builder.Default
    private String pendingForcedSwitchPlayerId = null;

    @Builder.Default
    private String pendingHandDiscardPlayerId = null;

    @Builder.Default
    private int pendingHandDiscardCount = 0;

    @Builder.Default
    private String pendingAttackSelectionKey = null;

    @Builder.Default
    private String pendingAttackSelectionPlayerId = null;

    @Builder.Default
    private int pendingAttackSelectionMaxCards = 1;

    @Builder.Default
    private AttackSelectionType pendingAttackSelectionType = AttackSelectionType.PICK;

    public PlayerState getStateFor(String playerId) {
        if (playerId.equals(player1State.getPlayerId())) return player1State;
        if (playerId.equals(player2State.getPlayerId())) return player2State;
        throw new IllegalArgumentException("Player not found: " + playerId);
    }

    public PlayerState getOpponentState(String playerId) {
        if (playerId.equals(player1State.getPlayerId())) return player2State;
        if (playerId.equals(player2State.getPlayerId())) return player1State;
        throw new IllegalArgumentException("Player not found: " + playerId);
    }

    /**
     * Returns true if any player still has pending mulligan bonus draws
     * that have not been accepted or declined yet.
     */
    public boolean hasAnyPendingBonus() {
        return (player1State != null && player1State.getMulliganBonusDraws() > 0)
                || (player2State != null && player2State.getMulliganBonusDraws() > 0);
    }

    /**
     * Returns true when the defending player's Active Pokémon was knocked out
     * and they must choose a replacement from their bench before play continues.
     */
    public boolean isPendingBenchChoice() {
        return pendingBenchChoicePlayerId != null;
    }

    /**
     * Returns true when a card effect (e.g. Great Ball) requires the player
     * to select a card from a set of revealed deck cards.
     */
    public boolean isPendingDeckSelection() {
        return pendingDeckSelectionPlayerId != null;
    }

    public boolean isPendingForcedSwitch() {
        return pendingForcedSwitchPlayerId != null;
    }

    /**
     * Returns true when a card effect (e.g. Malamar's Mental Trash) requires the
     * given player to choose which cards to discard from their own hand.
     * All actions are blocked for both players until DISCARD_FROM_HAND is sent
     * by pendingHandDiscardPlayerId.
     */
    public boolean isPendingHandDiscard() {
        return pendingHandDiscardPlayerId != null;
    }

    public boolean isPendingAttackSelection() {
        return pendingAttackSelectionPlayerId != null;
    }
}