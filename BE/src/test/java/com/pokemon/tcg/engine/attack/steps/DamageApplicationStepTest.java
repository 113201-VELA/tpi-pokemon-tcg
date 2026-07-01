package com.pokemon.tcg.engine.attack.steps;

import com.pokemon.tcg.domain.model.card.Attack;
import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.card.TypeModifier;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.engine.DamageCalculator;
import com.pokemon.tcg.engine.attack.AttackChain;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.fixtures.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class DamageApplicationStepTest {

    private DamageCalculator damageCalculator;
    private DamageApplicationStep step;
    private AttackChain chain;

    @BeforeEach
    void setUp() {
        damageCalculator = mock(DamageCalculator.class);
        step  = new DamageApplicationStep(damageCalculator);
        chain = mock(AttackChain.class);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private AttackContext buildCtx(ActivePokemon attacker, ActivePokemon defender, Attack attack) {
        PlayerState p1 = TestDataBuilder.playerState(TestDataBuilder.PLAYER_1,
                new ArrayList<>(), new ArrayList<>());
        p1.setActivePokemon(attacker);

        PlayerState p2 = TestDataBuilder.playerState(TestDataBuilder.PLAYER_2,
                new ArrayList<>(), new ArrayList<>());
        p2.setActivePokemon(defender);

        BoardState board = TestDataBuilder.boardState(p1, p2);
        GameAction action = TestDataBuilder.action(
                GameActionType.DECLARE_ATTACK, TestDataBuilder.PLAYER_1);

        return AttackContext.builder()
                .boardState(board)
                .action(action)
                .attack(attack)
                .modifiers(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
    }

    private Attack attack(int baseDamage) {
        return Attack.builder()
                .name("Tackle")
                .damage(String.valueOf(baseDamage))
                .cost(new ArrayList<>())
                .build();
    }

    private ActivePokemon pokemon(String cardId) {
        return ActivePokemon.builder()
                .instanceId(TestDataBuilder.instanceId())
                .cardId(cardId)
                .damageCounters(0)
                .types(List.of(EnergyType.FIRE))
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .activeEffects(new ArrayList<>())
                .build();
    }

    // ── tests ─────────────────────────────────────────────────────────────────

    @Test
    void nullAttack_shouldSkipAndCallChain() {
        AttackContext ctx = buildCtx(pokemon("xy1-1"), pokemon("xy1-2"), null);

        step.execute(ctx, chain);

        verify(damageCalculator, never()).calculate(any(), any(), anyInt(), any(), any(), anyBoolean());
        verify(chain).next(ctx);
    }

    @Test
    void zeroDamageAttack_shouldSkipAndCallChain() {
        AttackContext ctx = buildCtx(pokemon("xy1-1"), pokemon("xy1-2"), attack(0));

        step.execute(ctx, chain);

        verify(damageCalculator, never()).calculate(any(), any(), anyInt(), any(), any(), anyBoolean());
        verify(chain).next(ctx);
    }

    @Test
    void normalDamage_shouldApplyCountersToDefender() {
        ActivePokemon attacker = pokemon("xy1-1");
        ActivePokemon defender = pokemon("xy1-2");
        AttackContext ctx = buildCtx(attacker, defender, attack(50));

        when(damageCalculator.calculate(any(), any(), eq(50), any(), any(), eq(false))).thenReturn(50);
        when(damageCalculator.toCounters(50)).thenReturn(5);

        step.execute(ctx, chain);

        assertThat(defender.getDamageCounters()).isEqualTo(5);
        assertThat(ctx.getDamageToApply()).isEqualTo(50);
        verify(chain).next(ctx);
    }

    @Test
    void normalDamage_shouldEmitDamageAppliedEvent() {
        ActivePokemon attacker = pokemon("xy1-1");
        ActivePokemon defender = pokemon("xy1-2");
        AttackContext ctx = buildCtx(attacker, defender, attack(80));

        when(damageCalculator.calculate(any(), any(), eq(80), any(), any(), eq(false))).thenReturn(160);
        when(damageCalculator.toCounters(160)).thenReturn(16);

        step.execute(ctx, chain);

        assertThat(ctx.getEvents())
                .anyMatch(e -> e.getType() == GameEventType.DAMAGE_APPLIED
                        && ((Integer) e.getData().get("damage")) == 160);
    }

    @Test
    void invulnerableDefender_shouldTakeZeroDamage() {
        ActivePokemon attacker = pokemon("xy1-1");
        ActivePokemon defender = pokemon("xy1-2");
        defender.setActiveEffects(new ArrayList<>(List.of(PokemonEffect.INVULNERABLE)));
        AttackContext ctx = buildCtx(attacker, defender, attack(80));

        when(damageCalculator.calculate(any(), any(), eq(80), any(), any(), eq(false))).thenReturn(80);
        when(damageCalculator.toCounters(0)).thenReturn(0);

        step.execute(ctx, chain);

        assertThat(defender.getDamageCounters()).isZero();
        assertThat(ctx.getDamageToApply()).isZero();
        verify(chain).next(ctx);
    }

    @Test
    void hardenEffect_damageBelowThreshold_shouldTakeZeroDamage() {
        ActivePokemon attacker = pokemon("xy1-1");
        ActivePokemon defender = pokemon("xy1-2");
        defender.setActiveEffects(new ArrayList<>(List.of(PokemonEffect.HARDEN)));
        AttackContext ctx = buildCtx(attacker, defender, attack(60));

        when(damageCalculator.calculate(any(), any(), eq(60), any(), any(), eq(false))).thenReturn(60);
        when(damageCalculator.toCounters(0)).thenReturn(0);

        step.execute(ctx, chain);

        assertThat(defender.getDamageCounters()).isZero();
    }

    @Test
    void hardenEffect_damageAboveThreshold_shouldApplyNormally() {
        ActivePokemon attacker = pokemon("xy1-1");
        ActivePokemon defender = pokemon("xy1-2");
        defender.setActiveEffects(new ArrayList<>(List.of(PokemonEffect.HARDEN)));
        AttackContext ctx = buildCtx(attacker, defender, attack(70));

        when(damageCalculator.calculate(any(), any(), eq(70), any(), any(), eq(false))).thenReturn(70);
        when(damageCalculator.toCounters(70)).thenReturn(7);

        step.execute(ctx, chain);

        assertThat(defender.getDamageCounters()).isEqualTo(7);
    }

    @Test
    void nullDefender_shouldSkipDamageAndCallChain() {
        PlayerState p1 = TestDataBuilder.playerState(TestDataBuilder.PLAYER_1,
                new ArrayList<>(), new ArrayList<>());
        p1.setActivePokemon(pokemon("xy1-1"));
        PlayerState p2 = TestDataBuilder.playerState(TestDataBuilder.PLAYER_2,
                new ArrayList<>(), new ArrayList<>());
        p2.setActivePokemon(null);

        BoardState board = TestDataBuilder.boardState(p1, p2);
        GameAction action = TestDataBuilder.action(
                GameActionType.DECLARE_ATTACK, TestDataBuilder.PLAYER_1);
        AttackContext ctx = AttackContext.builder()
                .boardState(board).action(action).attack(attack(50))
                .modifiers(new ArrayList<>()).events(new ArrayList<>()).build();

        step.execute(ctx, chain);

        verify(damageCalculator, never()).calculate(any(), any(), anyInt(), any(), any(), anyBoolean());
        verify(chain).next(ctx);
    }

    @Test
    void existingCounters_shouldAccumulate() {
        ActivePokemon attacker = pokemon("xy1-1");
        ActivePokemon defender = pokemon("xy1-2");
        defender.setDamageCounters(3);
        AttackContext ctx = buildCtx(attacker, defender, attack(50));

        when(damageCalculator.calculate(any(), any(), eq(50), any(), any(), eq(false))).thenReturn(50);
        when(damageCalculator.toCounters(50)).thenReturn(5);

        step.execute(ctx, chain);

        assertThat(defender.getDamageCounters()).isEqualTo(8); // 3 + 5
    }

    // ── ignoreResistance flag (Puncture-style attacks) ──────────────────────────

    @Test
    void ignoreResistance_shouldSkipResistanceCalculation() {
        ActivePokemon attacker = pokemon("xy1-1");
        ActivePokemon defender = pokemon("xy1-2");
        AttackContext ctx = buildCtx(attacker, defender, attack(20));
        ctx.setIgnoreResistance(true);

        when(damageCalculator.calculate(any(), any(), eq(20), any(), any(), eq(true)))
                .thenReturn(20);
        when(damageCalculator.toCounters(20)).thenReturn(2);

        step.execute(ctx, chain);

        assertThat(defender.getDamageCounters()).isEqualTo(2);
        verify(damageCalculator).calculate(any(), any(), eq(20), any(), any(), eq(true));
    }
}