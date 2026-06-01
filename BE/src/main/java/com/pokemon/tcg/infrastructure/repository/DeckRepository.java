package com.pokemon.tcg.infrastructure.repository;

import com.pokemon.tcg.domain.model.deck.Deck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DeckRepository extends JpaRepository<Deck, UUID> {

    List<Deck> findByPlayerIdOrderByCreatedAtDesc(UUID playerId);

    boolean existsByIdAndPlayerId(UUID deckId, UUID playerId);
}
