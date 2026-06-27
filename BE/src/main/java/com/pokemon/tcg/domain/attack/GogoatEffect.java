package com.pokemon.tcg.domain.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.engine.CardLookupPort;
import com.pokemon.tcg.engine.DamageModifier;
import com.pokemon.tcg.engine.attack.AttackContext;
import com.pokemon.tcg.engine.attack.AttackEffect;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class GogoatEffect implements AttackEffect {

    private static final String LEAD         = "lead";
    private static final String CHARGE_DASH  = "charge dash";
    private static final String SUPPORTER    = "Supporter";
    private static final String ATTACK_KEY   = "gogoat|lead";
    private static final int    MAX_CARDS    = 2;
    private static final int    BOOST_DAMAGE = 20;
    private static final int    RECOIL_COUNTERS = 2;

    private final CardLookupPort cardLookupPort;

    public GogoatEffect(CardLookupPort cardLookupPort) {
        this.cardLookupPort = cardLookupPort;
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        switch (attackName) {
            case LEAD        -> applyLead(ctx);
            case CHARGE_DASH -> applyChargeDash(ctx);
            default          -> { }
        }
    }

    private void applyLead(AttackContext ctx) {
        String attackerId = ctx.getAction().getPlayerId();
        PlayerState ps    = ctx.getBoardState().getStateFor(attackerId);

        List<String> deck = ps.getDeck() != null ? ps.getDeck() : List.of();

        List<String> supporterIds = deck.stream()
                .filter(this::isSupporter)
                .toList();

        if (supporterIds.isEmpty()) return;

        ctx.setBoardState(ctx.getBoardState().toBuilder()
                .pendingAttackSelectionKey(ATTACK_KEY)
                .pendingAttackSelectionPlayerId(attackerId)
                .pendingAttackSelectionMaxCards(MAX_CARDS)
                .pendingDeckSelectionCardIds(new ArrayList<>(supporterIds))
                .build());
    }

    private void applyChargeDash(AttackContext ctx) {
        Boolean chargeBoost = getChargeBoost(ctx.getAction());
        if (!Boolean.TRUE.equals(chargeBoost)) return;

        List<DamageModifier> modifiers = new ArrayList<>(
                ctx.getModifiers() != null ? ctx.getModifiers() : new ArrayList<>());
        modifiers.add(new DamageModifier("charge-dash-boost", BOOST_DAMAGE, true));
        ctx.setModifiers(modifiers);

        String attackerId         = ctx.getAction().getPlayerId();
        PlayerState attackerState = ctx.getBoardState().getStateFor(attackerId);
        ActivePokemon gogoat      = attackerState.getActivePokemon();

        if (gogoat != null) {
            gogoat.setDamageCounters(gogoat.getDamageCounters() + RECOIL_COUNTERS);
        }
    }

    private boolean isSupporter(String cardId) {
        return cardLookupPort.findCardById(cardId)
                .map(card -> card.getSubtypes() != null
                        && card.getSubtypes().contains(SUPPORTER))
                .orElse(false);
    }

    private Boolean getChargeBoost(GameAction action) {
        Object raw = action.getPayload() != null
                ? action.getPayload().get("chargeBoost")
                : null;
        if (raw instanceof Boolean b) return b;
        return null;
    }
}
