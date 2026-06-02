package com.pokemon.tcg.domain.model.card;

import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
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

    @Type(ListArrayType.class)
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private List<String> subtypes = new ArrayList<>();

    private Integer hp;

    @Type(ListArrayType.class)
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private List<String> types = new ArrayList<>();

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

    @Type(ListArrayType.class)
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private List<String> retreatCost = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<Ability> abilities = new ArrayList<>();

    // Renombrado para evitar conflicto Lombok/JPA con "is" prefix
    @Column(name = "is_basic_energy", nullable = false)
    private boolean basicEnergy = false;

    private String imageSmall;
    private String imageLarge;
    private String rarity;
    private String number;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String rawData;

    @Column(nullable = false)
    private Instant cachedAt = Instant.now();

    public boolean isPokemon()  { return supertype == CardType.POKEMON; }
    public boolean isEnergy()   { return supertype == CardType.ENERGY; }
    public boolean isTrainer()  { return supertype == CardType.TRAINER; }

    public boolean isBasicPokemon() { return false; }
    public boolean isEX()           { return false; }
    public boolean isMega()         { return false; }

    public PokemonPhase getPhase()             { return null; }
    public TrainerSubtype getTrainerSubtype()   { return null; }
}