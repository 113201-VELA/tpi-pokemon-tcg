package com.pokemon.tcg.domain.model.card;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "card_cache")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Card {

    @Id
    private String id;

    @Column(name = "set_id", nullable = false)
    private String setId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardType supertype;

    @ElementCollection
    @CollectionTable(name = "card_subtypes", joinColumns = @JoinColumn(name = "card_id"))
    @Column(name = "subtype")
    private List<String> subtypes = new ArrayList<>();

    private Integer hp;

    @ElementCollection
    @Enumerated(EnumType.STRING)
    private List<EnergyType> types = new ArrayList<>();

    private String evolvesFrom;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<Attack> attacks = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<TypeModifier> weaknesses = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<TypeModifier> resistances = new ArrayList<>();

    @ElementCollection
    @Enumerated(EnumType.STRING)
    private List<EnergyType> retreatCost = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<Ability> abilities = new ArrayList<>();

    @Column(nullable = false)
    private boolean isBasicEnergy = false;

    private String imageSmall;
    private String imageLarge;
    private String rarity;
    private String number;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String rawData;

    @Column(nullable = false)
    private Instant cachedAt = Instant.now();

    public boolean isPokemon() {
        return supertype == CardType.POKEMON;
    }

    public boolean isEnergy() {
        return supertype == CardType.ENERGY;
    }

    public boolean isTrainer() {
        return supertype == CardType.TRAINER;
    }

    public boolean isBasicPokemon() {
        return false;
    }

    public boolean isEX() {
        return false;
    }

    public boolean isMega() {
        return false;
    }

    public PokemonPhase getPhase() {
        return null;
    }

    public TrainerSubtype getTrainerSubtype() {
        return null;
    }
}
