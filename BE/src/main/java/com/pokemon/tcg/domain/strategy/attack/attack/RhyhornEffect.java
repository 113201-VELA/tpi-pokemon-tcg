package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import com.pokemon.tcg.engine.CardLookupPort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * xy1-60 Rhyhorn
 *
 * Dig Out: 10 damage. Discard the top card of your deck. If that card is a
 *          Fighting Energy, attach it to this Pokémon.
 * Horn Drill: 40 damage. No additional effect.
 */
@Component
public class RhyhornEffect implements AttackEffect {

    private static final String FIGHTING_TYPE = EnergyType.FIGHTING.name();
    private static final String DIG_OUT       = "dig out";
    private static final String HORN_DRILL    = "horn drill";

    private final CardLookupPort cardLookupPort;

    public RhyhornEffect(CardLookupPort cardLookupPort) {
        this.cardLookupPort = cardLookupPort;
    }

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("rhyhorn|dig out", "rhyhorn|horn drill");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        switch (attackName) {
            case DIG_OUT    -> applyDigOut(ctx);
            case HORN_DRILL -> { } // 40 damage handled by pipeline — no extra effect
            default         -> { }
        }
    }

    /**
     * Dig Out: discard the top card of the attacker's deck. If it is a
     * Fighting Energy card, attach it to Rhyhorn instead of discarding it.
     */
    private void applyDigOut(AttackContext ctx) {
        String attackerId    = ctx.getAction().getPlayerId();
        PlayerState attacker = ctx.getBoardState().getStateFor(attackerId);
        ActivePokemon rhyhorn = attacker.getActivePokemon();

        if (rhyhorn == null) return;

        List<String> deck = attacker.getDeck() != null
                ? new ArrayList<>(attacker.getDeck())
                : new ArrayList<>();

        if (deck.isEmpty()) return;

        String topCard = deck.remove(0);
        attacker.setDeck(deck);

        if (isFightingEnergy(topCard)) {
            List<String> energies = new ArrayList<>(
                    rhyhorn.getAttachedEnergyIds() != null
                            ? rhyhorn.getAttachedEnergyIds() : new ArrayList<>());
            energies.add(topCard);
            rhyhorn.setAttachedEnergyIds(energies);
        } else {
            List<String> discard = new ArrayList<>(
                    attacker.getDiscard() != null
                            ? attacker.getDiscard() : new ArrayList<>());
            discard.add(topCard);
            attacker.setDiscard(discard);
        }
    }

    private boolean isFightingEnergy(String cardId) {
        return cardLookupPort.findCardById(cardId)
                .map(card -> card.isBasicEnergy()
                        && card.getTypes() != null
                        && card.getTypes().contains(FIGHTING_TYPE))
                .orElse(false);
    }
}