package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.engine.CoinFlipService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CorsolaEffectTest {

    private CoinFlipService coinFlipService;
    private CorsolaEffect effect;

    @BeforeEach
    void setUp() {
        coinFlipService = mock(CoinFlipService.class);
        effect = new CorsolaEffect(coinFlipService);
    }

    // ─── getSupportedAttacks ──────────────────────────────────────────────────

    @Test
    void shouldSupportBothAttacks() {
        assertThat(effect.getSupportedAttacks())
                .contains("corsola|refresh", "corsola|spiny rush");
    }

    // ─── Refresh ──────────────────────────────────────────────────────────────

    @Test
    void refresh_shouldHeal30Damage() {
        AttackContext ctx = buildContext("refresh", 5, new HashSet<>());

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getDamageCounters()).isEqualTo(2);
    }

    @Test
    void refresh_shouldNotGoBelowZero_whenLessThan30Damage() {
        AttackContext ctx = buildContext("refresh", 2, new HashSet<>());

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getDamageCounters()).isEqualTo(0);
    }

    @Test
    void refresh_shouldHealToZero_whenExactly30Damage() {
        AttackContext ctx = buildContext("refresh", 3, new HashSet<>());

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getDamageCounters()).isEqualTo(0);
    }

    @Test
    void refresh_shouldRemoveAllSpecialConditions() {
        Set<SpecialCondition> conditions = new HashSet<>(
                Set.of(SpecialCondition.POISONED, SpecialCondition.BURNED));
        AttackContext ctx = buildContext("refresh", 3, conditions);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getConditions()).isEmpty();
    }

    @Test
    void refresh_shouldNotAffectOpponent() {
        AttackContext ctx = buildContext("refresh", 4, new HashSet<>());
        int defenderCounters = ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getDamageCounters();

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getDamageCounters()).isEqualTo(defenderCounters);
    }

    @Test
    void refresh_shouldNotFlipCoin() {
        AttackContext ctx = buildContext("refresh", 3, new HashSet<>());

        effect.apply(ctx);

        verifyNoInteractions(coinFlipService);
    }

    // ─── Spiny Rush ───────────────────────────────────────────────────────────

    @Test
    void spinyRush_shouldAdd20Damage_perHead_thenStop() {
        // HEADS, HEADS, TAILS
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString()))
                .thenReturn(CoinResult.HEADS)
                .thenReturn(CoinResult.HEADS)
                .thenReturn(CoinResult.TAILS);
        AttackContext ctx = buildContext("spiny rush", 0, new HashSet<>());

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).hasSize(1);
        assertThat(ctx.getModifiers().get(0).amount()).isEqualTo(40); // 2 × 20
    }

    @Test
    void spinyRush_shouldAdd0Damage_whenFirstFlipIsTails() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.TAILS);
        AttackContext ctx = buildContext("spiny rush", 0, new HashSet<>());

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    @Test
    void spinyRush_shouldAdd100Damage_when5Heads() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString()))
                .thenReturn(CoinResult.HEADS)
                .thenReturn(CoinResult.HEADS)
                .thenReturn(CoinResult.HEADS)
                .thenReturn(CoinResult.HEADS)
                .thenReturn(CoinResult.HEADS)
                .thenReturn(CoinResult.TAILS);
        AttackContext ctx = buildContext("spiny rush", 0, new HashSet<>());

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).hasSize(1);
        assertThat(ctx.getModifiers().get(0).amount()).isEqualTo(100); // 5 × 20
    }

    @Test
    void spinyRush_shouldStopFlipping_afterFirstTails() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString()))
                .thenReturn(CoinResult.HEADS)
                .thenReturn(CoinResult.TAILS);
        AttackContext ctx = buildContext("spiny rush", 0, new HashSet<>());

        effect.apply(ctx);

        verify(coinFlipService, times(2)).flipAndEmit(any(AttackContext.class), anyString());
    }

    // ─── unknown attack ───────────────────────────────────────────────────────

    @Test
    void unknownAttack_shouldDoNothing() {
        AttackContext ctx = buildContext("unknown", 3, new HashSet<>());

        effect.apply(ctx);

        verifyNoInteractions(coinFlipService);
        assertThat(ctx.getModifiers()).isEmpty();
        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getDamageCounters()).isEqualTo(3);
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private AttackContext buildContext(String attackName,
                                       int initialDamageCounters,
                                       Set<SpecialCondition> conditions) {
        ActivePokemon corsola = ActivePokemon.builder()
                .instanceId("corsola-1")
                .cardId("xy1-36")
                .types(new ArrayList<>(List.of(EnergyType.WATER)))
                .attachedEnergyIds(new ArrayList<>(List.of("xy1-131", "xy1-131")))
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(initialDamageCounters)
                .conditions(new HashSet<>(conditions))
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
        attackerState.setActivePokemon(corsola);
        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(defender);

        BoardState state = boardState(attackerState, defenderState);

        GameAction act = GameAction.builder()
                .type(GameActionType.DECLARE_ATTACK)
                .playerId(PLAYER_1)
                .payload(Map.of("attackName", attackName))
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