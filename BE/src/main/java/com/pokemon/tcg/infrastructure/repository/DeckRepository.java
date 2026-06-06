package com.pokemon.tcg.infrastructure.repository;

import com.pokemon.tcg.domain.model.deck.Deck;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeckRepository extends JpaRepository<Deck, UUID> {

    @EntityGraph(attributePaths = {"cards", "cards.card"})
    List<Deck> findByPlayerIdOrderByCreatedAtDesc(UUID playerId);

    @EntityGraph(attributePaths = {"cards", "cards.card"})
    Optional<Deck> findWithCardsById(UUID id);

    boolean existsByIdAndPlayerId(UUID deckId, UUID playerId);
}
