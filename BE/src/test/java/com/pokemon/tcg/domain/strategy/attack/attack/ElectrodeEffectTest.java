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

class ElectrodeEffectTest {

    private CoinFlipService coinFlipService;
    private ElectrodeEffect effect;

    private static final String WATER_ENERGY = "xy1-131";

    @BeforeEach
    void setUp() {
        coinFlipService = mock(CoinFlipService.class);
        effect = new ElectrodeEffect(coinFlipService);
    }

    @Test
    void shouldSupportEerieImpulse() {
        assertThat(effect.getSupportedAttacks())
                .containsExactly("electrode|eerie impulse");
    }

    @Test
    void eerieImpulse_onHeads_shouldDiscardEnergyFromOpponentActive() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext(true, false, "def-1", WATER_ENERGY);

        effect.apply(ctx);

        PlayerState opponent = ctx.getBoardState().getStateFor(PLAYER_2);
        assertThat(opponent.getActivePokemon().getAttachedEnergyIds()).isEmpty();
        assertThat(opponent.getDiscard()).contains(WATER_ENERGY);
    }

    @Test
    void eerieImpulse_onHeads_shouldDiscardEnergyFromOpponentBench() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext(false, true, "bench-opp-1", WATER_ENERGY);

        effect.apply(ctx);

        PlayerState opponent = ctx.getBoardState().getStateFor(PLAYER_2);
        assertThat(opponent.getBench().get(0).getAttachedEnergyIds()).isEmpty();
        assertThat(opponent.getDiscard()).contains(WATER_ENERGY);
    }

    @Test
    void eerieImpulse_onTails_shouldNotDiscardEnergy() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.TAILS);
        AttackContext ctx = buildContext(true, false, "def-1", WATER_ENERGY);

        effect.apply(ctx);

        PlayerState opponent = ctx.getBoardState().getStateFor(PLAYER_2);
        assertThat(opponent.getActivePokemon().getAttachedEnergyIds())
                .contains(WATER_ENERGY);
        assertThat(opponent.getDiscard()).isEmpty();
    }

    @Test
    void eerieImpulse_shouldDoNothing_whenTargetInstanceIdNull() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext(true, false, null, WATER_ENERGY);

        effect.apply(ctx);

        PlayerState opponent = ctx.getBoardState().getStateFor(PLAYER_2);
        assertThat(opponent.getActivePokemon().getAttachedEnergyIds())
                .contains(WATER_ENERGY);
    }

    @Test
    void eerieImpulse_shouldDoNothing_whenEnergyCardIdNull() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext(true, false, "def-1", null);

        effect.apply(ctx);

        PlayerState opponent = ctx.getBoardState().getStateFor(PLAYER_2);
        assertThat(opponent.getActivePokemon().getAttachedEnergyIds())
                .contains(WATER_ENERGY);
    }

    @Test
    void eerieImpulse_shouldDoNothing_whenEnergyNotAttachedToTarget() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext(true, false, "def-1", "xy1-133"); // wrong energy

        effect.apply(ctx);

        PlayerState opponent = ctx.getBoardState().getStateFor(PLAYER_2);
        assertThat(opponent.getActivePokemon().getAttachedEnergyIds())
                .contains(WATER_ENERGY);
    }

    @Test
    void eerieImpulse_shouldNotAffectAttacker() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext(true, false, "def-1", WATER_ENERGY);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getDamageCounters()).isEqualTo(0);
    }

    @Test
    void unknownAttack_shouldDoNothing() {
        AttackContext ctx = buildContextWithAttackName("rollout", "def-1", WATER_ENERGY);

        effect.apply(ctx);

        verifyNoInteractions(coinFlipService);
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private AttackContext buildContext(boolean energyOnActive,
                                       boolean energyOnBench,
                                       String targetInstanceId,
                                       String energyCardId) {
        ActivePokemon electrode = ActivePokemon.builder()
                .instanceId("electrode-1")
                .cardId("xy1-45")
                .types(new ArrayList<>(List.of(EnergyType.LIGHTNING)))
                .attachedEnergyIds(new ArrayList<>(List.of("xy1-135")))
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
                .attachedEnergyIds(energyOnActive
                        ? new ArrayList<>(List.of(WATER_ENERGY))
                        : new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();

        PlayerState attackerState = playerState(PLAYER_1, List.of(), cardIds(5));
        attackerState.setActivePokemon(electrode);

        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(defender);
        defenderState.setDiscard(new ArrayList<>());

        if (energyOnBench) {
            BenchPokemon bench = BenchPokemon.builder()
                    .instanceId("bench-opp-1")
                    .cardId("xy1-2")
                    .attachedEnergyIds(new ArrayList<>(List.of(WATER_ENERGY)))
                    .evolutionStack(new ArrayList<>())
                    .damageCounters(0)
                    .build();
            defenderState.setBench(new ArrayList<>(List.of(bench)));
        }

        BoardState state = boardState(attackerState, defenderState);

        Map<String, Object> payload = new HashMap<>();
        payload.put("attackName", "Eerie Impulse");
        if (targetInstanceId != null) payload.put("targetInstanceId", targetInstanceId);
        if (energyCardId != null) payload.put("energyCardId", energyCardId);

        GameAction act = GameAction.builder()
                .type(GameActionType.DECLARE_ATTACK)
                .playerId(PLAYER_1)
                .payload(payload)
                .build();

        return AttackContext.builder()
                .boardState(state)
                .action(act)
                .attackName("eerie impulse")
                .modifiers(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
    }

    private AttackContext buildContextWithAttackName(String attackName,
                                                     String targetInstanceId,
                                                     String energyCardId) {
        ActivePokemon electrode = ActivePokemon.builder()
                .instanceId("electrode-1")
                .cardId("xy1-45")
                .types(new ArrayList<>(List.of(EnergyType.LIGHTNING)))
                .attachedEnergyIds(new ArrayList<>(List.of("xy1-135")))
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
                .attachedEnergyIds(new ArrayList<>(List.of(WATER_ENERGY)))
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();

        PlayerState attackerState = playerState(PLAYER_1, List.of(), cardIds(5));
        attackerState.setActivePokemon(electrode);
        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(defender);

        BoardState state = boardState(attackerState, defenderState);

        Map<String, Object> payload = new HashMap<>();
        payload.put("attackName", attackName);
        if (targetInstanceId != null) payload.put("targetInstanceId", targetInstanceId);
        if (energyCardId != null) payload.put("energyCardId", energyCardId);

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