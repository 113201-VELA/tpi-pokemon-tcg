package com.pokemon.tcg.domain.model.game;

import lombok.*;

import java.util.List;

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
    @Builder.Default
    private boolean bonusDrawPending = false;

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
}