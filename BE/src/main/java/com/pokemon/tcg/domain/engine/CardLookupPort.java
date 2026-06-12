package com.pokemon.tcg.domain.engine;

import com.pokemon.tcg.domain.model.card.Attack;
import com.pokemon.tcg.domain.model.card.Card;

import java.util.Optional;

/**
 * Port (in the Ports & Adapters sense) that the game engine uses to look up
 * card data without depending on any persistence technology.
 *
 * <p>The domain engine needs two pieces of card data at runtime:
 * <ul>
 *   <li>The {@link Attack} object for a given card and attack name, so the
 *       pipeline can resolve energy costs, damage, and effects.</li>
 *   <li>The max HP of the defending Pokémon, so the KO check uses the real
 *       printed value instead of a hardcoded threshold.</li>
 * </ul>
 *
 * <p>Keeping this interface in the {@code domain.engine} package ensures that
 * {@link TurnManager} and the attack pipeline steps depend only on the domain
 * layer — never on JPA, Spring Data, or any infrastructure class.
 * The actual database query lives in {@code infrastructure.adapter.CardLookupAdapter},
 * which implements this interface and is injected by Spring at startup.
 */
public interface CardLookupPort {

    /**
     * Finds an attack by name on a specific card.
     *
     * @param cardId     the card's cache ID (e.g. {@code "xy1-1"})
     * @param attackName the attack name as it appears on the card (case-insensitive)
     * @return an {@link Optional} containing the matching {@link Attack},
     *         or empty if the card does not exist or has no attack with that name
     */
    Optional<Attack> findAttack(String cardId, String attackName);

    /**
     * Returns the printed max HP of a card.
     *
     * @param cardId the card's cache ID
     * @return the HP value, or 0 if the card is not found or has no HP
     *         (e.g. Energy and Trainer cards)
     */
    int getMaxHp(String cardId);

    /**
     * Returns the full card data for a given card ID.
     *
     * @param cardId the card's cache ID
     * @return an Optional containing the Card, or empty if not found
     */
    Optional<Card> findCardById(String cardId);
}