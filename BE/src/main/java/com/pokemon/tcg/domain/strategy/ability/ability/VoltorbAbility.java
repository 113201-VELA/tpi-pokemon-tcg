package com.pokemon.tcg.domain.strategy.ability.ability;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.ability.PassiveAbilityEffect;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.engine.CoinFlipService;
import org.springframework.stereotype.Component;

/**
 * Voltorb — Destiny Burst (Passive Ability).
 *
 * <p>If this Pokémon is your Active Pokémon and is Knocked Out by damage
 * from an opponent's attack, flip a coin. If heads, put 5 damage counters
 * on the Attacking Pokémon.
 */
@Component
public class VoltorbAbility implements PassiveAbilityEffect {

    private static final int DESTINY_BURST_COUNTERS = 5;

    private final CoinFlipService coinFlipService;

    public VoltorbAbility(CoinFlipService coinFlipService) {
        this.coinFlipService = coinFlipService;
    }

    @Override
    public String getIdentifier() {
        return "voltorb";
    }

    @Override
    public void onDamageReceived(AttackContext ctx, ActivePokemon defender) {
        // Only triggers if Voltorb was knocked out by this attack
        if (ctx.getDamageToApply() <= 0) return;

        // Check if Voltorb is knocked out — defender is now null in the state
        // because PostDamageEffectStep clears it after KO.
        // We verify KO by checking the defender's current HP via the snapshot.
        String defenderId     = ctx.getBoardState()
                .getOpponentState(ctx.getAction().getPlayerId()).getPlayerId();
        PlayerState defenderState = ctx.getBoardState().getStateFor(defenderId);

        // If active is still alive, Destiny Burst does not trigger
        if (defenderState.getActivePokemon() != null) return;

        // Flip a coin — on heads, deal 5 counters to the attacker
        if (coinFlipService.flip() != CoinResult.HEADS) return;

        String attackerId    = ctx.getAction().getPlayerId();
        PlayerState attacker = ctx.getBoardState().getStateFor(attackerId);
        ActivePokemon attackingPokemon = attacker.getActivePokemon();

        if (attackingPokemon == null) return;

        attackingPokemon.setDamageCounters(
                attackingPokemon.getDamageCounters() + DESTINY_BURST_COUNTERS);
    }
}