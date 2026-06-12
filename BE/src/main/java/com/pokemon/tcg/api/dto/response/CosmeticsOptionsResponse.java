package com.pokemon.tcg.api.dto.response;

import java.util.List;

public record CosmeticsOptionsResponse(
        List<String> cardBacks,
        List<String> coins
) {}
