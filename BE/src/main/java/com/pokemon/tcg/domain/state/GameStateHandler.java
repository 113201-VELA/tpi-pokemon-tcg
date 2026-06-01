package com.pokemon.tcg.domain.state;

import com.pokemon.tcg.domain.model.game.BoardState;
import com.pokemon.tcg.domain.model.game.EngineResult;
import com.pokemon.tcg.domain.model.game.GameAction;
import com.pokemon.tcg.domain.model.game.GameState;

public interface GameStateHandler {

    EngineResult handle(BoardState state, GameAction action);

    GameState getState();
}
