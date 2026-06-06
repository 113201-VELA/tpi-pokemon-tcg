package com.pokemon.tcg.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pokemon.tcg.api.mapper.CardMapper;
import com.pokemon.tcg.api.dto.response.CardResponseDTO;
import com.pokemon.tcg.domain.model.card.*;
import com.pokemon.tcg.infrastructure.cache.PokemonTcgApiClient;
import com.pokemon.tcg.infrastructure.repository.CardRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CardCacheService {

    private final CardRepository cardRepository;
    private final PokemonTcgApiClient apiClient;
    private final ObjectMapper objectMapper;
    private final CardMapper cardMapper;

    @Value("${pokemon-tcg.cache.set-id:xy1}")
    private String defaultSetId;

    public CardCacheService(CardRepository cardRepository,
                            PokemonTcgApiClient apiClient,
                            ObjectMapper objectMapper,
                            CardMapper cardMapper) {
        this.cardRepository = cardRepository;
        this.apiClient      = apiClient;
        this.objectMapper   = objectMapper;
        this.cardMapper     = cardMapper;
    }

    @PostConstruct
    public void initCache() {
        long count = cardRepository.countBySetId(defaultSetId);
        if (count < 100) {
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

    public Optional<CardResponseDTO> findById(String id) {
        return cardRepository.findById(id).map(cardMapper::toResponseDTO);
    }

    public Page<CardResponseDTO> searchCards(String setId, String name,
                                             CardType supertype,
                                             org.springframework.data.domain.Pageable pageable) {
        Page<Card> cardPage;
        if (name != null && !name.isBlank()) {
            cardPage = cardRepository.findBySetIdAndNameContainingIgnoreCase(setId, name, pageable);
        } else if (supertype != null) {
            cardPage = cardRepository.findBySetIdAndSupertype(setId, supertype, pageable);
        } else {
            cardPage = cardRepository.findBySetIdAndNameContainingIgnoreCase(setId, "", pageable);
        }
        return cardPage.map(cardMapper::toResponseDTO);
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

        List<String> subtypes    = toStringList(raw.get("subtypes"));
        List<String> types       = toStringList(raw.get("types"));
        List<String> retreatCost = toStringList(raw.get("retreatCost"));

        Integer hp = null;
        Object hpObj = raw.get("hp");
        if (hpObj instanceof String s && !s.isBlank()) {
            try { hp = Integer.parseInt(s); } catch (NumberFormatException ignored) {}
        }

        String imageSmall = null;
        String imageLarge = null;
        Object imagesObj = raw.get("images");
        if (imagesObj instanceof Map<?, ?> images) {
            imageSmall = (String) images.get("small");
            imageLarge = (String) images.get("large");
        }

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

        List<TypeModifier> weaknesses = parseTypeModifiers(raw.get("weaknesses"));
        List<TypeModifier> resistances = parseTypeModifiers(raw.get("resistances"));

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

        boolean basicEnergy = "Energy".equals(supertype) && subtypes.contains("Basic");

        String rawJson;
        try {
            rawJson = objectMapper.writeValueAsString(raw);
        } catch (Exception e) {
            rawJson = "{}";
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
                .attacks(attacks)
                .weaknesses(weaknesses)
                .resistances(resistances)
                .retreatCost(retreatCost)
                .abilities(abilities)
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
