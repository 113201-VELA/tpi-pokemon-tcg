// domain/engine/GameEngineFacadeImpl.java
package com.pokemon.tcg.domain.engine;

import com.pokemon.tcg.domain.model.game.BoardState;
import com.pokemon.tcg.domain.model.game.EngineResult;
import com.pokemon.tcg.domain.model.game.GameAction;
import com.pokemon.tcg.domain.model.game.PlayerState;
import org.springframework.stereotype.Component;

@Component
public class GameEngineFacadeImpl implements GameEngineFacade {

    @Override
    public EngineResult processAction(BoardState currentState, GameAction action) {
        // TODO: implementar lógica del motor de juego
        return null;
    }

    @Override
    public EngineResult initializeGame(String gameId, PlayerState player1, PlayerState player2) {
        // TODO: implementar inicialización de partida
        return null;
    }
}