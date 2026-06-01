package com.pokemon.tcg.application;

import com.pokemon.tcg.api.dto.DeckValidationResult;
import com.pokemon.tcg.domain.model.deck.Deck;
import com.pokemon.tcg.infrastructure.repository.CardRepository;
import com.pokemon.tcg.infrastructure.repository.DeckRepository;
import com.pokemon.tcg.infrastructure.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DeckService {

    private final DeckRepository deckRepository;
    private final CardRepository cardRepository;
    private final PlayerRepository playerRepository;

    public DeckService(DeckRepository deckRepository,
                       CardRepository cardRepository,
                       PlayerRepository playerRepository) {
        this.deckRepository  = deckRepository;
        this.cardRepository  = cardRepository;
        this.playerRepository = playerRepository;
    }

    public Deck createDeck(UUID playerId, String name, String description) {
        return null;
    }

    public Deck addCard(UUID deckId, UUID playerId, String cardId, int quantity) {
        return null;
    }

    public DeckValidationResult validate(UUID deckId) {
        return null;
    }

    public List<Deck> listByPlayer(UUID playerId) {
        return deckRepository.findByPlayerIdOrderByCreatedAtDesc(playerId);
    }

    public void deleteDeck(UUID deckId, UUID playerId) {
    }
}
