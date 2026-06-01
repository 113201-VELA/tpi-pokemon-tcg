package com.pokemon.tcg.infrastructure.repository;

import com.pokemon.tcg.domain.model.game.Game;
import com.pokemon.tcg.domain.model.game.GameState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameRepository extends JpaRepository<Game, UUID> {

    List<Game> findByStateOrderByCreatedAtDesc(GameState state);

    Optional<Game> findByIdAndState(UUID gameId, GameState state);
}
