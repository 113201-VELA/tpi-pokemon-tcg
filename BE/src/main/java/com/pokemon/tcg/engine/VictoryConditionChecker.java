package com.pokemon.tcg.engine;

import com.pokemon.tcg.domain.model.game.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Checks victory and defeat conditions after each relevant game action.
 *
 * <p>The three win conditions per the rulebook are:
 * <ol>
 *   <li>A player takes their last Prize card.</li>
 *   <li>The opponent has no Pokémon left in play after a KO (no Active, no Bench).</li>
 *   <li>The opponent cannot draw a card at the start of their turn (empty deck).</li>
 * </ol>
 *
 * <p>If both players satisfy a win condition simultaneously, Sudden Death applies.
 * That case is detected here and flagged via a dedicated event type.
 *
 * <p>This checker is intentionally stateless — it only reads the board state
 * and returns an event. No mutations occur here.
 */
@Component
public class VictoryConditionChecker {

    /**
     * Evaluates all win conditions against the current board state.
     * Returns a {@link GameEvent} of type {@code GAME_OVER} if a winner is found,
     * {@code SUDDEN_DEATH_STARTED} if both players win simultaneously,
     * or empty if the game continues.
     */
    public Optional<GameEvent> check(BoardState state) {
        // Victory conditions are not evaluated during setup —
        // players are still placing their Pokémon and empty fields are expected.
        if (state.getTurnPhase() == TurnPhase.SETUP ||
                state.getGameState() == GameState.SETUP) {
            return Optional.empty();
        }

        boolean p1Wins = playerWins(state, state.getPlayer1State(), state.getPlayer2State());
        boolean p2Wins = playerWins(state, state.getPlayer2State(), state.getPlayer1State());

        if (p1Wins && p2Wins) {
            return Optional.of(buildEvent(state, null, "SUDDEN_DEATH",
                    GameEventType.SUDDEN_DEATH_STARTED));
        }
        if (p1Wins) {
            return Optional.of(buildEvent(state,
                    state.getPlayer1State().getPlayerId(), "WIN", GameEventType.GAME_OVER));
        }
        if (p2Wins) {
            return Optional.of(buildEvent(state,
                    state.getPlayer2State().getPlayerId(), "WIN", GameEventType.GAME_OVER));
        }

        return Optional.empty();
    }

    /**
     * Returns true if the given player satisfies at least one win condition.
     *
     * <p>Win condition 1 — prizes: the player has taken all their prize cards
     * (prize list is empty).
     *
     * <p>Win condition 2 — no Pokémon: the opponent has no Active Pokémon
     * and no Bench Pokémon remaining after a KO.
     *
     * <p>Win condition 3 — empty deck: the opponent has no cards left in their
     * deck at the start of their turn. This condition is checked here but is
     * only meaningful when called after a DRAW_CARD action fails.
     */
    private boolean playerWins(BoardState state, PlayerState player, PlayerState opponent) {
        // Condition 1: all prizes taken
        if (player.getPrizes() != null && player.getPrizes().isEmpty()) {
            return true;
        }

        // Condition 2: opponent has no Pokémon left in play
        boolean opponentHasNoPokemon = opponent.getActivePokemon() == null
                && (opponent.getBench() == null || opponent.getBench().isEmpty());
        if (opponentHasNoPokemon) {
            return true;
        }

        // Condition 3: opponent deck is empty (cannot draw)
        boolean opponentCannotDraw = opponent.getDeck() == null || opponent.getDeck().isEmpty();
        if (opponentCannotDraw && state.getCurrentPlayerId().equals(opponent.getPlayerId())) {
            return true;
        }

        return false;
    }

    /**
     * Checks whether a specific Pokémon is knocked out based on its
     * accumulated damage counters and its printed max HP.
     */
    public boolean isKnockedOut(ActivePokemon pokemon, int maxHp) {
        return pokemon.getDamageCounters() * 10 >= maxHp;
    }

    private GameEvent buildEvent(BoardState state, String winnerId,
                                 String reason, GameEventType type) {
        return GameEvent.builder()
                .type(type)
                .gameId(state.getGameId())
                .playerId(winnerId)
                .turnNumber(state.getTurnNumber())
                .data(Map.of(
                        "winnerId", winnerId != null ? winnerId : "none",
                        "reason", reason))
                .occurredAt(Instant.now())
                .build();
    }
}