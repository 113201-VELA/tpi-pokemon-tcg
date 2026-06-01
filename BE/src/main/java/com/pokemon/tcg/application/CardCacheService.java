package com.pokemon.tcg.application;

import com.pokemon.tcg.domain.model.card.Card;
import com.pokemon.tcg.domain.model.card.CardType;
import com.pokemon.tcg.infrastructure.cache.PokemonTcgApiClient;
import com.pokemon.tcg.infrastructure.repository.CardRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class CardCacheService {

    private final CardRepository cardRepository;
    private final PokemonTcgApiClient apiClient;

    public CardCacheService(CardRepository cardRepository,
                            PokemonTcgApiClient apiClient) {
        this.cardRepository = cardRepository;
        this.apiClient      = apiClient;
    }

    @PostConstruct
    public void initCache() {
    }

    public void loadSet(String setId) {
    }

    public Page<Card> searchCards(String setId, String name,
                                   CardType supertype, Pageable pageable) {
        return Page.empty();
    }
}
