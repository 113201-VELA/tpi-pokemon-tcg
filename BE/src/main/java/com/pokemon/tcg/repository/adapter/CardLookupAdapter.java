package com.pokemon.tcg.repository.adapter;

import com.pokemon.tcg.engine.CardLookupPort;
import com.pokemon.tcg.domain.model.card.Attack;
import com.pokemon.tcg.domain.model.card.Card;
import com.pokemon.tcg.repository.CardRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Infrastructure adapter that fulfils the {@link CardLookupPort} contract
 * using the local card cache stored in PostgreSQL.
 *
 * <p>This class is the only place in the codebase that knows how card data
 * is physically retrieved (JPA + {@link CardRepository}). The domain engine
 * is completely unaware of this implementation — it only sees the port interface.
 *
 * <p>The adapter is annotated with {@code @Component} so Spring injects it
 * wherever a {@link CardLookupPort} is required, satisfying the dependency
 * without the domain importing anything from the infrastructure layer.
 */
@Component
public class CardLookupAdapter implements CardLookupPort {

    private final CardRepository cardRepository;

    public CardLookupAdapter(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Loads the card from the local cache and performs a case-insensitive
     * name match against its attack list. Returns empty if the card is absent
     * or carries no attack with the given name.
     */
    @Override
    public Optional<Attack> findAttack(String cardId, String attackName) {
        if (cardId == null || attackName == null) return Optional.empty();

        return cardRepository.findById(cardId)
                .flatMap(card -> {
                    List<Attack> attacks = card.getAttacks();
                    if (attacks == null) return Optional.empty();
                    return attacks.stream()
                            .filter(a -> attackName.equalsIgnoreCase(a.getName()))
                            .findFirst();
                });
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns the printed HP from the card cache. Non-Pokémon cards
     * (Trainers, Energies) have a {@code null} HP column, so those resolve to 0.
     */
    @Override
    public int getMaxHp(String cardId) {
        if (cardId == null) return 0;
        return cardRepository.findById(cardId)
                .map(card -> card.getHp() != null ? card.getHp() : 0)
                .orElse(0);
    }

    @Override
    public Optional<Card> findCardById(String cardId) {
        if (cardId == null) return Optional.empty();
        return cardRepository.findById(cardId);
    }

    @Override
    public Map<String, Card> findAllById(Set<String> cardIds) {
        if (cardIds == null || cardIds.isEmpty()) return Map.of();
        return cardRepository.findAllById(cardIds).stream()
                .collect(Collectors.toMap(Card::getId, c -> c));
    }
}