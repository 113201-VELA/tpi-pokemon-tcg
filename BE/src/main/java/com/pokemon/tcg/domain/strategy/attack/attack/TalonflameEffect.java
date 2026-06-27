package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import com.pokemon.tcg.engine.CardLookupPort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class TalonflameEffect implements AttackEffect {

    private static final String DEVASTATING_WIND = "devastating wind";
    private static final String FLARE_BLITZ      = "flare blitz";
    private static final String FIRE_TYPE        = EnergyType.FIRE.name();
    private static final int    CARDS_TO_DRAW    = 4;

    private final CardLookupPort cardLookupPort;

    public TalonflameEffect(CardLookupPort cardLookupPort) {
        this.cardLookupPort = cardLookupPort;
    }

    @Override
    public List<String> getSupportedAttacks() {
        return List.of(
                "talonflame|devastating wind",
                "talonflame|flare blitz"
        );
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        switch (attackName) {
            case DEVASTATING_WIND -> applyDevastatingWind(ctx);
            case FLARE_BLITZ     -> applyFlareBlitz(ctx);
            default              -> { }
        }
    }

    private void applyDevastatingWind(AttackContext ctx) {
        String attackerId    = ctx.getAction().getPlayerId();
        PlayerState opponent = ctx.getBoardState().getOpponentState(attackerId);

        List<String> deck = new ArrayList<>(
                opponent.getDeck() != null ? opponent.getDeck() : new ArrayList<>());
        if (opponent.getHand() != null) {
            deck.addAll(opponent.getHand());
        }
        Collections.shuffle(deck);

        List<String> newHand = new ArrayList<>();
        int cardsToDraw = Math.min(CARDS_TO_DRAW, deck.size());
        for (int i = 0; i < cardsToDraw; i++) {
            newHand.add(deck.remove(0));
        }

        opponent.setDeck(deck);
        opponent.setHand(newHand);
    }

    private void applyFlareBlitz(AttackContext ctx) {
        String attackerId         = ctx.getAction().getPlayerId();
        PlayerState attackerState = ctx.getBoardState().getStateFor(attackerId);
        ActivePokemon talonflame  = attackerState.getActivePokemon();

        if (talonflame == null) return;

        List<String> attached = talonflame.getAttachedEnergyIds() != null
                ? new ArrayList<>(talonflame.getAttachedEnergyIds())
                : new ArrayList<>();

        List<String> fireEnergies = attached.stream()
                .filter(this::isFireEnergy)
                .collect(Collectors.toList());
        List<String> remaining = attached.stream()
                .filter(id -> !isFireEnergy(id))
                .collect(Collectors.toList());

        if (fireEnergies.isEmpty()) return;

        talonflame.setAttachedEnergyIds(remaining);

        List<String> discard = new ArrayList<>(
                attackerState.getDiscard() != null
                        ? attackerState.getDiscard() : new ArrayList<>());
        discard.addAll(fireEnergies);
        attackerState.setDiscard(discard);
    }

    private boolean isFireEnergy(String cardId) {
        return cardLookupPort.findCardById(cardId)
                .map(card -> card.getTypes() != null
                        && card.getTypes().contains(FIRE_TYPE))
                .orElse(false);
    }
}
