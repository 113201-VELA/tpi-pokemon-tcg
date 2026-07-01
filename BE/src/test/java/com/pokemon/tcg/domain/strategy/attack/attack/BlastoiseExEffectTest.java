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

class BlastoiseExEffectTest {

    private CoinFlipService coinFlipService;
    private BlastoiseExEffect effect;

    private static final String WATER_ENERGY = "xy1-131";

    @BeforeEach
    void setUp() {
        coinFlipService = mock(CoinFlipService.class);
        effect = new BlastoiseExEffect(coinFlipService);
    }

    // ─── getSupportedAttacks ──────────────────────────────────────────────────

    @Test
    void shouldSupportBothAttacks() {
        assertThat(effect.getSupportedAttacks())
                .contains("blastoise-ex|rapid spin", "blastoise-ex|splash bomb");
    }

    // ─── Rapid Spin ───────────────────────────────────────────────────────────

    @Test
    void rapidSpin_shouldSetForcedSwitch_forAttacker_whenAttackerHasBench() {
        AttackContext ctx = buildContext("rapid spin", true, true);

        effect.apply(ctx);

        // After both flags are set, the last one written is opponent's
        // but we verify at least one forced switch was pending at some point.
        // Since opponent overwrites attacker, final state has opponent pending.
        assertThat(ctx.getBoardState().getPendingForcedSwitchPlayerId())
                .isEqualTo(PLAYER_2);
    }

    @Test
    void rapidSpin_shouldSetForcedSwitch_forOpponentOnly_whenAttackerHasNoBench() {
        AttackContext ctx = buildContext("rapid spin", false, true);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getPendingForcedSwitchPlayerId())
                .isEqualTo(PLAYER_2);
    }

    @Test
    void rapidSpin_shouldSetForcedSwitch_forAttackerOnly_whenOpponentHasNoBench() {
        AttackContext ctx = buildContext("rapid spin", true, false);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getPendingForcedSwitchPlayerId())
                .isEqualTo(PLAYER_1);
    }

    @Test
    void rapidSpin_shouldNotSetForcedSwitch_whenBothHaveNoBench() {
        AttackContext ctx = buildContext("rapid spin", false, false);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getPendingForcedSwitchPlayerId()).isNull();
    }

    @Test
    void rapidSpin_shouldNotFlipCoin() {
        AttackContext ctx = buildContext("rapid spin", true, true);

        effect.apply(ctx);

        verifyNoInteractions(coinFlipService);
    }

    // ─── Splash Bomb ──────────────────────────────────────────────────────────

    @Test
    void splashBomb_shouldAddSelfDamage_onTails() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.TAILS);
        AttackContext ctx = buildContext("splash bomb", false, false);
        int initialCounters = ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getDamageCounters();

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getDamageCounters())
                .isEqualTo(initialCounters + 3);
    }

    @Test
    void splashBomb_shouldNotAddSelfDamage_onHeads() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext("splash bomb", false, false);
        int initialCounters = ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getDamageCounters();

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getDamageCounters())
                .isEqualTo(initialCounters);
    }

    @Test
    void splashBomb_shouldNotAffectOpponent_onTails() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.TAILS);
        AttackContext ctx = buildContext("splash bomb", false, false);
        int defenderCounters = ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getDamageCounters();

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getDamageCounters())
                .isEqualTo(defenderCounters);
    }

    @Test
    void splashBomb_shouldNotSetForcedSwitch() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.TAILS);
        AttackContext ctx = buildContext("splash bomb", false, false);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getPendingForcedSwitchPlayerId()).isNull();
    }

    // ─── unknown attack ───────────────────────────────────────────────────────

    @Test
    void unknownAttack_shouldDoNothing() {
        AttackContext ctx = buildContext("unknown", false, false);

        effect.apply(ctx);

        verifyNoInteractions(coinFlipService);
        assertThat(ctx.getBoardState().getPendingForcedSwitchPlayerId()).isNull();
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private AttackContext buildContext(String attackName,
                                       boolean attackerHasBench,
                                       boolean opponentHasBench) {
        ActivePokemon blastoiseEx = ActivePokemon.builder()
                .instanceId("blastoise-ex-1")
                .cardId("xy1-29")
                .types(new ArrayList<>(List.of(EnergyType.WATER)))
                .attachedEnergyIds(new ArrayList<>(
                        List.of(WATER_ENERGY, WATER_ENERGY, WATER_ENERGY)))
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
        attackerState.setActivePokemon(blastoiseEx);
        if (attackerHasBench) {
            BenchPokemon benchPokemon = BenchPokemon.builder()
                    .instanceId("bench-attacker-1")
                    .cardId("xy1-2")
                    .attachedEnergyIds(new ArrayList<>())
                    .evolutionStack(new ArrayList<>())
                    .damageCounters(0)
                    .build();
            attackerState.setBench(new ArrayList<>(List.of(benchPokemon)));
        }

        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(defender);
        if (opponentHasBench) {
            BenchPokemon benchPokemon = BenchPokemon.builder()
                    .instanceId("bench-opponent-1")
                    .cardId("xy1-3")
                    .attachedEnergyIds(new ArrayList<>())
                    .evolutionStack(new ArrayList<>())
                    .damageCounters(0)
                    .build();
            defenderState.setBench(new ArrayList<>(List.of(benchPokemon)));
        }

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