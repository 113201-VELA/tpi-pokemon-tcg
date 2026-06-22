package com.pokemon.tcg.controller.mapper;

import com.pokemon.tcg.controller.dto.response.DeckCardResponseDTO;
import com.pokemon.tcg.controller.dto.response.DeckResponseDTO;
import com.pokemon.tcg.domain.model.deck.Deck;
import com.pokemon.tcg.domain.model.deck.DeckCard;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DeckMapper {

    private final CardMapper cardMapper;

    public DeckMapper(CardMapper cardMapper) {
        this.cardMapper = cardMapper;
    }

    public DeckResponseDTO toResponseDTO(Deck deck) {
        if (deck == null) return null;
        List<DeckCardResponseDTO> cardDTOs = safeList(deck.getCards()).stream()
                .map(this::toDeckCardResponseDTO)
                .collect(Collectors.toList());
        return new DeckResponseDTO(
                deck.getId(),
                deck.getName(),
                deck.getCardBack(),
                deck.getCoin(),
                deck.isValid(),
                cardDTOs,
                deck.getTotalCardCount()
        );
    }

    public List<DeckResponseDTO> toResponseDTOList(List<Deck> decks) {
        if (decks == null) return Collections.emptyList();
        return decks.stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    private DeckCardResponseDTO toDeckCardResponseDTO(DeckCard deckCard) {
        if (deckCard == null) return null;
        return new DeckCardResponseDTO(
                deckCard.getId(),
                cardMapper.toResponseDTO(deckCard.getCard()),
                deckCard.getQuantity()
        );
    }

    private <T> List<T> safeList(List<T> list) {
        return list != null ? list : Collections.emptyList();
    }
}
