package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import com.pokemon.tcg.domain.strategy.attack.DamageModifier;
import com.pokemon.tcg.engine.CardLookupPort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DelphoxEffect implements AttackEffect {

    private static final int    DAMAGE_PER_FIRE = 20;
    private static final String FIRE_TYPE       = EnergyType.FIRE.name();

    private final CardLookupPort cardLookupPort;

    public DelphoxEffect(CardLookupPort cardLookupPort) {
        this.cardLookupPort = cardLookupPort;
    }

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("delphox|blaze ball");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackerId         = ctx.getAction().getPlayerId();
        PlayerState attackerState = ctx.getBoardState().getStateFor(attackerId);
        ActivePokemon delphox     = attackerState.getActivePokemon();

        if (delphox == null) return;

        List<String> attachedIds = delphox.getAttachedEnergyIds() != null
                ? delphox.getAttachedEnergyIds()
                : List.of();

        long fireCount = attachedIds.stream()
                .filter(this::isFireEnergy)
                .count();

        if (fireCount > 0) {
            List<DamageModifier> modifiers = new ArrayList<>(
                    ctx.getModifiers() != null ? ctx.getModifiers() : new ArrayList<>());
            modifiers.add(new DamageModifier(
                    "blaze-ball-fire", (int) fireCount * DAMAGE_PER_FIRE, true));
            ctx.setModifiers(modifiers);
        }
    }

    private boolean isFireEnergy(String cardId) {
        return cardLookupPort.findCardById(cardId)
                .map(card -> card.getTypes() != null
                        && card.getTypes().contains(FIRE_TYPE))
                .orElse(false);
    }
}
