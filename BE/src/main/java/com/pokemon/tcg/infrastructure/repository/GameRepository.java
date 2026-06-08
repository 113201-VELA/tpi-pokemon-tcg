package com.pokemon.tcg.infrastructure.repository;

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
}
