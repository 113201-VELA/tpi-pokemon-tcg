package com.pokemon.tcg.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateNicknameRequest(
        @NotBlank(message = "Nickname cannot be blank")
        @Size(max = 30, message = "Nickname must be 30 characters or fewer")
        String nickname
) {}
