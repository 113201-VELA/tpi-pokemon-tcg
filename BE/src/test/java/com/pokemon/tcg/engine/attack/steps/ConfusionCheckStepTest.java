package com.pokemon.tcg.engine.attack.steps;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.engine.CoinFlipService;
import com.pokemon.tcg.engine.attack.AttackChain;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.fixtures.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ConfusionCheckStepTest {

    private CoinFlipService coinFlipService;
    private ConfusionCheckStep step;
    private AttackChain chain;

    @BeforeEach
    void setUp() {
        coinFlipService = mock(CoinFlipService.class);
        step  = new ConfusionCheckStep(coinFlipService);
        chain = mock(AttackChain.class);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private AttackContext buildCtx(ActivePokemon attacker) {
        PlayerState p1 = TestDataBuilder.playerState(TestDataBuilder.PLAYER_1,
                new ArrayList<>(), new ArrayList<>());
        p1.setActivePokemon(attacker);

        PlayerState p2 = TestDataBuilder.playerState(TestDataBuilder.PLAYER_2,
                new ArrayList<>(), new ArrayList<>());
        p2.setActivePokemon(ActivePokemon.builder()
                .instanceId(TestDataBuilder.instanceId())
                .cardId("xy1-2")
                .damageCounters(0)
                .build());

        BoardState board = TestDataBuilder.boardState(p1, p2);
        GameAction action = TestDataBuilder.action(
                GameActionType.DECLARE_ATTACK, TestDataBuilder.PLAYER_1, "attackName", "Tackle");

        return AttackContext.builder()
                .boardState(board)
                .action(action)
                .events(new ArrayList<>())
                .build();
    }

    private ActivePokemon attackerWithCondition(SpecialCondition condition) {
        return ActivePokemon.builder()
                .instanceId(TestDataBuilder.instanceId())
                .cardId("xy1-1")
                .damageCounters(0)
                .conditions(condition != null ? Set.of(condition) : new HashSet<>())
                .build();
    }

    private ActivePokemon attackerWith(SpecialCondition condition, boolean pendingAttackFailChance) {
        return ActivePokemon.builder()
                .instanceId(TestDataBuilder.instanceId())
                .cardId("xy1-1")
                .damageCounters(0)
                .conditions(condition != null ? Set.of(condition) : new HashSet<>())
                .pendingAttackFailChance(pendingAttackFailChance)
                .build();
    }

    // ── tests: CONFUSED ──────────────────────────────────────────────────────

    @Test
    void notConfused_shouldCallChainWithoutFlip() {
        AttackContext ctx = buildCtx(attackerWithCondition(null));

        step.execute(ctx, chain);

        verify(coinFlipService, never()).flip();
        verify(chain).next(ctx);
        assertThat(ctx.isCancelled()).isFalse();
    }

    @Test
    void confused_heads_shouldContinueChain() {
        when(coinFlipService.flip()).thenReturn(CoinResult.HEADS);
        ActivePokemon attacker = attackerWithCondition(SpecialCondition.CONFUSED);
        AttackContext ctx = buildCtx(attacker);

        step.execute(ctx, chain);

        verify(chain).next(ctx);
        assertThat(ctx.isCancelled()).isFalse();
        assertThat(attacker.getDamageCounters()).isZero();
    }

    /**
     * UPDATED: Confused + tails no longer cancels the pipeline. The step now
     * lets the chain continue (with 30 self-damage staged via
     * ctx.setDamageToApply / ctx.setConfusionSelfDamage) so a later step
     * (PostDamageEffectStep) can detect if the attacker KO'd themselves.
     * See the "Do NOT cancel" comment in ConfusionCheckStep.
     */
    @Test
    void confused_tails_shouldAdd3CountersAndContinueChain() {
        when(coinFlipService.flip()).thenReturn(CoinResult.TAILS);
        ActivePokemon attacker = attackerWithCondition(SpecialCondition.CONFUSED);
        AttackContext ctx = buildCtx(attacker);

        step.execute(ctx, chain);

        verify(chain).next(ctx);
        assertThat(ctx.isCancelled()).isFalse();
        assertThat(attacker.getDamageCounters()).isEqualTo(3);
        assertThat(ctx.isConfusionSelfDamage()).isTrue();
        assertThat(ctx.getDamageToApply()).isEqualTo(30);
    }

    /**
     * UPDATED: event type/key now match actual ConfusionCheckStep output —
     * GameEventType.COIN_FLIP with data key "result" (NOT
     * SPECIAL_CONDITION_APPLIED / "coinResult", which is what the Mental
     * Panic branch below this one uses instead).
     */
    @Test
    void confused_tails_shouldAddCoinFlipEvent() {
        when(coinFlipService.flip()).thenReturn(CoinResult.TAILS);
        AttackContext ctx = buildCtx(attackerWithCondition(SpecialCondition.CONFUSED));

        step.execute(ctx, chain);

        assertThat(ctx.getEvents())
                .anyMatch(e -> e.getType() == GameEventType.COIN_FLIP
                        && "TAILS".equals(e.getData().get("result")));
    }

    @Test
    void confused_heads_shouldAddCoinFlipEvent() {
        when(coinFlipService.flip()).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildCtx(attackerWithCondition(SpecialCondition.CONFUSED));

        step.execute(ctx, chain);

        assertThat(ctx.getEvents())
                .anyMatch(e -> e.getType() == GameEventType.COIN_FLIP
                        && "HEADS".equals(e.getData().get("result")));
    }

    @Test
    void confused_tails_existingCounters_shouldAccumulate() {
        when(coinFlipService.flip()).thenReturn(CoinResult.TAILS);
        ActivePokemon attacker = attackerWithCondition(SpecialCondition.CONFUSED);
        attacker.setDamageCounters(2);
        AttackContext ctx = buildCtx(attacker);

        step.execute(ctx, chain);

        assertThat(attacker.getDamageCounters()).isEqualTo(5); // 2 + 3
    }

    @Test
    void nullActivePokemon_shouldCallChainWithoutFlip() {
        PlayerState p1 = TestDataBuilder.playerState(TestDataBuilder.PLAYER_1,
                new ArrayList<>(), new ArrayList<>());
        p1.setActivePokemon(null);
        PlayerState p2 = TestDataBuilder.playerState(TestDataBuilder.PLAYER_2,
                new ArrayList<>(), new ArrayList<>());
        BoardState board = TestDataBuilder.boardState(p1, p2);
        GameAction action = TestDataBuilder.action(
                GameActionType.DECLARE_ATTACK, TestDataBuilder.PLAYER_1);
        AttackContext ctx = AttackContext.builder()
                .boardState(board).action(action).events(new ArrayList<>()).build();

        step.execute(ctx, chain);

        verify(coinFlipService, never()).flip();
        verify(chain).next(ctx);
    }

    // ── tests: pendingAttackFailChance (Mental Panic) ───────────────────────────

    @Test
    void pendingAttackFailChance_heads_shouldContinueChainAndClearFlag() {
        when(coinFlipService.flip()).thenReturn(CoinResult.HEADS);
        ActivePokemon attacker = attackerWith(null, true);
        AttackContext ctx = buildCtx(attacker);

        step.execute(ctx, chain);

        verify(chain).next(ctx);
        assertThat(ctx.isCancelled()).isFalse();
        assertThat(attacker.isPendingAttackFailChance()).isFalse();
    }

    @Test
    void pendingAttackFailChance_tails_shouldCancelAndClearFlag() {
        when(coinFlipService.flip()).thenReturn(CoinResult.TAILS);
        ActivePokemon attacker = attackerWith(null, true);
        AttackContext ctx = buildCtx(attacker);

        step.execute(ctx, chain);

        verify(chain, never()).next(any());
        assertThat(ctx.isCancelled()).isTrue();
        assertThat(attacker.isPendingAttackFailChance()).isFalse();
        // Unlike Confusion, Mental Panic does not add damage counters to the attacker
        assertThat(attacker.getDamageCounters()).isZero();
    }

    @Test
    void pendingAttackFailChance_tails_shouldAddCoinFlipEvent() {
        when(coinFlipService.flip()).thenReturn(CoinResult.TAILS);
        AttackContext ctx = buildCtx(attackerWith(null, true));

        step.execute(ctx, chain);

        assertThat(ctx.getEvents())
                .anyMatch(e -> e.getType() == GameEventType.COIN_FLIP
                        && "TAILS".equals(e.getData().get("result")));
    }

    @Test
    void notPendingAttackFailChance_shouldNotFlip() {
        AttackContext ctx = buildCtx(attackerWith(null, false));

        step.execute(ctx, chain);

        verify(coinFlipService, never()).flip();
        verify(chain).next(ctx);
    }

    /**
     * UPDATED: Confused + tails no longer short-circuits via cancel() — it
     * now continues the chain (see comment above). Mental Panic's check is
     * still never reached in this scenario because the Confused branch
     * returns immediately after handling its own tails case.
     */
    @Test
    void confusedAndPendingAttackFailChance_confusedTailsFirst_shouldContinueWithoutSecondFlip() {
        when(coinFlipService.flip()).thenReturn(CoinResult.TAILS);
        ActivePokemon attacker = attackerWith(SpecialCondition.CONFUSED, true);
        AttackContext ctx = buildCtx(attacker);

        step.execute(ctx, chain);

        verify(coinFlipService, times(1)).flip();
        assertThat(ctx.isCancelled()).isFalse();
        verify(chain).next(ctx);
        // Mental Panic flag survives untouched since its check was never reached
        assertThat(attacker.isPendingAttackFailChance()).isTrue();
    }

    @Test
    void confusedAndPendingAttackFailChance_confusedHeadsThenMentalPanicTails_shouldCancelOnSecondFlip() {
        when(coinFlipService.flip())
                .thenReturn(CoinResult.HEADS)  // confusion check passes
                .thenReturn(CoinResult.TAILS); // mental panic check fails
        ActivePokemon attacker = attackerWith(SpecialCondition.CONFUSED, true);
        AttackContext ctx = buildCtx(attacker);

        step.execute(ctx, chain);

        verify(coinFlipService, times(2)).flip();
        verify(chain, never()).next(any());
        assertThat(ctx.isCancelled()).isTrue();
        assertThat(attacker.isPendingAttackFailChance()).isFalse();
        // Confusion's heads path adds no counters; Mental Panic never adds counters either
        assertThat(attacker.getDamageCounters()).isZero();
    }

    @Test
    void confusedAndPendingAttackFailChance_bothHeads_shouldContinueChain() {
        when(coinFlipService.flip()).thenReturn(CoinResult.HEADS);
        ActivePokemon attacker = attackerWith(SpecialCondition.CONFUSED, true);
        AttackContext ctx = buildCtx(attacker);

        step.execute(ctx, chain);

        verify(coinFlipService, times(2)).flip();
        verify(chain).next(ctx);
        assertThat(ctx.isCancelled()).isFalse();
        assertThat(attacker.isPendingAttackFailChance()).isFalse();
    }
}