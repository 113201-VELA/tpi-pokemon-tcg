package com.pokemon.tcg.controller.dto.response;

import java.util.List;

public record CosmeticsOptionsResponse(
        List<String> cardBacks,
        List<String> coins
) {}
