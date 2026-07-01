package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import com.pokemon.tcg.domain.strategy.attack.DamageModifier;
import com.pokemon.tcg.engine.CoinFlipService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CloysterEffect implements AttackEffect {

    private static final String CLAMP_CRUSH  = "clamp crush";
    private static final String SPIKE_CANNON = "spike cannon";

    private static final int SPIKE_CANNON_FLIPS       = 5;
    private static final int SPIKE_CANNON_DAMAGE_PER_HEAD = 30;

    private final CoinFlipService coinFlipService;

    public CloysterEffect(CoinFlipService coinFlipService) {
        this.coinFlipService = coinFlipService;
    }

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("cloyster|clamp crush", "cloyster|spike cannon");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        switch (attackName) {
            case CLAMP_CRUSH  -> applyClampCrush(ctx);
            case SPIKE_CANNON -> applySpikeCannon(ctx);
            default           -> { }
        }
    }

    /**
     * Clamp Crush: flip a coin. On heads, the opponent's Active Pokémon is
     * Paralyzed and one Energy attached to it is discarded.
     *
     * <p>Paralyzed replaces Asleep and Confused (rotation conditions).
     * The first Energy in the attached list is discarded — the frontend
     * should let the player choose which one, but since SelectionStep is
     * not yet fully implemented we discard the first available one.
     */
    private void applyClampCrush(AttackContext ctx) {
        if (coinFlipService.flipAndEmit(ctx, ctx.getAction().getPlayerId()) != CoinResult.HEADS) return;

        String attackerId    = ctx.getAction().getPlayerId();
        PlayerState opponent = ctx.getBoardState().getOpponentState(attackerId);
        ActivePokemon defender = opponent.getActivePokemon();

        if (defender == null) return;

        // Apply Paralyzed — replaces Asleep and Confused per rulebook
        defender.getConditions().remove(SpecialCondition.ASLEEP);
        defender.getConditions().remove(SpecialCondition.CONFUSED);
        defender.getConditions().add(SpecialCondition.PARALYZED);

        // Discard one Energy attached to the defender
        List<String> energies = defender.getAttachedEnergyIds();
        if (energies == null || energies.isEmpty()) return;

        List<String> mutableEnergies = new ArrayList<>(energies);
        String discarded = mutableEnergies.remove(0);
        defender.setAttachedEnergyIds(mutableEnergies);

        List<String> discard = new ArrayList<>(
                opponent.getDiscard() != null ? opponent.getDiscard() : new ArrayList<>());
        discard.add(discarded);
        opponent.setDiscard(discard);
    }

    /**
     * Spike Cannon: flip 5 coins. This attack does 30 damage times the number
     * of heads. The extra damage is added as a modifier on top of the base 30.
     */
    private void applySpikeCannon(AttackContext ctx) {
        int heads = 0;
        for (int i = 0; i < SPIKE_CANNON_FLIPS; i++) {
            if (coinFlipService.flipAndEmit(ctx, ctx.getAction().getPlayerId()) == CoinResult.HEADS) {
                heads++;
            }
        }

        // The base damage of Spike Cannon is 30×heads total.
        // The pipeline already applies base damage from the card (which is "30×",
        // treated as 0 since it's not a fixed number), so we set the full damage
        // as a modifier here.
        List<DamageModifier> modifiers = new ArrayList<>(
                ctx.getModifiers() != null ? ctx.getModifiers() : new ArrayList<>());
        modifiers.add(new DamageModifier(
                "spike-cannon-heads", heads * SPIKE_CANNON_DAMAGE_PER_HEAD, true));
        ctx.setModifiers(modifiers);
    }
}