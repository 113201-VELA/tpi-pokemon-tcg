package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import com.pokemon.tcg.engine.CoinFlipService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * xy1-70 Krokorok
 *
 * Crunch: 20 damage. Flip a coin; if heads, discard an Energy attached to
 *         the opponent's Active Pokémon (specified via energyToDiscardId).
 * Darkness Fang: 60 damage. No additional effect.
 */
@Component
public class KrokorokEffect implements AttackEffect {

    private static final String CRUNCH         = "crunch";
    private static final String DARKNESS_FANG  = "darkness fang";

    private final CoinFlipService coinFlipService;

    public KrokorokEffect(CoinFlipService coinFlipService) {
        this.coinFlipService = coinFlipService;
    }

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("krokorok|crunch", "krokorok|darkness fang");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        switch (attackName) {
            case CRUNCH        -> applyCrunch(ctx);
            case DARKNESS_FANG -> { } // 60 damage handled by pipeline — no extra effect
            default            -> { }
        }
    }

    /**
     * Crunch: flip a coin; on heads discard the energy specified by
     * energyToDiscardId from the opponent's Active Pokémon.
     */
    private void applyCrunch(AttackContext ctx) {
        if (coinFlipService.flip() != CoinResult.HEADS) return;

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