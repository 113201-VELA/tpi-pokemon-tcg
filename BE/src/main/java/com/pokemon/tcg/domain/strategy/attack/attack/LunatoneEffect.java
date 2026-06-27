package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * xy1-63 Lunatone
 *
 * Double Draw: draw 2 cards.
 * Moonblast: 20 damage. During the opponent's next turn, damage from the
 *            Defending Pokémon is reduced by 20 (before Weakness/Resistance).
 *            Implemented by adding DAMAGE_REDUCED_20 to the attacker's own
 *            ActivePokemon, which DamageApplicationStep checks when Lunatone
 *            is the defender next turn.
 */
@Component
public class LunatoneEffect implements AttackEffect {

    private static final String DOUBLE_DRAW = "double draw";
    private static final String MOONBLAST   = "moonblast";

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("lunatone|double draw", "lunatone|moonblast");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        switch (attackName) {
            case DOUBLE_DRAW -> applyDoubleDraw(ctx);
            case MOONBLAST   -> applyMoonblast(ctx);
            default          -> { }
        }
    }

    /**
     * Double Draw: draw 2 cards from the attacker's deck into hand.
     * If fewer than 2 cards remain, draw what's available.
     */
    private void applyDoubleDraw(AttackContext ctx) {
        String attackerId    = ctx.getAction().getPlayerId();
        PlayerState attacker = ctx.getBoardState().getStateFor(attackerId);

        List<String> deck = new ArrayList<>(
                attacker.getDeck() != null ? attacker.getDeck() : new ArrayList<>());
        List<String> hand = new ArrayList<>(
                attacker.getHand() != null ? attacker.getHand() : new ArrayList<>());

        int toDraw = Math.min(2, deck.size());
        for (int i = 0; i < toDraw; i++) {
            hand.add(deck.remove(0));
        }

        attacker.setDeck(deck);
        attacker.setHand(hand);
    }

    /**
     * Moonblast: add DAMAGE_REDUCED_20 to Lunatone's own activeEffects so
     * that when the opponent attacks next turn, DamageApplicationStep reduces
     * their damage by 20. TurnManager clears activeEffects between turns.
     */
    private void applyMoonblast(AttackContext ctx) {
        String attackerId     = ctx.getAction().getPlayerId();
        PlayerState attacker  = ctx.getBoardState().getStateFor(attackerId);
        ActivePokemon lunatone = attacker.getActivePokemon();

        if (lunatone == null) return;

        List<PokemonEffect> effects = new ArrayList<>(
                lunatone.getActiveEffects() != null
                        ? lunatone.getActiveEffects() : new ArrayList<>());
        if (!effects.contains(PokemonEffect.DAMAGE_REDUCED_20)) {
            effects.add(PokemonEffect.DAMAGE_REDUCED_20);
        }
        lunatone.setActiveEffects(effects);
    }
}