package com.pokemon.tcg.repository;

import com.pokemon.tcg.domain.model.card.Card;
import com.pokemon.tcg.domain.model.card.CardType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CardRepository extends JpaRepository<Card, String> {

    List<Card> findBySetId(String setId);

    Page<Card> findBySetIdAndNameContainingIgnoreCase(
        String setId, String name, Pageable pageable);

    Page<Card> findBySetIdAndSupertype(
        String setId, CardType supertype, Pageable pageable);

    boolean existsBySetId(String setId);

    long countBySetId(String setId);
}
