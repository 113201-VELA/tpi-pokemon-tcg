package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.DamageModifier;
import com.pokemon.tcg.engine.CoinFlipService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class MrMimeEffectTest {

    private CoinFlipService coinFlipService;
    private MrMimeEffect    effect;

    @BeforeEach
    void setUp() {
        coinFlipService = mock(CoinFlipService.class);
        effect = new MrMimeEffect(coinFlipService);
    }

    @Test
    void shouldSupportBothAttacks() {
        assertThat(effect.getSupportedAttacks())
                .containsExactly("mr. mime|massage", "mr. mime|slap down");
    }

    // ─── Massage ──────────────────────────────────────────────────────────────

    @Test
    void massage_shouldHealSixCounters_fromTargetBenchPokemon() {
        AttackContext ctx = buildMassageContext("bench-1", 8);

        effect.apply(ctx);

        BenchPokemon bench = ctx.getBoardState().getStateFor(PLAYER_1).getBench().get(0);
        assertThat(bench.getDamageCounters()).isEqualTo(2); // 8 - 6
    }

    @Test
    void massage_shouldClampAtZero_whenHealExceedsDamage() {
        AttackContext ctx = buildMassageContext("bench-1", 3);

        effect.apply(ctx);

        BenchPokemon bench = ctx.getBoardState().getStateFor(PLAYER_1).getBench().get(0);
        assertThat(bench.getDamageCounters()).isEqualTo(0);
    }

    @Test
    void massage_shouldDoNothing_whenNoTargetSpecified() {
        AttackContext ctx = buildContext("massage", Map.of("attackName", "massage"), true, 5);

        effect.apply(ctx);

        BenchPokemon bench = ctx.getBoardState().getStateFor(PLAYER_1).getBench().get(0);
        assertThat(bench.getDamageCounters()).isEqualTo(5);
    }

    @Test
    void massage_shouldDoNothing_whenTargetNotFoundOnBench() {
        AttackContext ctx = buildMassageContext("nonexistent-bench", 5);

        effect.apply(ctx);

        BenchPokemon bench = ctx.getBoardState().getStateFor(PLAYER_1).getBench().get(0);
        assertThat(bench.getDamageCounters()).isEqualTo(5);
    }

    @Test
    void massage_shouldDoNothing_whenNoBenchInPlay() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("attackName", "massage");
        payload.put("targetInstanceId", "bench-1");
        AttackContext ctx = buildContext("massage", payload, false, 0);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getBench()).isEmpty();
    }

    @Test
    void massage_shouldNotFlipCoins() {
        AttackContext ctx = buildMassageContext("bench-1", 5);

        effect.apply(ctx);

        verifyNoInteractions(coinFlipService);
    }

    // ─── Slap Down ────────────────────────────────────────────────────────────

    @Test
    void slapDown_shouldAdd40_whenBothHeads() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString()))
                .thenReturn(CoinResult.HEADS, CoinResult.HEADS);
        AttackContext ctx = buildContext("slap down",
                Map.of("attackName", "slap down"), false, 0);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).hasSize(1);
        DamageModifier mod = ctx.getModifiers().get(0);
        assertThat(mod.amount()).isEqualTo(40);
        assertThat(mod.beforeWeakness()).isTrue();
    }

    @Test
    void slapDown_shouldAdd20_whenOneHead() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString()))
                .thenReturn(CoinResult.HEADS, CoinResult.TAILS);
        AttackContext ctx = buildContext("slap down",
                Map.of("attackName", "slap down"), false, 0);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).hasSize(1);
        assertThat(ctx.getModifiers().get(0).amount()).isEqualTo(20);
    }

    @Test
    void slapDown_shouldAddNoModifier_whenBothTails() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString()))
                .thenReturn(CoinResult.TAILS, CoinResult.TAILS);
        AttackContext ctx = buildContext("slap down",
                Map.of("attackName", "slap down"), false, 0);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    @Test
    void slapDown_shouldAlwaysFlipExactlyTwoCoins() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString()))
                .thenReturn(CoinResult.TAILS, CoinResult.TAILS);
        AttackContext ctx = buildContext("slap down",
                Map.of("attackName", "slap down"), false, 0);

        effect.apply(ctx);

        verify(coinFlipService, times(2)).flipAndEmit(any(AttackContext.class), eq(PLAYER_1));
    }

    // ─── unknown attack ───────────────────────────────────────────────────────

    @Test
    void unknownAttack_shouldDoNothing() {
        AttackContext ctx = buildContext("unknown",
                Map.of("attackName", "unknown"), false, 0);

        effect.apply(ctx);

        verifyNoInteractions(coinFlipService);
        assertThat(ctx.getModifiers()).isEmpty();
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private AttackContext buildMassageContext(String targetInstanceId, int benchDamageCounters) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("attackName", "massage");
        payload.put("targetInstanceId", targetInstanceId);
        return buildContext("massage", payload, true, benchDamageCounters);
    }

    private AttackContext buildContext(String attackName,
                                       Map<String, Object> payload,
                                       boolean hasBench,
                                       int benchDamageCounters) {
        ActivePokemon mrMime = ActivePokemon.builder()
                .instanceId("mr-mime-1")
                .cardId("xy1-91")
                .types(new ArrayList<>(List.of(EnergyType.FAIRY)))
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();

        ActivePokemon defender = ActivePokemon.builder()
                .instanceId("def-1")
                .cardId("xy1-1")
                .types(new ArrayList<>(List.of(EnergyType.GRASS)))
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();

        PlayerState attackerState = playerState(PLAYER_1, List.of(), cardIds(5));
        attackerState.setActivePokemon(mrMime);

        if (hasBench) {
            BenchPokemon bench = BenchPokemon.builder()
                    .instanceId("bench-1")
                    .cardId("xy1-2")
                    .attachedEnergyIds(new ArrayList<>())
                    .evolutionStack(new ArrayList<>())
                    .damageCounters(benchDamageCounters)
                    .build();
            attackerState.setBench(new ArrayList<>(List.of(bench)));
        } else {
            attackerState.setBench(new ArrayList<>());
        }

        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(defender);

        BoardState state = boardState(attackerState, defenderState);

        GameAction act = GameAction.builder()
                .type(GameActionType.DECLARE_ATTACK)
                .playerId(PLAYER_1)
                .payload(payload)
                .build();

        return AttackContext.builder()
                .boardState(state)
                .action(act)
                .attackName(attackName)
                .modifiers(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
    }
}