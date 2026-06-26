package com.pokemon.tcg.fixtures;

import com.pokemon.tcg.domain.model.game.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Factory class providing static builder methods for domain objects used in unit tests.
 *
 * <p>All methods return fully initialized objects with sensible defaults.
 * Use the provided parameters to customize only the fields relevant to each test.
 *
 * <p>Conventions:
 * <ul>
 *   <li>Player IDs follow the pattern "player-1", "player-2".</li>
 *   <li>Card IDs follow the pattern "xy1-N" (e.g. "xy1-3", "xy1-132").</li>
 *   <li>Instance IDs are random UUIDs generated at call time.</li>
 * </ul>
 */
public final class TestDataBuilder {

    private TestDataBuilder() {
        // Utility class — not instantiable
    }

    // ── IDs ───────────────────────────────────────────────────────────────────

    public static final String PLAYER_1 = "player-1";
    public static final String PLAYER_2 = "player-2";
    public static final String GAME_ID  = "game-1";

    // ── GameAction ─────────────────────────────────────────────────────────────

    /**
     * Builds a GameAction with the given type, player, and payload entries.
     *
     * @param type     the action type
     * @param playerId the acting player's ID
     * @param payload  alternating key-value pairs (key, value, key, value, ...)
     */
    public static GameAction action(GameActionType type, String playerId, Object... payload) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < payload.length - 1; i += 2) {
            map.put((String) payload[i], payload[i + 1]);
        }
        return GameAction.builder()
                .type(type)
                .playerId(playerId)
                .payload(map)
                .build();
    }

    // ── PlayerState ────────────────────────────────────────────────────────────

    /**
     * Builds a minimal PlayerState with the given hand and deck contents.
     * Discard, prizes, bench and active Pokémon are initialized to empty lists/null.
     */
    public static PlayerState playerState(String playerId,
                                          List<String> hand,
                                          List<String> deck) {
        return PlayerState.builder()
                .playerId(playerId)
                .hand(new ArrayList<>(hand))
                .deck(new ArrayList<>(deck))
                .discard(new ArrayList<>())
                .prizes(new ArrayList<>())
                .bench(new ArrayList<>())
                .activePokemon(null)
                .build();
    }

    // ── BoardState ─────────────────────────────────────────────────────────────

    /**
     * Builds a BoardState in the ACTIVE / MAIN phase with fresh TurnFlags.
     * Uses PLAYER_1 as the current player.
     */
    public static BoardState boardState(PlayerState player1State,
                                        PlayerState player2State) {
        return BoardState.builder()
                .gameId(GAME_ID)
                .gameState(GameState.ACTIVE)
                .turnPhase(TurnPhase.MAIN)
                .currentPlayerId(PLAYER_1)
                .turnNumber(1)
                .turnFlags(TurnFlags.fresh())
                .pendingEvents(new ArrayList<>())
                .player1State(player1State)
                .player2State(player2State)
                .build();
    }

    // ── Card ID helpers ────────────────────────────────────────────────────────

    /** Returns a list of N generic card IDs for use as deck/hand filler. */
    public static List<String> cardIds(int count) {
        List<String> ids = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            ids.add("xy1-" + i);
        }
        return ids;
    }

    /** Returns a random instance ID simulating a Pokémon in play. */
    public static String instanceId() {
        return UUID.randomUUID().toString();
    }
}
