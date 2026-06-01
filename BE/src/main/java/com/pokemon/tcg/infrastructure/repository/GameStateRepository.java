package com.pokemon.tcg.infrastructure.repository;

import com.pokemon.tcg.domain.model.game.GameStateSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameStateRepository extends JpaRepository<GameStateSnapshot, UUID> {

    Optional<GameStateSnapshot> findTopByGameIdOrderByCreatedAtDesc(UUID gameId);

    List<GameStateSnapshot> findByGameIdOrderByCreatedAtAsc(UUID gameId);
}
