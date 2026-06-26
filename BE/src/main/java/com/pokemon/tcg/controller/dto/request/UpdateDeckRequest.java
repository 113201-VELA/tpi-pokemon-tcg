package com.pokemon.tcg.controller.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateDeckRequest {
    private String name;
    private String cardBack;
    private String coin;
    private String featuredCardId;
}
