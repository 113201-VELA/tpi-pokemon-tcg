package com.pokemon.tcg.domain.attack;

import com.pokemon.tcg.domain.model.card.Card;
import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.engine.CardLookupPort;
import com.pokemon.tcg.engine.DamageModifier;
import com.pokemon.tcg.engine.attack.AttackContext;
import com.pokemon.tcg.engine.attack.AttackEffect;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class VivillonEffect implements AttackEffect {

    private static final String CONVERSION_POWDER = "conversion powder";
    private static final String COLORFUL_WIND     = "colorful wind";
    private static final String RAINBOW_ENERGY    = "rainbow energy";
    private static final int    DAMAGE_PER_TYPE   = 30;

    private static final List<EnergyType> ALL_TYPES = List.of(
            EnergyType.GRASS, EnergyType.FIRE, EnergyType.WATER,
            EnergyType.LIGHTNING, EnergyType.PSYCHIC, EnergyType.FIGHTING,
            EnergyType.DARKNESS, EnergyType.METAL, EnergyType.FAIRY,
            EnergyType.DRAGON, EnergyType.COLORLESS
    );

    private final CardLookupPort cardLookupPort;

    public VivillonEffect(CardLookupPort cardLookupPort) {
        this.cardLookupPort = cardLookupPort;
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        switch (attackName) {
            case CONVERSION_POWDER -> applyConversionPowder(ctx);
            case COLORFUL_WIND     -> applyColorfulWind(ctx);
            default                -> { }
        }
    }

    private void applyConversionPowder(AttackContext ctx) {
        String chosen = ctx.getAction().getPayloadString("chosenCondition");
        if (chosen == null) return;

        String attackerId     = ctx.getAction().getPlayerId();
        PlayerState opponent  = ctx.getBoardState().getOpponentState(attackerId);
        ActivePokemon defender = opponent.getActivePokemon();
        if (defender == null) return;

        switch (chosen.toUpperCase()) {
            case "ASLEEP" -> {
                defender.getConditions().remove(SpecialCondition.CONFUSED);
                defender.getConditions().remove(SpecialCondition.PARALYZED);
                defender.getConditions().add(SpecialCondition.ASLEEP);
            }
            case "POISONED" -> defender.getConditions().add(SpecialCondition.POISONED);
            default -> { }
        }
    }

    private void applyColorfulWind(AttackContext ctx) {
        String attackerId         = ctx.getAction().getPlayerId();
        PlayerState attackerState = ctx.getBoardState().getStateFor(attackerId);
        ActivePokemon vivillon    = attackerState.getActivePokemon();

        if (vivillon == null) return;

        List<String> attachedIds = vivillon.getAttachedEnergyIds() != null
                ? vivillon.getAttachedEnergyIds()
                : List.of();

        Set<EnergyType> uniqueTypes = new HashSet<>();
        boolean hasRainbow = false;

        for (String cardId : attachedIds) {
            Optional<Card> cardOpt = cardLookupPort.findCardById(cardId);
            if (cardOpt.isEmpty()) continue;

            Card card = cardOpt.get();
            String name = card.getName() != null ? card.getName().toLowerCase() : "";

            if (RAINBOW_ENERGY.equals(name)) {
                hasRainbow = true;
            } else {
                EnergyType type = resolveEnergyType(card);
                if (type != null) uniqueTypes.add(type);
            }
        }

        if (hasRainbow && uniqueTypes.size() < ALL_TYPES.size()) {
            ALL_TYPES.stream()
                    .filter(t -> !uniqueTypes.contains(t))
                    .findFirst()
                    .ifPresent(uniqueTypes::add);
        }

        int totalDamage = uniqueTypes.size() * DAMAGE_PER_TYPE;
        if (totalDamage > 0) {
            List<DamageModifier> modifiers = new ArrayList<>(
                    ctx.getModifiers() != null ? ctx.getModifiers() : new ArrayList<>());
            modifiers.add(new DamageModifier("colorful-wind-types", totalDamage, true));
            ctx.setModifiers(modifiers);
        }
    }

    private EnergyType resolveEnergyType(Card card) {
        if (card.getTypes() == null || card.getTypes().isEmpty()) return null;
        try {
            return EnergyType.valueOf(card.getTypes().get(0));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
