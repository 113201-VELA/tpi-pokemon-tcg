package com.pokemon.tcg.service;

import com.pokemon.tcg.controller.mapper.DeckMapper;
import com.pokemon.tcg.controller.dto.response.DeckResponseDTO;
import com.pokemon.tcg.controller.dto.response.DeckValidationResult;
import com.pokemon.tcg.domain.model.card.Card;
import com.pokemon.tcg.domain.model.deck.Deck;
import com.pokemon.tcg.domain.model.deck.DeckCard;
import com.pokemon.tcg.domain.model.player.Player;
import com.pokemon.tcg.repository.CardRepository;
import com.pokemon.tcg.repository.DeckRepository;
import com.pokemon.tcg.repository.PlayerRepository;
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
    private final DeckMapper deckMapper;

    public DeckService(DeckRepository deckRepository,
                       CardRepository cardRepository,
                       PlayerRepository playerRepository,
                       DeckMapper deckMapper) {
        this.deckRepository   = deckRepository;
        this.cardRepository   = cardRepository;
        this.playerRepository = playerRepository;
        this.deckMapper       = deckMapper;
    }

    public DeckResponseDTO createDeck(UUID playerId, String name) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Player not found"));

        Deck deck = Deck.builder()
                .player(player)
                .name(name)
                .build();

        return deckMapper.toResponseDTO(deckRepository.save(deck));
    }

    public DeckResponseDTO addCard(UUID deckId, UUID playerId, String cardId, int quantity) {
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
        return deckMapper.toResponseDTO(deckRepository.save(deck));
    }

    public DeckValidationResult validate(UUID deckId) {
        Deck deck = deckRepository.findWithCardsById(deckId)
                .orElseThrow(() -> new IllegalArgumentException("Deck not found"));

        return buildValidationResult(deck);
    }

    public List<DeckResponseDTO> listByPlayer(UUID playerId) {
        return deckMapper.toResponseDTOList(
                deckRepository.findByPlayerIdOrderByCreatedAtDesc(playerId));
    }

    public DeckResponseDTO updateCardQuantity(UUID deckId, UUID playerId, String cardId, int quantity) {
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
        return deckMapper.toResponseDTO(deckRepository.save(deck));
    }

    public void deleteDeck(UUID deckId, UUID playerId) {
        Deck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new IllegalArgumentException("Deck not found"));

        if (!deck.getPlayer().getId().equals(playerId)) {
            throw new IllegalArgumentException("Deck does not belong to player");
        }

        deckRepository.delete(deck);
    }

    public DeckResponseDTO removeCard(UUID deckId, UUID playerId, String cardId) {
        Deck deck = deckRepository.findWithCardsById(deckId)
                .orElseThrow(() -> new IllegalArgumentException("Deck not found"));

        if (!deck.getPlayer().getId().equals(playerId)) {
            throw new IllegalArgumentException("Deck does not belong to player");
        }

        deck.getCards().removeIf(dc -> dc.getCard().getId().equals(cardId));

        // Clear featured card if the removed card was selected as featured
        if (cardId.equals(deck.getFeaturedCardId())) {
            deck.setFeaturedCardId(null);
        }

        deck.setValid(isValidDeck(deck));
        return deckMapper.toResponseDTO(deckRepository.save(deck));
    }

    private boolean isValidDeck(Deck deck) {
        int total = deck.getTotalCardCount();
        if (total != 60) return false;

        java.util.Map<String, Integer> nameCounts = new java.util.HashMap<>();
        for (DeckCard dc : deck.getCards()) {
            Card card = dc.getCard();
            if (!card.isBasicEnergy()) {
                nameCounts.merge(card.getName(), dc.getQuantity(), Integer::sum);
            }
        }
        if (nameCounts.values().stream().anyMatch(count -> count > 4)) return false;

        boolean hasBasic = deck.getCards().stream()
                .anyMatch(dc -> dc.getCard().isPokemon()
                        && dc.getCard().getSubtypes().contains("Basic"));

        return hasBasic;
    }

    public DeckResponseDTO updateDeck(UUID deckId, UUID playerId,
                                      String name, String cardBack,
                                      String coin, String featuredCardId) {
        Deck deck = deckRepository.findWithCardsById(deckId)
                .orElseThrow(() -> new IllegalArgumentException("Deck not found"));

        if (!deck.getPlayer().getId().equals(playerId)) {
            throw new IllegalArgumentException("Deck does not belong to player");
        }

        if (name != null && !name.trim().isEmpty()) {
            deck.setName(name);
        }
        if (cardBack != null) {
            deck.setCardBack(cardBack);
        }
        if (coin != null) {
            deck.setCoin(coin);
        }

        // Validate featured card: must be a Pokémon present in the deck
        if (featuredCardId != null) {
            if (featuredCardId.isBlank()) {
                deck.setFeaturedCardId(null);
            } else {
                boolean cardIsInDeck = deck.getCards().stream()
                        .anyMatch(dc -> dc.getCard().getId().equals(featuredCardId)
                                && dc.getCard().isPokemon());
                if (!cardIsInDeck) {
                    throw new IllegalArgumentException(
                            "Featured card must be a Pokémon present in the deck");
                }
                deck.setFeaturedCardId(featuredCardId);
            }
        }

        return deckMapper.toResponseDTO(deckRepository.save(deck));
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
