package com.pokemon.tcg.domain.model.player;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class PlayerMatchupId implements Serializable {
    private UUID player;
    private UUID opponent;

    public PlayerMatchupId() {}

    public PlayerMatchupId(UUID player, UUID opponent) {
        this.player = player;
        this.opponent = opponent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerMatchupId other)) return false;
        return Objects.equals(player, other.player) &&
               Objects.equals(opponent, other.opponent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(player, opponent);
    }
}
