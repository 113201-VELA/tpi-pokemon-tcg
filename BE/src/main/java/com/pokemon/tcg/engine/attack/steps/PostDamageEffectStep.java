package com.pokemon.tcg.engine.attack.steps;

import com.pokemon.tcg.engine.VictoryConditionChecker;
import com.pokemon.tcg.engine.attack.AttackChain;
import com.pokemon.tcg.engine.attack.AttackContext;
import com.pokemon.tcg.engine.attack.AttackStep;
import com.pokemon.tcg.domain.model.game.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Step 7 — Handles post-damage effects: checks for knockouts using the real
 * card HP resolved before the pipeline started, moves KO'd Pokémon to discard,
 * awards prize cards to the attacker, and flags a pending bench choice if the
 * defender still has Pokémon on the bench to replace the KO'd Active.
 */
@Component
public class PostDamageEffectStep implements AttackStep {

    private final VictoryConditionChecker victoryChecker;

    public PostDamageEffectStep(VictoryConditionChecker victoryChecker) {
        this.victoryChecker = victoryChecker;
    }

    @Override
    public void execute(AttackContext ctx, AttackChain chain) {
        String attackerId = ctx.getAction().getPlayerId();
        String defenderId = ctx.getBoardState().getOpponentState(attackerId).getPlayerId();

        PlayerState attackerState = ctx.getBoardState().getStateFor(attackerId);
        PlayerState defenderState = ctx.getBoardState().getStateFor(defenderId);

        ActivePokemon defender = defenderState.getActivePokemon();

        boolean defenderInvulnerable = defender != null
                && defender.getActiveEffects() != null
                && defender.getActiveEffects().contains(PokemonEffect.INVULNERABLE);

        if (!defenderInvulnerable && defender != null && ctx.getDamageToApply() > 0) {
            checkAndHandleKnockout(ctx, attackerState, defenderState, defender);
        }

        chain.next(ctx);
    }

    private void checkAndHandleKnockout(AttackContext ctx,
                                        PlayerState attackerState,
                                        PlayerState defenderState,
                                        ActivePokemon defender) {
        int maxHp = ctx.getDefenderMaxHp();
        // If maxHp could not be resolved (card not found), skip KO check
        if (maxHp <= 0) return;

        if (defender.getDamageCounters() * 10 >= maxHp) {
            handleKnockout(ctx, attackerState, defenderState, defender);
        }
    }

    private void handleKnockout(AttackContext ctx,
                                PlayerState attackerState,
                                PlayerState defenderState,
                                ActivePokemon knockedOut) {
        // Move KO'd Pokémon and all attached cards to defender's discard
        List<String> discard = new ArrayList<>(
                defenderState.getDiscard() != null ? defenderState.getDiscard() : new ArrayList<>());
        discard.add(knockedOut.getCardId());
        if (knockedOut.getAttachedEnergyIds() != null) {
            discard.addAll(knockedOut.getAttachedEnergyIds());
        }
        defenderState.setDiscard(discard);
        defenderState.setActivePokemon(null);

        // Attacker takes one prize card into their hand
        List<String> prizes = new ArrayList<>(
                attackerState.getPrizes() != null ? attackerState.getPrizes() : new ArrayList<>());
        if (!prizes.isEmpty()) {
            String takenPrize = prizes.remove(0);
            attackerState.setPrizes(prizes);

            // Prize goes directly to the attacker's hand
            List<String> hand = new ArrayList<>(
                    attackerState.getHand() != null ? attackerState.getHand() : new ArrayList<>());
            hand.add(takenPrize);
            attackerState.setHand(hand);
        }

        ctx.addEvent(GameEvent.builder()
                .type(GameEventType.POKEMON_KNOCKED_OUT)
                .gameId(ctx.getBoardState().getGameId())
                .playerId(ctx.getAction().getPlayerId())
                .turnNumber(ctx.getBoardState().getTurnNumber())
                .data(Map.of("knockedOutCardId", knockedOut.getCardId()))
                .occurredAt(Instant.now())
                .build());

        ctx.addEvent(GameEvent.builder()
                .type(GameEventType.PRIZE_TAKEN)
                .gameId(ctx.getBoardState().getGameId())
                .playerId(ctx.getAction().getPlayerId())
                .turnNumber(ctx.getBoardState().getTurnNumber())
                .data(Map.of("prizesRemaining", attackerState.getPrizes().size()))
                .occurredAt(Instant.now())
                .build());

        // If the defender still has bench Pokémon, flag a pending bench choice.
        // The turn will not advance until they send CHOOSE_BENCH_POKEMON.
        // If the bench is empty, VictoryConditionChecker will detect the win condition.
        boolean defenderHasBench = defenderState.getBench() != null
                && !defenderState.getBench().isEmpty();
        if (defenderHasBench) {
            ctx.setBoardState(ctx.getBoardState().toBuilder()
                    .pendingBenchChoicePlayerId(defenderState.getPlayerId())
                    .build());
        }
    }
}