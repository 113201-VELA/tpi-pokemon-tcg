package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.engine.CoinFlipService;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ZoroarkEffect implements AttackEffect {

    private static final String CORNER     = "corner";
    private static final String NIGHT_CLAW = "night claw";

    private final CoinFlipService coinFlipService;

    public ZoroarkEffect(CoinFlipService coinFlipService) {
        this.coinFlipService = coinFlipService;
    }

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("zoroark|corner", "zoroark|night claw");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        switch (attackName) {
            case CORNER     -> applyCorner(ctx);
            case NIGHT_CLAW -> applyNightClaw(ctx);
            default         -> { }
        }
    }

    private void applyCorner(AttackContext ctx) {
        String attackerId = ctx.getAction().getPlayerId();
        PlayerState defenderState = ctx.getBoardState().getOpponentState(attackerId);

        ActivePokemon defender = defenderState.getActivePokemon();
        if (defender == null) return;

        List<PokemonEffect> effects = new ArrayList<>(
                defender.getActiveEffects() != null ? defender.getActiveEffects() : new ArrayList<>());
        if (!effects.contains(PokemonEffect.CANT_RETREAT)) {
            effects.add(PokemonEffect.CANT_RETREAT);
        }
        defender.setActiveEffects(effects);
    }

    private void applyNightClaw(AttackContext ctx) {
        if (coinFlipService.flip() == CoinResult.TAILS) {
            String attackerId = ctx.getAction().getPlayerId();
            PlayerState attackerState = ctx.getBoardState().getStateFor(attackerId);
            ActivePokemon attacker = attackerState.getActivePokemon();
            if (attacker == null || attacker.getAttachedEnergyIds() == null) return;

            List<String> energies = new ArrayList<>(attacker.getAttachedEnergyIds());
            int toDiscard = Math.min(2, energies.size());

            List<String> discard = new ArrayList<>(
                    attackerState.getDiscard() != null ? attackerState.getDiscard() : new ArrayList<>());

            for (int i = 0; i < toDiscard; i++) {
                String energyId = energies.remove(energies.size() - 1);
                discard.add(energyId);
            }

            attacker.setAttachedEnergyIds(energies);
            attackerState.setDiscard(discard);
        }
    }
}