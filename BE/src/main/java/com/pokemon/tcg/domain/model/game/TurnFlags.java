package com.pokemon.tcg.domain.model.game;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
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