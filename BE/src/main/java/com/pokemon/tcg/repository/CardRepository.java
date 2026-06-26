package com.pokemon.tcg.repository;

import com.pokemon.tcg.domain.model.card.Card;
import com.pokemon.tcg.domain.model.card.CardType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CardRepository extends JpaRepository<Card, String> {

    List<Card> findBySetId(String setId);

    boolean existsBySetId(String setId);

    long countBySetId(String setId);

    @Query("""
        SELECT c FROM Card c
        WHERE c.setId = :setId
        ORDER BY CAST(SUBSTRING(c.id, LOCATE('-', c.id) + 1) AS integer)
        """)
    List<Card> findBySetIdOrderByNumber(@Param("setId") String setId);

    @Query("""
        SELECT c FROM Card c
        WHERE c.setId = :setId
          AND LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))
        ORDER BY CAST(SUBSTRING(c.id, LOCATE('-', c.id) + 1) AS integer)
        """)
    List<Card> findBySetIdAndNameOrderByNumber(
            @Param("setId") String setId,
            @Param("name") String name);

    @Query("""
        SELECT c FROM Card c
        WHERE c.setId = :setId
          AND c.supertype = :supertype
        ORDER BY CAST(SUBSTRING(c.id, LOCATE('-', c.id) + 1) AS integer)
        """)
    List<Card> findBySetIdAndSupertypeOrderByNumber(
            @Param("setId") String setId,
            @Param("supertype") CardType supertype);

    @Query("""
        SELECT c FROM Card c
        WHERE c.setId = :setId
          AND c.supertype = :supertype
          AND LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))
        ORDER BY CAST(SUBSTRING(c.id, LOCATE('-', c.id) + 1) AS integer)
        """)
    List<Card> findBySetIdAndSupertypeAndNameOrderByNumber(
            @Param("setId") String setId,
            @Param("supertype") CardType supertype,
            @Param("name") String name);
}
