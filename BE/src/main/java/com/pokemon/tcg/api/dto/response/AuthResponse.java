package com.pokemon.tcg.api.dto.response;

import java.util.UUID;

public record AuthResponse(
        UUID id,
        String username,
        String email,
        String token
) {}
