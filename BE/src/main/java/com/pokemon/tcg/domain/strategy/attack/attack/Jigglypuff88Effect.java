package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import com.pokemon.tcg.engine.CoinFlipService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * xy1-88 Jigglypuff
 *
 * Body Slam: 20 damage. Flip a coin. If heads, the opponent's Active
 *            Pokémon is now Paralyzed.
 */
@Component
public class Jigglypuff88Effect implements AttackEffect {

    private static final String BODY_SLAM = "body slam";

    private final CoinFlipService coinFlipService;

    public Jigglypuff88Effect(CoinFlipService coinFlipService) {
        this.coinFlipService = coinFlipService;
    }

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("jigglypuff|body slam");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        if (BODY_SLAM.equals(attackName)) {
            applyBodySlam(ctx);
        }
    }

    /**
     * Flip a coin; on heads, the opponent's Active Pokémon becomes
     * Paralyzed. Paralyzed replaces Asleep/Confused, since these rotation
     * conditions are mutually exclusive.
     */
    private void applyBodySlam(AttackContext ctx) {
        String attackerId = ctx.getAction().getPlayerId();

        if (coinFlipService.flipAndEmit(ctx, attackerId) != CoinResult.HEADS) return;

        PlayerState opponent   = ctx.getBoardState().getOpponentState(attackerId);
        ActivePokemon defender = opponent.getActivePokemon();

        if (defender == null) return;

        defender.getConditions().remove(SpecialCondition.ASLEEP);
        defender.getConditions().remove(SpecialCondition.CONFUSED);
        defender.getConditions().add(SpecialCondition.PARALYZED);
    }
}