package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import com.pokemon.tcg.engine.CoinFlipService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MalamarEffect implements AttackEffect {

    private static final String MENTAL_TRASH    = "mental trash";
    private static final String DISTORTION_BEAM = "distortion beam";
    private static final int    MENTAL_TRASH_FLIPS = 4;

    private final CoinFlipService coinFlipService;

    public MalamarEffect(CoinFlipService coinFlipService) {
        this.coinFlipService = coinFlipService;
    }

    @Override
    public List<String> getSupportedAttacks() {
        return List.of("malamar|mental trash", "malamar|distortion beam");
    }

    @Override
    public void apply(AttackContext ctx) {
        String attackName = ctx.getAttackName() != null
                ? ctx.getAttackName().toLowerCase()
                : "";

        switch (attackName) {
            case MENTAL_TRASH    -> applyMentalTrash(ctx);
            case DISTORTION_BEAM -> applyDistortionBeam(ctx);
            default               -> { }
        }
    }

    /**
     * Mental Trash: your opponent flips 4 coins. For each tails, he or she
     * discards a card from his or her hand.
     * <p>
     * The opponent (defender) chooses which cards to discard, not the
     * attacker. Since only the attacker sent DECLARE_ATTACK, this effect
     * suspends the turn by setting pendingHandDiscardPlayerId/Count on the
     * BoardState. TurnManager.handleDeclareAttack detects this pending
     * state and does not advance the turn until the defender sends
     * DISCARD_FROM_HAND with their chosen cardIds.
     */
    private void applyMentalTrash(AttackContext ctx) {
        String attackerId    = ctx.getAction().getPlayerId();
        PlayerState opponent = ctx.getBoardState().getOpponentState(attackerId);

        int tails = 0;
        for (int i = 0; i < MENTAL_TRASH_FLIPS; i++) {
            if (coinFlipService.flipAndEmit(ctx, attackerId) == CoinResult.TAILS) {
                tails++;
            }
        }

        if (tails == 0) return;

        int handSize = opponent.getHand() != null ? opponent.getHand().size() : 0;
        int discardCount = Math.min(tails, handSize);
        if (discardCount == 0) return;

        ctx.setBoardState(ctx.getBoardState().toBuilder()
                .pendingHandDiscardPlayerId(opponent.getPlayerId())
                .pendingHandDiscardCount(discardCount)
                .build());
    }

    /**
     * Distortion Beam: flip a coin. If heads, your opponent's Active Pokémon
     * is now Asleep. If tails, your opponent's Active Pokémon is now Confused.
     */
    private void applyDistortionBeam(AttackContext ctx) {
        String attackerId    = ctx.getAction().getPlayerId();
        PlayerState opponent = ctx.getBoardState().getOpponentState(attackerId);
        ActivePokemon defender = opponent.getActivePokemon();

        if (defender == null) return;

        CoinResult result = coinFlipService.flipAndEmit(ctx, attackerId);
        SpecialCondition condition = result == CoinResult.HEADS
                ? SpecialCondition.ASLEEP
                : SpecialCondition.CONFUSED;

        defender.getConditions().remove(SpecialCondition.ASLEEP);
        defender.getConditions().remove(SpecialCondition.CONFUSED);
        defender.getConditions().remove(SpecialCondition.PARALYZED);
        defender.getConditions().add(condition);
    }
}