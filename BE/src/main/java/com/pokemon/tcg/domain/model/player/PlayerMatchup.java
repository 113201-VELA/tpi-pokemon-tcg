package com.pokemon.tcg.domain.model.player;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "player_matchups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(PlayerMatchupId.class)
public class PlayerMatchup {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opponent_id", nullable = false)
    private Player opponent;

    @Column(nullable = false)
    @Builder.Default
    private int wins = 0;

    @Column(nullable = false)
    @Builder.Default
    private int losses = 0;
}
