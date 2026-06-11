package com.pokemon.tcg.infrastructure.repository;

import com.pokemon.tcg.domain.model.player.PlayerMatchup;
import com.pokemon.tcg.domain.model.player.PlayerMatchupId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayerMatchupRepository extends JpaRepository<PlayerMatchup, PlayerMatchupId> {

    List<PlayerMatchup> findByPlayerId(UUID playerId);

    Optional<PlayerMatchup> findByPlayerIdAndOpponentId(UUID playerId, UUID opponentId);
}
