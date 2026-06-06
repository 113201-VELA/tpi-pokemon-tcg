package com.pokemon.tcg.api.rest;

import com.pokemon.tcg.api.dto.AddCardRequest;
import com.pokemon.tcg.api.dto.CreateDeckRequest;
import com.pokemon.tcg.api.dto.DeckValidationResult;
import com.pokemon.tcg.api.dto.UpdateCardQuantityRequest;
import com.pokemon.tcg.application.DeckService;
import com.pokemon.tcg.domain.model.deck.Deck;
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
    public ResponseEntity<List<Deck>> listDecks(@AuthenticationPrincipal Player player) {
        return ResponseEntity.ok(deckService.listByPlayer(player.getId()));
    }

    /** Creates a new empty deck for the authenticated player. */
    @PostMapping
    public ResponseEntity<Deck> createDeck(@AuthenticationPrincipal Player player,
                                           @Valid @RequestBody CreateDeckRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(deckService.createDeck(player.getId(), request.getName(), request.getDescription()));
    }

    /** Adds a new card to the deck. Fails if the card is already present — use PUT to update quantity. */
    @PostMapping("/{deckId}/cards")
    public ResponseEntity<Deck> addCard(@AuthenticationPrincipal Player player,
                                        @PathVariable UUID deckId,
                                        @Valid @RequestBody AddCardRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(deckService.addCard(deckId, player.getId(), request.cardId(), request.quantity()));
    }

    /** Updates the quantity of an existing card in the deck. */
    @PutMapping("/{deckId}/cards/{cardId}")
    public ResponseEntity<Deck> updateCardQuantity(@AuthenticationPrincipal Player player,
                                                   @PathVariable UUID deckId,
                                                   @PathVariable String cardId,
                                                   @Valid @RequestBody UpdateCardQuantityRequest request) {
        return ResponseEntity.ok(deckService.updateCardQuantity(deckId, player.getId(), cardId, request.quantity()));
    }

    /** Removes a card completely from the deck. */
    @DeleteMapping("/{deckId}/cards/{cardId}")
    public ResponseEntity<Deck> removeCard(@AuthenticationPrincipal Player player,
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