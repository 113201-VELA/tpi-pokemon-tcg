package com.pokemon.tcg.api.rest;

import com.pokemon.tcg.api.dto.CreateDeckRequest;
import com.pokemon.tcg.api.dto.DeckValidationResult;
import com.pokemon.tcg.api.dto.UpdateDeckRequest;
import com.pokemon.tcg.application.DeckService;
import com.pokemon.tcg.domain.model.deck.Deck;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<List<Deck>> listDecks() {
        return ResponseEntity.ok(List.of());
    }

    @PostMapping
    public ResponseEntity<Deck> createDeck(@RequestBody CreateDeckRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{deckId}")
    public ResponseEntity<Deck> updateDeck(@PathVariable UUID deckId,
                                            @RequestBody UpdateDeckRequest request) {
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{deckId}")
    public ResponseEntity<Void> deleteDeck(@PathVariable UUID deckId) {
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{deckId}/validate")
    public ResponseEntity<DeckValidationResult> validate(@PathVariable UUID deckId) {
        return ResponseEntity.ok(deckService.validate(deckId));
    }
}
