package com.pokemon.tcg.application;

import com.pokemon.tcg.api.websocket.GameEventPublisher;
import com.pokemon.tcg.domain.engine.GameEngineFacade;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.infrastructure.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class GameService {

    private final GameEngineFacade engine;
    private final GameRepository gameRepository;
    private final GameStateRepository stateRepository;
    private final GameLogRepository logRepository;
    private final GameEventPublisher eventPublisher;
    private final CardRepository cardRepository;

    public GameService(GameEngineFacade engine,
                       GameRepository gameRepository,
                       GameStateRepository stateRepository,
                       GameLogRepository logRepository,
                       GameEventPublisher eventPublisher,
                       CardRepository cardRepository) {
        this.engine           = engine;
        this.gameRepository   = gameRepository;
        this.stateRepository  = stateRepository;
        this.logRepository    = logRepository;
        this.eventPublisher   = eventPublisher;
        this.cardRepository   = cardRepository;
    }

    public Game createGame(UUID playerId, UUID deckId) {
        return null;
    }

    public Game joinGame(UUID gameId, UUID playerId, UUID deckId) {
        return null;
    }

    public EngineResult processAction(UUID gameId, UUID playerId, GameAction action) {
        return null;
    }

    public BoardState getCurrentState(UUID gameId) {
        return null;
    }

    public List<GameLogEntry> getLog(UUID gameId) {
        return logRepository.findByGameIdOrderByCreatedAtAsc(gameId);
    }

    public List<Game> listOpenGames() {
        return gameRepository.findByStateOrderByCreatedAtDesc(GameState.WAITING);
    }
}
