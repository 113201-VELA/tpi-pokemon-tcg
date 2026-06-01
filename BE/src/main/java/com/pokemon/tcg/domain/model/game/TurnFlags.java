package com.pokemon.tcg.domain.model.game;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TurnFlags {
    private boolean energyAttachedThisTurn;
    private boolean retreatedThisTurn;
    private boolean supporterPlayedThisTurn;
    private boolean stadiumPlayedThisTurn;
    private boolean attackedThisTurn;
    private boolean isFirstTurnOfGame;

    public static TurnFlags fresh() {
        return TurnFlags.builder().build();
    }
}
