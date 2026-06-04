package com.pokemon.tcg.api.rest;

import com.pokemon.tcg.api.dto.AddCardRequest;
import com.pokemon.tcg.api.dto.CreateDeckRequest;
import com.pokemon.tcg.api.dto.DeckValidationResult;
import com.pokemon.tcg.api.dto.UpdateDeckRequest;
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

    @GetMapping
    public ResponseEntity<List<Deck>> listDecks(@AuthenticationPrincipal Player player) {
        return ResponseEntity.ok(deckService.listByPlayer(player.getId()));
    }

    @PostMapping
    public ResponseEntity<Deck> createDeck(@AuthenticationPrincipal Player player,
                                           @Valid @RequestBody CreateDeckRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(deckService.createDeck(player.getId(), request.getName(), request.getDescription()));
    }

    @PostMapping("/{deckId}/cards")
    public ResponseEntity<Deck> addCard(@AuthenticationPrincipal Player player,
                                        @PathVariable UUID deckId,
                                        @Valid @RequestBody AddCardRequest request) {
        return ResponseEntity.ok(deckService.addCard(deckId, player.getId(), request.cardId(), request.quantity()));
    }

    @DeleteMapping("/{deckId}")
    public ResponseEntity<Void> deleteDeck(@AuthenticationPrincipal Player player,
                                           @PathVariable UUID deckId) {
        deckService.deleteDeck(deckId, player.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{deckId}/validate")
    public ResponseEntity<DeckValidationResult> validate(@PathVariable UUID deckId) {
        return ResponseEntity.ok(deckService.validate(deckId));
    }
}