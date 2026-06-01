package com.pokemon.tcg.domain.model.card;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Attack {
    private String name;
    private List<EnergyType> cost;
    private String damage;
    private String text;

    public int getBaseDamage() {
        return 0;
    }

    public boolean hasVariableDamage() {
        return false;
    }
}
