package com.pokemon.tcg.controller.dto.response;

import java.util.UUID;

public record AuthResponse(
        UUID id,
        String username,
        String nickname,
        String email,
        String token
) {}
