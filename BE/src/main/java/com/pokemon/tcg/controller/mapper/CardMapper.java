package com.pokemon.tcg.controller.mapper;

import com.pokemon.tcg.controller.dto.response.CardResponseDTO;
import com.pokemon.tcg.domain.model.card.Card;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class CardMapper {

    public CardResponseDTO toResponseDTO(Card card) {
        if (card == null) return null;
        return new CardResponseDTO(
                card.getId(),
                card.getSetId(),
                card.getName(),
                card.getSupertype(),
                safeList(card.getSubtypes()),
                card.getHp(),
                safeList(card.getTypes()),
                card.getEvolvesFrom(),
                safeList(card.getAttacks()),
                safeList(card.getWeaknesses()),
                safeList(card.getResistances()),
                safeList(card.getRetreatCost()),
                safeList(card.getAbilities()),
                card.isBasicEnergy(),
                card.getImageSmall(),
                card.getImageLarge(),
                card.getRarity(),
                card.getNumber()
        );
    }

    private <T> List<T> safeList(List<T> list) {
        return list != null ? list : Collections.emptyList();
    }
}
