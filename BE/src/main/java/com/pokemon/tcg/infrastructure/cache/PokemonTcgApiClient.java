package com.pokemon.tcg.infrastructure.cache;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Component
public class PokemonTcgApiClient {

    private static final String API_URL = "https://api.pokemontcg.io/v2/cards";

    private final WebClient webClient;

    public PokemonTcgApiClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(API_URL).build();
    }

    public List<Map<String, Object>> fetchCardsBySet(String setId) {
        return List.of();
    }
}
