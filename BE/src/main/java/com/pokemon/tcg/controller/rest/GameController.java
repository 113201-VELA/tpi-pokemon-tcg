package com.pokemon.tcg.controller.rest;

import com.pokemon.tcg.controller.dto.request.CreateGameRequest;
import com.pokemon.tcg.controller.dto.request.JoinGameRequest;
import com.pokemon.tcg.controller.dto.response.GameLogResponseDTO;
import com.pokemon.tcg.controller.dto.response.GameResponseDTO;
import com.pokemon.tcg.controller.dto.response.GameStateResponseDTO;
import com.pokemon.tcg.service.GameService;
import com.pokemon.tcg.domain.model.game.Game;
import com.pokemon.tcg.domain.model.player.Player;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    /** Returns all games currently waiting for a second player to join. */
    @GetMapping
    public ResponseEntity<List<GameResponseDTO>> listOpenGames() {
        return ResponseEntity.ok(gameService.listOpenGames());
    }

    /** Creates a new game in WAITING state with the authenticated player as player 1. */
    @PostMapping
    public ResponseEntity<Game> createGame(@AuthenticationPrincipal Player player,
                                            @RequestBody CreateGameRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(gameService.createGame(player.getId(), request.getDeckId()));
    }

    /** Joins an existing WAITING game as player 2, triggering game initialization. */
    @PostMapping("/{gameId}/join")
    public ResponseEntity<Game> joinGame(@AuthenticationPrincipal Player player,
                                          @PathVariable UUID gameId,
                                          @RequestBody JoinGameRequest request) {
        return ResponseEntity.ok(gameService.joinGame(gameId, player.getId(), request.getDeckId()));
    }

    /** Cancels a WAITING game. Only the creator (player 1) can cancel it. */
    @DeleteMapping("/{gameId}")
    public ResponseEntity<Void> cancelGame(@AuthenticationPrincipal Player player,
                                            @PathVariable UUID gameId) {
        gameService.cancelGame(gameId, player.getId());
        return ResponseEntity.noContent().build();
    }

    /** Returns the current board state for the authenticated player, hiding opponent's private data. */
    @GetMapping("/{gameId}/state")
    public ResponseEntity<GameStateResponseDTO> getState(@AuthenticationPrincipal Player player,
                                                          @PathVariable UUID gameId) {
        return ResponseEntity.ok(gameService.getCurrentState(gameId, player.getId()));
    }

    /** Returns the complete action log for the specified game. */
    @GetMapping("/{gameId}/log")
    public ResponseEntity<List<GameLogResponseDTO>> getLog(@PathVariable UUID gameId) {
        return ResponseEntity.ok(gameService.getLog(gameId));
    }
}
