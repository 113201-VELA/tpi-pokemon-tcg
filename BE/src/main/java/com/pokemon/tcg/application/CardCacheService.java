package com.pokemon.tcg.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pokemon.tcg.domain.model.card.*;
import com.pokemon.tcg.infrastructure.cache.PokemonTcgApiClient;
import com.pokemon.tcg.infrastructure.repository.CardRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CardCacheService {

    private final CardRepository cardRepository;
    private final PokemonTcgApiClient apiClient;
    private final ObjectMapper objectMapper;

    @Value("${pokemon-tcg.cache.set-id:xy1}")
    private String defaultSetId;

    public CardCacheService(CardRepository cardRepository,
                            PokemonTcgApiClient apiClient,
                            ObjectMapper objectMapper) {
        this.cardRepository = cardRepository;
        this.apiClient      = apiClient;
        this.objectMapper   = objectMapper;
    }

    @PostConstruct
    public void initCache() {
        long count = cardRepository.countBySetId(defaultSetId);
        if (count < 100) {// XY1 contains 146 cards; reload if fewer than 100 are cached
            cardRepository.deleteAll(cardRepository.findBySetId(defaultSetId));
            loadSet(defaultSetId);
        }
    }

    @Transactional
    public void loadSet(String setId) {
        List<Map<String, Object>> cards = apiClient.fetchCardsBySet(setId);
        for (Map<String, Object> raw : cards) {
            try {
                Card card = mapToCard(raw);
                cardRepository.save(card);
            } catch (Exception e) {
                System.err.println("Error saving card: " + raw.get("id") + " — " + e.getMessage());
            }
        }
    }

    public Page<Card> searchCards(String setId, String name,
                                  CardType supertype, Pageable pageable) {
        if (name != null && !name.isBlank()) {
            return cardRepository.findBySetIdAndNameContainingIgnoreCase(setId, name, pageable);
        }
        if (supertype != null) {
            return cardRepository.findBySetIdAndSupertype(setId, supertype, pageable);
        }
        return cardRepository.findBySetIdAndNameContainingIgnoreCase(setId, "", pageable);
    }

    @SuppressWarnings("unchecked")
    private Card mapToCard(Map<String, Object> raw) throws Exception {
        String id          = (String) raw.get("id");
        String name        = (String) raw.get("name");
        String setId       = (String) ((Map<String, Object>) raw.get("set")).get("id");
        String supertype   = (String) raw.get("supertype");
        String number      = (String) raw.get("number");
        String rarity      = (String) raw.get("rarity");
        String evolvesFrom = (String) raw.get("evolvesFrom");

        // subtypes, types, retreatCost
        List<String> subtypes    = toStringList(raw.get("subtypes"));
        List<String> types       = toStringList(raw.get("types"));
        List<String> retreatCost = toStringList(raw.get("retreatCost"));
        // hp
        Integer hp = null;
        Object hpObj = raw.get("hp");
        if (hpObj instanceof String s && !s.isBlank()) {
            try { hp = Integer.parseInt(s); } catch (NumberFormatException ignored) {}
        }
        // images
        String imageSmall = null;
        String imageLarge = null;
        Object imagesObj = raw.get("images");
        if (imagesObj instanceof Map<?, ?> images) {
            imageSmall = (String) images.get("small");
            imageLarge = (String) images.get("large");
        }

        // attacks
        List<Attack> attacks = new ArrayList<>();
        Object attacksObj = raw.get("attacks");
        if (attacksObj instanceof List<?> list) {
            for (Object a : list) {
                if (a instanceof Map<?, ?> am) {
                    List<String> costStrings = toStringList(am.get("cost"));
                    List<EnergyType> cost = costStrings.stream()
                            .map(this::parseEnergyType)
                            .filter(e -> e != null)
                            .toList();
                    attacks.add(Attack.builder()
                            .name((String) am.get("name"))
                            .cost(cost)
                            .damage((String) am.get("damage"))
                            .text((String) am.get("text"))
                            .build());
                }
            }
        }

        // weaknesses
        List<TypeModifier> weaknesses = parseTypeModifiers(raw.get("weaknesses"));

        // resistances
        List<TypeModifier> resistances = parseTypeModifiers(raw.get("resistances"));

        // abilities
        List<Ability> abilities = new ArrayList<>();
        Object abilitiesObj = raw.get("abilities");
        if (abilitiesObj instanceof List<?> list) {
            for (Object a : list) {
                if (a instanceof Map<?, ?> am) {
                    abilities.add(Ability.builder()
                            .name((String) am.get("name"))
                            .text((String) am.get("text"))
                            .type((String) am.get("type"))
                            .build());
                }
            }
        }
        // basicEnergy
        boolean basicEnergy = "Energy".equals(supertype) && subtypes.contains("Basic");
        // rawData
        String rawJson;
        try {
            rawJson = objectMapper.writeValueAsString(raw);
        } catch (Exception e) {
            rawJson = "{}";
        }

        String attacksJson;
        String weaknessesJson;
        String resistancesJson;
        String abilitiesJson;
        try {
            attacksJson    = objectMapper.writeValueAsString(attacks);
            weaknessesJson  = objectMapper.writeValueAsString(weaknesses);
            resistancesJson = objectMapper.writeValueAsString(resistances);
            abilitiesJson   = objectMapper.writeValueAsString(abilities);
        } catch (Exception e) {
            attacksJson    = "[]";
            weaknessesJson  = "[]";
            resistancesJson = "[]";
            abilitiesJson   = "[]";
        }

        return Card.builder()
                .id(id)
                .setId(setId)
                .name(name)
                .supertype(parseCardType(supertype))
                .subtypes(subtypes)
                .hp(hp)
                .types(types)
                .evolvesFrom(evolvesFrom)
                .attacks(attacksJson)
                .weaknesses(weaknessesJson)
                .resistances(resistancesJson)
                .retreatCost(retreatCost)
                .abilities(abilitiesJson)
                .basicEnergy(basicEnergy)
                .imageSmall(imageSmall)
                .imageLarge(imageLarge)
                .rarity(rarity)
                .number(number)
                .rawData(rawJson)
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<String> toStringList(Object obj) {
        if (obj instanceof List<?> list) {
            return list.stream()
                    .filter(o -> o instanceof String)
                    .map(o -> (String) o)
                    .toList();
        }
        return new ArrayList<>();
    }

    private List<TypeModifier> parseTypeModifiers(Object obj) {
        List<TypeModifier> result = new ArrayList<>();
        if (obj instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> m) {
                    result.add(TypeModifier.builder()
                            .type(parseEnergyType((String) m.get("type")))
                            .value((String) m.get("value"))
                            .build());
                }
            }
        }
        return result;
    }

    private CardType parseCardType(String s) {
        if (s == null) return CardType.TRAINER;
        return switch (s) {
            case "Pokémon", "Pokemon" -> CardType.POKEMON;
            case "Energy"             -> CardType.ENERGY;
            default                   -> CardType.TRAINER;
        };
    }

    private EnergyType parseEnergyType(Object obj) {
        if (!(obj instanceof String s)) return null;
        return switch (s) {
            case "Grass"     -> EnergyType.GRASS;
            case "Fire"      -> EnergyType.FIRE;
            case "Water"     -> EnergyType.WATER;
            case "Lightning" -> EnergyType.LIGHTNING;
            case "Psychic"   -> EnergyType.PSYCHIC;
            case "Fighting"  -> EnergyType.FIGHTING;
            case "Darkness"  -> EnergyType.DARKNESS;
            case "Metal"     -> EnergyType.METAL;
            case "Fairy"     -> EnergyType.FAIRY;
            case "Dragon"    -> EnergyType.DRAGON;
            case "Colorless" -> EnergyType.COLORLESS;
            default          -> null;
        };
    }
}