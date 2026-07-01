package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import com.pokemon.tcg.engine.CoinFlipService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * xy1-68 Sableye
 *
 * Filch: draw a card.
 * Rip Claw: 30 damage. Flip a coin; if heads, discard an Energy attached
 *           to the opponent's Active Pokémon. The energy to discard is
 *           specified via energyToDiscardId in the action payload.
 */
@Component
public class SableyeEffect implements AttackEffect {

    private static final String FILCH    = "filch";
    private static final String RIP_CLAW = "rip claw";

    private final CoinFlipService coinFlipService;

    public SableyeEffect(CoinFlipService coinFlipService) {
        this.coinFlipService = coinFlipService;
    }

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("sableye|filch", "sableye|rip claw");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        switch (attackName) {
            case FILCH    -> applyFilch(ctx);
            case RIP_CLAW -> applyRipClaw(ctx);
            default       -> { }
        }
    }

    /**
     * Filch: draw 1 card from the attacker's deck into hand.
     * If the deck is empty, nothing happens.
     */
    private void applyFilch(AttackContext ctx) {
        String attackerId    = ctx.getAction().getPlayerId();
        PlayerState attacker = ctx.getBoardState().getStateFor(attackerId);

        List<String> deck = new ArrayList<>(
                attacker.getDeck() != null ? attacker.getDeck() : new ArrayList<>());
        if (deck.isEmpty()) return;

        List<String> hand = new ArrayList<>(
                attacker.getHand() != null ? attacker.getHand() : new ArrayList<>());

        hand.add(deck.remove(0));
        attacker.setDeck(deck);
        attacker.setHand(hand);
    }

    /**
     * Rip Claw: flip a coin; on heads, discard the energy specified by
     * energyToDiscardId from the opponent's Active Pokémon.
     * If the coin is tails or the energy is not attached, nothing happens.
     */
    private void applyRipClaw(AttackContext ctx) {
        if (coinFlipService.flipAndEmit(ctx, ctx.getAction().getPlayerId()) != CoinResult.HEADS) return;

        String energyToDiscardId = ctx.getAction().getPayloadString("energyToDiscardId");
        if (energyToDiscardId == null) return;

        String attackerId    = ctx.getAction().getPlayerId();
        PlayerState opponent = ctx.getBoardState().getOpponentState(attackerId);
        ActivePokemon defender = opponent.getActivePokemon();

        if (defender == null) return;
        if (defender.getAttachedEnergyIds() == null
                || !defender.getAttachedEnergyIds().contains(energyToDiscardId)) return;

        List<String> energies = new ArrayList<>(defender.getAttachedEnergyIds());
        energies.remove(energyToDiscardId);
        defender.setAttachedEnergyIds(energies);

        List<String> discard = new ArrayList<>(
                opponent.getDiscard() != null ? opponent.getDiscard() : new ArrayList<>());
        discard.add(energyToDiscardId);
        opponent.setDiscard(discard);
    }
}