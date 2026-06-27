package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import com.pokemon.tcg.domain.strategy.attack.DamageModifier;
import com.pokemon.tcg.engine.CoinFlipService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Component
public class CorsolaEffect implements AttackEffect {

    private static final String REFRESH    = "refresh";
    private static final String SPINY_RUSH = "spiny rush";

    // 30 damage healed = 3 counters removed
    private static final int HEAL_COUNTERS      = 3;
    private static final int SPINY_RUSH_BONUS   = 20;

    private final CoinFlipService coinFlipService;

    public CorsolaEffect(CoinFlipService coinFlipService) {
        this.coinFlipService = coinFlipService;
    }

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("corsola|refresh", "corsola|spiny rush");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        switch (attackName) {
            case REFRESH    -> applyRefresh(ctx);
            case SPINY_RUSH -> applySpinyRush(ctx);
            default         -> { }
        }
    }

    /**
     * Refresh: heal 30 damage (remove 3 counters, minimum 0) and remove all
     * Special Conditions from Corsola.
     */
    private void applyRefresh(AttackContext ctx) {
        String attackerId    = ctx.getAction().getPlayerId();
        PlayerState attacker = ctx.getBoardState().getStateFor(attackerId);
        ActivePokemon corsola = attacker.getActivePokemon();

        if (corsola == null) return;

        // Heal 30 damage — minimum 0
        int healed = Math.max(0, corsola.getDamageCounters() - HEAL_COUNTERS);
        corsola.setDamageCounters(healed);

        // Remove all Special Conditions
        corsola.setConditions(new HashSet<>());
    }

    /**
     * Spiny Rush: flip coins until tails. This attack does 20 more damage
     * for each heads result.
     */
    private void applySpinyRush(AttackContext ctx) {
        int heads = 0;
        while (coinFlipService.flip() == CoinResult.HEADS) {
            heads++;
        }

        if (heads > 0) {
            List<DamageModifier> modifiers = new ArrayList<>(
                    ctx.getModifiers() != null ? ctx.getModifiers() : new ArrayList<>());
            modifiers.add(new DamageModifier(
                    "spiny-rush-heads", heads * SPINY_RUSH_BONUS, true));
            ctx.setModifiers(modifiers);
        }
    }
}