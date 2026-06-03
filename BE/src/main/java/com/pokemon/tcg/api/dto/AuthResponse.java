package com.pokemon.tcg.api.dto;

import java.util.UUID;

public record AuthResponse(
        UUID id,
        String username,
        String email,
        String token
) {}