package com.pokemon.tcg.controller.dto.response;

import com.pokemon.tcg.domain.model.game.GameState;
import com.pokemon.tcg.domain.model.game.TurnFlags;
import com.pokemon.tcg.domain.model.game.TurnPhase;

public record PublicBoardStateDTO(
        String gameId,
        GameState gameState,
        TurnPhase turnPhase,
        String currentPlayerId,
        int turnNumber,
        String activeStadiumCardId,
        TurnFlags turnFlags,
        boolean bonusDrawPending,
        PublicPlayerStateDTO player1State,
        PublicPlayerStateDTO player2State
) {}
