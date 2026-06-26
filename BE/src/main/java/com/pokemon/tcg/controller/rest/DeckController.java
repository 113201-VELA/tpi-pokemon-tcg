package com.pokemon.tcg.controller.rest;

import com.pokemon.tcg.controller.dto.request.AddCardRequest;
import com.pokemon.tcg.controller.dto.request.CreateDeckRequest;
import com.pokemon.tcg.controller.dto.request.UpdateCardQuantityRequest;
import com.pokemon.tcg.controller.dto.response.DeckResponseDTO;
import com.pokemon.tcg.controller.dto.response.DeckValidationResult;
import com.pokemon.tcg.controller.dto.request.UpdateDeckRequest;
import com.pokemon.tcg.service.DeckService;
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

    /** Returns all decks belonging to the authenticated player. */
    @GetMapping
    public ResponseEntity<List<DeckResponseDTO>> listDecks(@AuthenticationPrincipal Player player) {
        return ResponseEntity.ok(deckService.listByPlayer(player.getId()));
    }

    /** Creates a new empty deck for the authenticated player. */
    @PostMapping
    public ResponseEntity<DeckResponseDTO> createDeck(@AuthenticationPrincipal Player player,
                                                       @Valid @RequestBody CreateDeckRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(deckService.createDeck(player.getId(), request.getName()));
    }

    /** Updates the name and/or description of an existing deck. */
    @PutMapping("/{deckId}")
    public ResponseEntity<DeckResponseDTO> updateDeck(@AuthenticationPrincipal Player player,
                                                      @PathVariable UUID deckId,
                                                      @Valid @RequestBody UpdateDeckRequest request) {
        return ResponseEntity.ok(deckService.updateDeck(deckId, player.getId(), request.getName(), request.getCardBack(), request.getCoin(), request.getFeaturedCardId()));
    }

    /** Adds a new card to the deck. Fails if the card is already present — use PUT to update quantity. */
    @PostMapping("/{deckId}/cards")
    public ResponseEntity<DeckResponseDTO> addCard(@AuthenticationPrincipal Player player,
                                                    @PathVariable UUID deckId,
                                                    @Valid @RequestBody AddCardRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(deckService.addCard(deckId, player.getId(), request.cardId(), request.quantity()));
    }

    /** Updates the quantity of an existing card in the deck. */
    @PutMapping("/{deckId}/cards/{cardId}")
    public ResponseEntity<DeckResponseDTO> updateCardQuantity(@AuthenticationPrincipal Player player,
                                                               @PathVariable UUID deckId,
                                                               @PathVariable String cardId,
                                                               @Valid @RequestBody UpdateCardQuantityRequest request) {
        return ResponseEntity.ok(deckService.updateCardQuantity(deckId, player.getId(), cardId, request.quantity()));
    }

    /** Removes a card completely from the deck. */
    @DeleteMapping("/{deckId}/cards/{cardId}")
    public ResponseEntity<DeckResponseDTO> removeCard(@AuthenticationPrincipal Player player,
                                                       @PathVariable UUID deckId,
                                                       @PathVariable String cardId) {
        return ResponseEntity.ok(deckService.removeCard(deckId, player.getId(), cardId));
    }

    /** Validates the deck against all construction rules (60 cards, max 4 copies, at least 1 Basic Pokémon). */
    @PostMapping("/{deckId}/validate")
    public ResponseEntity<DeckValidationResult> validate(@PathVariable UUID deckId) {
        return ResponseEntity.ok(deckService.validate(deckId));
    }

    /** Deletes the entire deck. Only the owner can delete it. */
    @DeleteMapping("/{deckId}")
    public ResponseEntity<Void> deleteDeck(@AuthenticationPrincipal Player player,
                                            @PathVariable UUID deckId) {
        deckService.deleteDeck(deckId, player.getId());
        return ResponseEntity.noContent().build();
    }
}
