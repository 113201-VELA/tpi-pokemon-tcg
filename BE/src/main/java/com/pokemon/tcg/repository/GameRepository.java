package com.pokemon.tcg.repository;

import com.pokemon.tcg.domain.model.game.Game;
import com.pokemon.tcg.domain.model.game.GameState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameRepository extends JpaRepository<Game, UUID> {

    @Query("SELECT DISTINCT g FROM Game g " +
           "LEFT JOIN FETCH g.players gp " +
           "LEFT JOIN FETCH gp.player " +
           "WHERE g.state = :state " +
           "ORDER BY g.createdAt DESC")
    List<Game> findByStateWithPlayersOrderByCreatedAtDesc(@Param("state") GameState state);

    Optional<Game> findByIdAndState(UUID gameId, GameState state);

    /**
     * Returns the most recent active game for a given player.
     * Active means the game is in WAITING, SETUP, or ACTIVE state.
     * A player can participate as player 1 or player 2.
     */
    @Query("""
        SELECT DISTINCT g FROM Game g
        LEFT JOIN FETCH g.players gp
        LEFT JOIN FETCH gp.player
        WHERE g.state IN (:states)
          AND EXISTS (
            SELECT 1 FROM GamePlayer p
            WHERE p.game = g
              AND p.player.id = :playerId
          )
        ORDER BY g.createdAt DESC
        """)
    List<Game> findActiveGamesByPlayerId(
            @Param("playerId") UUID playerId,
            @Param("states") List<GameState> states);

    /** Returns a game with its players and their decks eagerly loaded. */
    @Query("SELECT DISTINCT g FROM Game g " +
           "LEFT JOIN FETCH g.players gp " +
           "LEFT JOIN FETCH gp.player " +
           "LEFT JOIN FETCH gp.deck " +
           "WHERE g.id = :gameId")
    Optional<Game> findByIdWithPlayersAndDecks(@Param("gameId") UUID gameId);
}
