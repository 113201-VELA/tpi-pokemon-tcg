package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.engine.CardLookupPort;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SimisearEffect implements AttackEffect {

    private static final String YAWN         = "yawn";
    private static final String FLAMETHROWER = "flamethrower";
    private static final String FIRE_TYPE    = EnergyType.FIRE.name();

    private final CardLookupPort cardLookupPort;

    public SimisearEffect(CardLookupPort cardLookupPort) {
        this.cardLookupPort = cardLookupPort;
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        switch (attackName) {
            case YAWN         -> applyYawn(ctx);
            case FLAMETHROWER -> applyFlamethrower(ctx);
            default           -> { }
        }
    }

    private void applyYawn(AttackContext ctx) {
        String attackerId     = ctx.getAction().getPlayerId();
        PlayerState opponent  = ctx.getBoardState().getOpponentState(attackerId);
        ActivePokemon defender = opponent.getActivePokemon();

        if (defender == null) return;

        defender.getConditions().remove(SpecialCondition.CONFUSED);
        defender.getConditions().remove(SpecialCondition.PARALYZED);
        defender.getConditions().add(SpecialCondition.ASLEEP);
    }

    private void applyFlamethrower(AttackContext ctx) {
        String energyToDiscardId = ctx.getAction().getPayloadString("energyToDiscardId");
        if (energyToDiscardId == null) return;

        String attackerId         = ctx.getAction().getPlayerId();
        PlayerState attackerState = ctx.getBoardState().getStateFor(attackerId);
        ActivePokemon simisear    = attackerState.getActivePokemon();

        if (simisear == null) return;
        if (simisear.getAttachedEnergyIds() == null
                || !simisear.getAttachedEnergyIds().contains(energyToDiscardId)) return;

        if (!isFireEnergy(energyToDiscardId)) return;

        List<String> energies = new ArrayList<>(simisear.getAttachedEnergyIds());
        energies.remove(energyToDiscardId);
        simisear.setAttachedEnergyIds(energies);

        List<String> discard = new ArrayList<>(
                attackerState.getDiscard() != null
                        ? attackerState.getDiscard() : new ArrayList<>());
        discard.add(energyToDiscardId);
        attackerState.setDiscard(discard);
    }

    private boolean isFireEnergy(String cardId) {
        return cardLookupPort.findCardById(cardId)
                .map(card -> card.getTypes() != null
                        && card.getTypes().contains(FIRE_TYPE))
                .orElse(false);
    }
}
