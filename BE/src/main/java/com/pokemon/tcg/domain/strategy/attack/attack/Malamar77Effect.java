package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class Malamar77Effect implements AttackEffect {

    private static final String MENTAL_PANIC = "mental panic";
    private static final String PUNCTURE     = "puncture";

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("malamar|mental panic", "malamar|puncture");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        switch (attackName) {
            case MENTAL_PANIC -> applyMentalPanic(ctx);
            case PUNCTURE     -> applyPuncture(ctx);
            default            -> { }
        }
    }

    /**
     * Mental Panic: if the Defending Pokémon tries to attack during your
     * opponent's next turn, your opponent flips a coin. If tails, that
     * attack does nothing.
     * <p>
     * Base damage (30) is handled by the pipeline via the card's Attack
     * data — this effect only marks the defender with
     * {@code pendingAttackFailChance}. ConfusionCheckStep consumes the flag
     * and flips the coin the next time that Pokémon attempts to attack.
     */
    private void applyMentalPanic(AttackContext ctx) {
        String attackerId      = ctx.getAction().getPlayerId();
        PlayerState opponent   = ctx.getBoardState().getOpponentState(attackerId);
        ActivePokemon defender = opponent.getActivePokemon();

        if (defender == null) return;

        defender.setPendingAttackFailChance(true);
    }

    /** Puncture: this attack's damage isn't affected by Resistance. */
    private void applyPuncture(AttackContext ctx) {
        ctx.setIgnoreResistance(true);
    }
}