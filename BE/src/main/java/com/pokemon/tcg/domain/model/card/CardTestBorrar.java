package com.pokemon.tcg.domain.model.card;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonSerialize
@JsonDeserialize
public class CardTestBorrar {
    private String name;
    private List<EnergyType> cost;
    private String damage;
    private String text;

    //Added JsonIgnore so Jackson does not serialize these methods.
    @JsonIgnore
    public int getBaseDamage() {
        if (damage == null || damage.isBlank()) return 0;
        try {
            return Integer.parseInt(damage.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @JsonIgnore
    public boolean hasVariableDamage() {
        if (damage == null || damage.isBlank()) return false;
        return damage.contains("+") || damage.contains("×") || damage.contains("-");
    }
}
