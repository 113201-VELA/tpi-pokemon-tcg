package com.pokemon.tcg.domain.model.game;

import com.pokemon.tcg.domain.model.player.Player;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "game_states")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameStateSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(nullable = false)
    private int turnNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TurnPhase turnPhase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_player_id", nullable = false)
    private Player currentPlayer;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private BoardState boardState;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
