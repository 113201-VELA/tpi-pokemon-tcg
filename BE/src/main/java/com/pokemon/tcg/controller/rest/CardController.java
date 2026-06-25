package com.pokemon.tcg.controller.rest;

import com.pokemon.tcg.controller.dto.response.CardResponseDTO;
import com.pokemon.tcg.service.CardCacheService;
import com.pokemon.tcg.domain.model.card.CardType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardCacheService cardCacheService;

    public CardController(CardCacheService cardCacheService) {
        this.cardCacheService = cardCacheService;
    }

    @GetMapping
    public ResponseEntity<Page<CardResponseDTO>> searchCards(
            @RequestParam(defaultValue = "xy1") String set,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) CardType type,
            Pageable pageable) {
        return ResponseEntity.ok(cardCacheService.searchCards(set, name, type, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CardResponseDTO> getCard(@PathVariable String id) {
        return cardCacheService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
