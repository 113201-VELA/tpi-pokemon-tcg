package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import com.pokemon.tcg.engine.CoinFlipService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * xy1-71 Krookodile
 *
 * Bother: 50 damage. Flip a coin; if heads, the opponent can't play any
 *         Supporter cards during their next turn.
 * Knock Back: 80 damage. The opponent switches their Active Pokémon with
 *             one of their Benched Pokémon (forced switch).
 */
@Component
public class KrookodileEffect implements AttackEffect {

    private static final String BOTHER     = "bother";
    private static final String KNOCK_BACK = "knock back";

    private final CoinFlipService coinFlipService;

    public KrookodileEffect(CoinFlipService coinFlipService) {
        this.coinFlipService = coinFlipService;
    }

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("krookodile|bother", "krookodile|knock back");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        switch (attackName) {
            case BOTHER     -> applyBother(ctx);
            case KNOCK_BACK -> applyKnockBack(ctx);
            default         -> { }
        }
    }

    /**
     * Bother: flip a coin; on heads add NO_SUPPORTER to the opponent's
     * Active Pokémon so RuleValidator blocks Supporter plays next turn.
     * TurnManager clears activeEffects between turns.
     */
    private void applyBother(AttackContext ctx) {
        if (coinFlipService.flip() != CoinResult.HEADS) return;

        String attackerId    = ctx.getAction().getPlayerId();
        PlayerState opponent = ctx.getBoardState().getOpponentState(attackerId);
        ActivePokemon defender = opponent.getActivePokemon();

        if (defender == null) return;

        List<PokemonEffect> effects = new ArrayList<>(
                defender.getActiveEffects() != null
                        ? defender.getActiveEffects() : new ArrayList<>());
        if (!effects.contains(PokemonEffect.NO_SUPPORTER)) {
            effects.add(PokemonEffect.NO_SUPPORTER);
        }
        defender.setActiveEffects(effects);
    }

    /**
     * Knock Back: force the opponent to switch their Active Pokémon with
     * one of their Benched Pokémon. Only triggers if the opponent has
     * at least one Pokémon on the Bench.
     */
    private void applyKnockBack(AttackContext ctx) {
        String attackerId    = ctx.getAction().getPlayerId();
        PlayerState opponent = ctx.getBoardState().getOpponentState(attackerId);

        List<BenchPokemon> bench = opponent.getBench();
        if (bench == null || bench.isEmpty()) return;

        ctx.setBoardState(ctx.getBoardState().toBuilder()
                .pendingForcedSwitchPlayerId(opponent.getPlayerId())
                .build());
    }
}