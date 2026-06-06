package com.pokemon.tcg.application;

import com.pokemon.tcg.api.dto.DeckValidationResult;
import com.pokemon.tcg.domain.model.card.Card;
import com.pokemon.tcg.domain.model.deck.Deck;
import com.pokemon.tcg.domain.model.deck.DeckCard;
import com.pokemon.tcg.domain.model.player.Player;
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
        this.deckRepository   = deckRepository;
        this.cardRepository   = cardRepository;
        this.playerRepository = playerRepository;
    }

    public Deck createDeck(UUID playerId, String name, String description) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));

        Deck deck = Deck.builder()
                .player(player)
                .name(name)
                .description(description)
                .build();

        return deckRepository.save(deck);
    }

    public Deck addCard(UUID deckId, UUID playerId, String cardId, int quantity) {
        Deck deck = deckRepository.findWithCardsById(deckId)
                .orElseThrow(() -> new IllegalArgumentException("Deck not found"));

        if (!deck.getPlayer().getId().equals(playerId)) {
            throw new IllegalArgumentException("Deck does not belong to player");
        }

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));

        deck.getCards().stream()
                .filter(dc -> dc.getCard().getId().equals(cardId))
                .findFirst()
                .ifPresentOrElse(
                        dc -> dc.setQuantity(quantity),
                        () -> deck.getCards().add(DeckCard.builder()
                                .deck(deck)
                                .card(card)
                                .quantity(quantity)
                                .build())
                );

        deck.setValid(isValidDeck(deck));
        return deckRepository.save(deck);
    }

    public DeckValidationResult validate(UUID deckId) {
        Deck deck = deckRepository.findWithCardsById(deckId)
                .orElseThrow(() -> new IllegalArgumentException("Deck not found"));

        return buildValidationResult(deck);
    }

    public List<Deck> listByPlayer(UUID playerId) {
        return deckRepository.findByPlayerIdOrderByCreatedAtDesc(playerId);
    }

    public Deck updateCardQuantity(UUID deckId, UUID playerId, String cardId, int quantity) {
        Deck deck = deckRepository.findWithCardsById(deckId)
                .orElseThrow(() -> new IllegalArgumentException("Deck not found"));

        if (!deck.getPlayer().getId().equals(playerId)) {
            throw new IllegalArgumentException("Deck does not belong to player");
        }

        DeckCard deckCard = deck.getCards().stream()
                .filter(dc -> dc.getCard().getId().equals(cardId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Card not found in deck"));

        deckCard.setQuantity(quantity);
        deck.setValid(isValidDeck(deck));
        return deckRepository.save(deck);
    }

    public void deleteDeck(UUID deckId, UUID playerId) {
        Deck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new IllegalArgumentException("Deck not found"));

        if (!deck.getPlayer().getId().equals(playerId)) {
            throw new IllegalArgumentException("Deck does not belong to player");
        }

        deckRepository.delete(deck);
    }

    public Deck removeCard(UUID deckId, UUID playerId, String cardId) {
        Deck deck = deckRepository.findWithCardsById(deckId)
                .orElseThrow(() -> new IllegalArgumentException("Deck not found"));

        if (!deck.getPlayer().getId().equals(playerId)) {
            throw new IllegalArgumentException("Deck does not belong to player");
        }

        deck.getCards().removeIf(dc -> dc.getCard().getId().equals(cardId));
        deck.setValid(isValidDeck(deck));
        return deckRepository.save(deck);
    }

    private boolean isValidDeck(Deck deck) {
        int total = deck.getTotalCardCount();
        if (total != 60) return false;

        // max 4 copies of same name (except basic energy)
        java.util.Map<String, Integer> nameCounts = new java.util.HashMap<>();
        for (DeckCard dc : deck.getCards()) {
            Card card = dc.getCard();
            if (!card.isBasicEnergy()) {
                nameCounts.merge(card.getName(), dc.getQuantity(), Integer::sum);
            }
        }
        if (nameCounts.values().stream().anyMatch(count -> count > 4)) return false;

        // at least 1 basic pokemon
        boolean hasBasic = deck.getCards().stream()
                .anyMatch(dc -> dc.getCard().isPokemon()
                        && dc.getCard().getSubtypes().contains("Basic"));

        return hasBasic;
    }

    private DeckValidationResult buildValidationResult(Deck deck) {
        int total = deck.getTotalCardCount();
        boolean exactly60 = total == 60;

        java.util.Map<String, Integer> nameCounts = new java.util.HashMap<>();
        for (DeckCard dc : deck.getCards()) {
            Card card = dc.getCard();
            if (!card.isBasicEnergy()) {
                nameCounts.merge(card.getName(), dc.getQuantity(), Integer::sum);
            }
        }
        boolean noExcessCopies = nameCounts.values().stream().noneMatch(count -> count > 4);

        boolean hasBasic = deck.getCards().stream()
                .anyMatch(dc -> dc.getCard().isPokemon()
                        && dc.getCard().getSubtypes().contains("Basic"));

        boolean valid = exactly60 && noExcessCopies && hasBasic;
        deck.setValid(valid);
        deckRepository.save(deck);

        return new DeckValidationResult(valid, total, exactly60, noExcessCopies, hasBasic);
    }
}