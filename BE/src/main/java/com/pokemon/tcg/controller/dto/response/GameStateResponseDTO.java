package com.pokemon.tcg.controller.dto.response;

import com.pokemon.tcg.domain.model.game.GameEvent;
import com.pokemon.tcg.domain.model.game.GameState;
import com.pokemon.tcg.domain.model.game.TurnFlags;
import com.pokemon.tcg.domain.model.game.TurnPhase;

import java.util.List;

public record GameStateResponseDTO(
        String gameId,
        GameState gameState,
        TurnPhase turnPhase,
        String currentPlayerId,
        int turnNumber,
        String activeStadiumCardId,
        TurnFlags turnFlags,
        List<GameEvent> pendingEvents,
        OwnPlayerStateResponseDTO ownState,
        OpponentPlayerStateResponseDTO opponentState
) {}
