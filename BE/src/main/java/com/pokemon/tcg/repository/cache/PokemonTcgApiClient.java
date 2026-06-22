package com.pokemon.tcg.repository.cache;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Component
public class PokemonTcgApiClient {

    private final WebClient webClient;

    @Value("${pokemon-tcg.api.key:}")
    private String apiKey;

    public PokemonTcgApiClient(WebClient.Builder webClientBuilder,
                               @Value("${pokemon-tcg.api.base-url}") String baseUrl) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    public List<Map<String, Object>> fetchCardsBySet(String setId) {
        List<Map<String, Object>> allCards = new java.util.ArrayList<>();
        int page = 1;
        int pageSize = 250;

        while (true) {
            final int currentPage = page;

            var requestSpec = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/cards")
                            .queryParam("q", "set.id:" + setId)
                            .queryParam("page", currentPage)
                            .queryParam("pageSize", pageSize)
                            .build());

            if (apiKey != null && !apiKey.isBlank()) {
                requestSpec = webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/cards")
                                .queryParam("q", "set.id:" + setId)
                                .queryParam("page", currentPage)
                                .queryParam("pageSize", pageSize)
                                .build())
                        .header("X-Api-Key", apiKey);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> response = requestSpec
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) break;

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> cards =
                    (List<Map<String, Object>>) response.get("data");

            if (cards == null || cards.isEmpty()) break;

            allCards.addAll(cards);

            Integer totalCount = (Integer) response.get("totalCount");
            if (totalCount == null || allCards.size() >= totalCount) break;

            page++;
        }

        return allCards;
    }
}