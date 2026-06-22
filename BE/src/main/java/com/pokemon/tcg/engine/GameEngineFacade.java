package com.pokemon.tcg.engine;

import com.pokemon.tcg.domain.model.game.BoardState;
import com.pokemon.tcg.domain.model.game.EngineResult;
import com.pokemon.tcg.domain.model.game.GameAction;
import com.pokemon.tcg.domain.model.game.PlayerState;

public interface GameEngineFacade {

    EngineResult processAction(BoardState currentState, GameAction action);

    EngineResult initializeGame(String gameId, PlayerState player1, PlayerState player2);
}
