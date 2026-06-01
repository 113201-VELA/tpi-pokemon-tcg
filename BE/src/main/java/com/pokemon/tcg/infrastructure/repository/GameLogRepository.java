package com.pokemon.tcg.infrastructure.repository;

import com.pokemon.tcg.domain.model.game.GameLogEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GameLogRepository extends JpaRepository<GameLogEntry, UUID> {

    List<GameLogEntry> findByGameIdOrderByCreatedAtAsc(UUID gameId);

    List<GameLogEntry> findByGameIdAndTurnNumberOrderByCreatedAtAsc(
        UUID gameId, int turnNumber);
}
