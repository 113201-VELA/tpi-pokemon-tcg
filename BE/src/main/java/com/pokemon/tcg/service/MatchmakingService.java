package com.pokemon.tcg.service;

import com.pokemon.tcg.domain.model.game.Game;
import com.pokemon.tcg.domain.model.game.GameState;
import com.pokemon.tcg.repository.GameRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class MatchmakingService {

    private final GameRepository gameRepository;
    private final GameService gameService;

    public MatchmakingService(GameRepository gameRepository,
                               GameService gameService) {
        this.gameRepository = gameRepository;
        this.gameService    = gameService;
    }

    public Game findAvailableGame() {
        List<Game> openGames = gameRepository.findByStateWithPlayersOrderByCreatedAtDesc(GameState.WAITING);
        return openGames.isEmpty() ? null : openGames.get(0);
    }

    public Game matchPlayer(UUID playerId, UUID deckId) {
        Game available = findAvailableGame();
        if (available != null) {
            return gameService.joinGame(available.getId(), playerId, deckId);
        }
        return gameService.createGame(playerId, deckId);
    }
}
