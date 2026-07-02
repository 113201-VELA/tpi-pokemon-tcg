package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import com.pokemon.tcg.engine.CoinFlipService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * xy1-81 Pawniard
 *
 * Cut Down: flip a coin. If heads, discard an Energy attached to the
 *           opponent's Active Pokémon.
 * Metal Claw: 30 damage, no additional text — plain attack, no effect needed.
 */
@Component
public class PawniardEffect implements AttackEffect {

    private static final String CUT_DOWN = "cut down";

    private final CoinFlipService coinFlipService;

    public PawniardEffect(CoinFlipService coinFlipService) {
        this.coinFlipService = coinFlipService;
    }

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("pawniard|cut down");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        if (CUT_DOWN.equals(attackName)) {
            applyCutDown(ctx);
        }
    }

    /**
     * Cut Down: flip a coin; on heads, discard an Energy attached to the
     * opponent's Active Pokémon. If the attacker specifies which Energy to
     * discard via {@code energyCardId} and it is actually attached, that one
     * is discarded; otherwise the first attached Energy is discarded.
     */
    private void applyCutDown(AttackContext ctx) {
        String attackerId = ctx.getAction().getPlayerId();

        if (coinFlipService.flipAndEmit(ctx, attackerId) != CoinResult.HEADS) return;

        PlayerState opponent   = ctx.getBoardState().getOpponentState(attackerId);
        ActivePokemon defender = opponent.getActivePokemon();

        if (defender == null
                || defender.getAttachedEnergyIds() == null
                || defender.getAttachedEnergyIds().isEmpty()) return;

        List<String> energies = new ArrayList<>(defender.getAttachedEnergyIds());
        String requestedEnergyId = ctx.getAction().getPayloadString("energyCardId");

        String chosen = (requestedEnergyId != null && energies.contains(requestedEnergyId))
                ? requestedEnergyId
                : energies.get(0);

        energies.remove(chosen);
        defender.setAttachedEnergyIds(energies);

        List<String> discard = new ArrayList<>(
                opponent.getDiscard() != null ? opponent.getDiscard() : new ArrayList<>());
        discard.add(chosen);
        opponent.setDiscard(discard);
    }
}