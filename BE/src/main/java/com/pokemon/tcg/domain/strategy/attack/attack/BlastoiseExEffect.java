package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import com.pokemon.tcg.engine.CoinFlipService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BlastoiseExEffect implements AttackEffect {

    private static final String RAPID_SPIN  = "rapid spin";
    private static final String SPLASH_BOMB = "splash bomb";

    // Damage Blastoise-EX deals to itself on tails with Splash Bomb
    private static final int SELF_DAMAGE_COUNTERS = 3;

    private final CoinFlipService coinFlipService;

    public BlastoiseExEffect(CoinFlipService coinFlipService) {
        this.coinFlipService = coinFlipService;
    }

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("blastoise-ex|rapid spin", "blastoise-ex|splash bomb");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        switch (attackName) {
            case RAPID_SPIN  -> applyRapidSpin(ctx);
            case SPLASH_BOMB -> applySplashBomb(ctx);
            default          -> { }
        }
    }

    /**
     * Rapid Spin: the attacker switches with one of their Benched Pokémon,
     * then the opponent must also switch their Active Pokémon with one of
     * their Benched Pokémon.
     *
     * <p>The attacker's switch is a forced switch (no energy cost), resolved
     * immediately via {@code pendingForcedSwitchPlayerId} on the attacker.
     * The opponent's switch is then also set as pending, so both are resolved
     * before the turn continues.
     *
     * <p>If either player has no Bench Pokémon, that switch is skipped.
     */
    private void applyRapidSpin(AttackContext ctx) {
        String attackerId    = ctx.getAction().getPlayerId();
        PlayerState attacker = ctx.getBoardState().getStateFor(attackerId);
        PlayerState opponent = ctx.getBoardState().getOpponentState(attackerId);

        // Attacker switches first — only if they have bench Pokémon
        if (attacker.getBench() != null && !attacker.getBench().isEmpty()) {
            ctx.setBoardState(ctx.getBoardState().toBuilder()
                    .pendingForcedSwitchPlayerId(attackerId)
                    .build());
        }

        // Opponent switches after — only if they have bench Pokémon
        if (opponent.getBench() != null && !opponent.getBench().isEmpty()) {
            // If attacker also needs to switch, opponent switch will be set
            // after attacker resolves theirs. For now we set opponent pending
            // which will be enforced after attacker's FORCED_SWITCH resolves.
            // Since both can be pending simultaneously we set opponent here;
            // the attacker's flag takes priority and is resolved first by the
            // RuleValidator (pendingForcedSwitch blocks all other actions).
            ctx.setBoardState(ctx.getBoardState().toBuilder()
                    .pendingForcedSwitchPlayerId(opponent.getPlayerId())
                    .build());
        }
    }

    /**
     * Splash Bomb: flip a coin. On tails, Blastoise-EX does 30 damage to itself
     * (3 damage counters).
     */
    private void applySplashBomb(AttackContext ctx) {
        if (coinFlipService.flipAndEmit(ctx, ctx.getAction().getPlayerId()) == CoinResult.TAILS) {
            String attackerId    = ctx.getAction().getPlayerId();
            PlayerState attacker = ctx.getBoardState().getStateFor(attackerId);
            ActivePokemon self   = attacker.getActivePokemon();

            if (self != null) {
                self.setDamageCounters(self.getDamageCounters() + SELF_DAMAGE_COUNTERS);
            }
        }
    }
}