package com.pokemon.tcg.engine.attack.steps;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.engine.CardLookupPort;
import com.pokemon.tcg.engine.VictoryConditionChecker;
import com.pokemon.tcg.engine.ability.PassiveAbilityRegistry;
import com.pokemon.tcg.engine.attack.AttackChain;
import com.pokemon.tcg.engine.attack.AttackContext;
import com.pokemon.tcg.engine.attack.AttackStep;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Step 7 — Handles post-damage effects: checks for knockouts using the real
 * card HP resolved before the pipeline started, moves KO'd Pokémon to discard,
 * awards prize cards to the attacker, flags a pending bench choice if the
 * defender still has Pokémon on the bench, and triggers passive abilities
 * on the defending Pokémon (e.g. Spiky Shield).
 */
@Component
public class PostDamageEffectStep implements AttackStep {

    private final VictoryConditionChecker victoryChecker;
    private final PassiveAbilityRegistry  passiveAbilityRegistry;
    private final CardLookupPort          cardLookupPort;

    public PostDamageEffectStep(VictoryConditionChecker victoryChecker,
                                PassiveAbilityRegistry passiveAbilityRegistry,
                                CardLookupPort cardLookupPort) {
        this.victoryChecker         = victoryChecker;
        this.passiveAbilityRegistry = passiveAbilityRegistry;
        this.cardLookupPort         = cardLookupPort;
    }

    @Override
    public void execute(AttackContext ctx, AttackChain chain) {
        String attackerId = ctx.getAction().getPlayerId();
        String defenderId = ctx.getBoardState().getOpponentState(attackerId).getPlayerId();

        PlayerState attackerState = ctx.getBoardState().getStateFor(attackerId);
        PlayerState defenderState = ctx.getBoardState().getStateFor(defenderId);

        ActivePokemon defender = defenderState.getActivePokemon();

        // Skip all post-damage effects if the defender is invulnerable
        boolean defenderInvulnerable = defender != null
                && defender.getActiveEffects() != null
                && defender.getActiveEffects().contains(PokemonEffect.INVULNERABLE);

        if (!defenderInvulnerable && defender != null && ctx.getDamageToApply() > 0) {
            // Save defender reference before KO check — passive abilities must
            // fire even if the Pokémon is knocked out during this step
            ActivePokemon defenderSnapshot = defender;

            checkAndHandleKnockout(ctx, attackerState, defenderState, defender);

            // Trigger passive ability on the defender after KO resolution
            triggerPassiveAbility(ctx, defenderSnapshot);
        }

        chain.next(ctx);
    }

    // ── Passive ability trigger ────────────────────────────────────────────────

    /**
     * Looks up the defender's card name and triggers its passive ability
     * if one is registered, passing the preserved defender snapshot.
     */
    private void triggerPassiveAbility(AttackContext ctx, ActivePokemon defender) {
        cardLookupPort.findCardById(defender.getCardId()).ifPresent(card -> {
            passiveAbilityRegistry.findAbility(card.getName())
                    .ifPresent(ability -> ability.onDamageReceived(ctx, defender));
        });
    }

    // ── KO handling ───────────────────────────────────────────────────────────

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
                defenderState.getDiscard() != null
                        ? defenderState.getDiscard() : new ArrayList<>());
        discard.add(knockedOut.getCardId());
        if (knockedOut.getAttachedEnergyIds() != null) {
            discard.addAll(knockedOut.getAttachedEnergyIds());
        }
        defenderState.setDiscard(discard);
        defenderState.setActivePokemon(null);

        // Attacker takes one prize card into their hand
        List<String> prizes = new ArrayList<>(
                attackerState.getPrizes() != null
                        ? attackerState.getPrizes() : new ArrayList<>());
        if (!prizes.isEmpty()) {
            String takenPrize = prizes.remove(0);
            attackerState.setPrizes(prizes);

            // Prize goes directly to the attacker's hand
            List<String> hand = new ArrayList<>(
                    attackerState.getHand() != null
                            ? attackerState.getHand() : new ArrayList<>());
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
