package com.pokemon.tcg.api.rest;

import com.pokemon.tcg.api.dto.request.AddCardRequest;
import com.pokemon.tcg.api.dto.request.CreateDeckRequest;
import com.pokemon.tcg.api.dto.request.UpdateCardQuantityRequest;
import com.pokemon.tcg.api.dto.response.DeckResponseDTO;
import com.pokemon.tcg.api.dto.response.DeckValidationResult;
import com.pokemon.tcg.api.dto.request.UpdateDeckRequest;
import com.pokemon.tcg.application.DeckService;
import com.pokemon.tcg.domain.model.player.Player;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/decks")
public class DeckController {

    private final DeckService deckService;

    public DeckController(DeckService deckService) {
        this.deckService = deckService;
    }

    @GetMapping
    public ResponseEntity<List<DeckResponseDTO>> listDecks(@AuthenticationPrincipal Player player) {
        return ResponseEntity.ok(deckService.listByPlayer(player.getId()));
    }

    @PostMapping
    public ResponseEntity<DeckResponseDTO> createDeck(@AuthenticationPrincipal Player player,
                                                       @Valid @RequestBody CreateDeckRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(deckService.createDeck(player.getId(), request.getName(), request.getDescription()));
    }

    @PutMapping("/{deckId}")
    public ResponseEntity<DeckResponseDTO> updateDeck(@AuthenticationPrincipal Player player,
                                                      @PathVariable UUID deckId,
                                                      @Valid @RequestBody UpdateDeckRequest request) {
        return ResponseEntity.ok(deckService.updateDeck(deckId, player.getId(), request.getName(), request.getDescription()));
    }

    @PostMapping("/{deckId}/cards")
    public ResponseEntity<DeckResponseDTO> addCard(@AuthenticationPrincipal Player player,
                                                    @PathVariable UUID deckId,
                                                    @Valid @RequestBody AddCardRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(deckService.addCard(deckId, player.getId(), request.cardId(), request.quantity()));
    }

    @PutMapping("/{deckId}/cards/{cardId}")
    public ResponseEntity<DeckResponseDTO> updateCardQuantity(@AuthenticationPrincipal Player player,
                                                               @PathVariable UUID deckId,
                                                               @PathVariable String cardId,
                                                               @Valid @RequestBody UpdateCardQuantityRequest request) {
        return ResponseEntity.ok(deckService.updateCardQuantity(deckId, player.getId(), cardId, request.quantity()));
    }

    @DeleteMapping("/{deckId}/cards/{cardId}")
    public ResponseEntity<DeckResponseDTO> removeCard(@AuthenticationPrincipal Player player,
                                                       @PathVariable UUID deckId,
                                                       @PathVariable String cardId) {
        return ResponseEntity.ok(deckService.removeCard(deckId, player.getId(), cardId));
    }

    @PostMapping("/{deckId}/validate")
    public ResponseEntity<DeckValidationResult> validate(@PathVariable UUID deckId) {
        return ResponseEntity.ok(deckService.validate(deckId));
    }

    @DeleteMapping("/{deckId}")
    public ResponseEntity<Void> deleteDeck(@AuthenticationPrincipal Player player,
                                            @PathVariable UUID deckId) {
        deckService.deleteDeck(deckId, player.getId());
        return ResponseEntity.noContent().build();
    }
}
