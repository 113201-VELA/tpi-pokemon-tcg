package com.pokemon.tcg.api.rest;

import com.pokemon.tcg.api.dto.CreateGameRequest;
import com.pokemon.tcg.api.dto.JoinGameRequest;
import com.pokemon.tcg.application.GameService;
import com.pokemon.tcg.domain.model.game.BoardState;
import com.pokemon.tcg.domain.model.game.Game;
import com.pokemon.tcg.domain.model.game.GameLogEntry;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping
    public ResponseEntity<List<Game>> listOpenGames() {
        return ResponseEntity.ok(gameService.listOpenGames());
    }

    @PostMapping
    public ResponseEntity<Game> createGame(@RequestBody CreateGameRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/{gameId}/join")
    public ResponseEntity<Game> joinGame(@PathVariable UUID gameId,
                                          @RequestBody JoinGameRequest request) {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{gameId}/state")
    public ResponseEntity<BoardState> getState(@PathVariable UUID gameId) {
        return ResponseEntity.ok(gameService.getCurrentState(gameId));
    }

    @GetMapping("/{gameId}/log")
    public ResponseEntity<List<GameLogEntry>> getLog(@PathVariable UUID gameId) {
        return ResponseEntity.ok(gameService.getLog(gameId));
    }
}
