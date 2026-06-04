package com.pokemon.tcg.domain.model.card;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum EnergyType {
    GRASS, FIRE, WATER, LIGHTNING, PSYCHIC,
    FIGHTING, DARKNESS, METAL, FAIRY, DRAGON, COLORLESS;

    @JsonValue
    public String toValue() {
        return name();
    }

    @JsonCreator
    public static EnergyType fromValue(String value) {
        if (value == null) return null;
        for (EnergyType type : values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return null;
    }
}