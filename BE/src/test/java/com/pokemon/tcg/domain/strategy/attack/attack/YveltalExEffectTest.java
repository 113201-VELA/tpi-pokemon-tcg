package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.DamageModifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

class YveltalExEffectTest {

    private YveltalExEffect effect;

    private static final String DARK_ENERGY  = "xy1-136";
    private static final String OTHER_ENERGY = "xy1-131";

    @BeforeEach
    void setUp() {
        effect = new YveltalExEffect();
    }

    @Test
    void shouldSupportBothAttacks() {
        assertThat(effect.getSupportedAttacks())
                .contains("yveltal-ex|evil ball", "yveltal-ex|y cyclone");
    }

    // ─── Evil Ball ────────────────────────────────────────────────────────────

    @Test
    void evilBall_shouldAdd20PerEnergy_countingBothActivePokemon() {
        // Yveltal has 2 energies, defender has 1 -> total 3 -> +60 damage
        AttackContext ctx = buildEvilBallContext(
                List.of(DARK_ENERGY, OTHER_ENERGY), List.of(DARK_ENERGY));

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).hasSize(1);
        DamageModifier mod = ctx.getModifiers().get(0);
        assertThat(mod.amount()).isEqualTo(60);
        assertThat(mod.beforeWeakness()).isTrue();
    }

    @Test
    void evilBall_shouldCountOnlyAttackerEnergy_whenDefenderHasNone() {
        AttackContext ctx = buildEvilBallContext(
                List.of(DARK_ENERGY, OTHER_ENERGY), List.of());

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).hasSize(1);
        assertThat(ctx.getModifiers().get(0).amount()).isEqualTo(40);
    }

    @Test
    void evilBall_shouldCountOnlyDefenderEnergy_whenAttackerHasNone() {
        AttackContext ctx = buildEvilBallContext(
                List.of(), List.of(DARK_ENERGY, OTHER_ENERGY));

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).hasSize(1);
        assertThat(ctx.getModifiers().get(0).amount()).isEqualTo(40);
    }

    @Test
    void evilBall_shouldNotAddModifier_whenNoEnergyOnEitherSide() {
        AttackContext ctx = buildEvilBallContext(List.of(), List.of());

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    // ─── Y Cyclone ────────────────────────────────────────────────────────────

    @Test
    void yCyclone_shouldMoveEnergy_fromActiveToOwnBench() {
        AttackContext ctx = buildYCycloneContext(
                List.of(DARK_ENERGY, OTHER_ENERGY),
                true, "bench-1", DARK_ENERGY);

        effect.apply(ctx);

        ActivePokemon yveltal = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        assertThat(yveltal.getAttachedEnergyIds()).containsExactly(OTHER_ENERGY);

        BenchPokemon bench = ctx.getBoardState().getStateFor(PLAYER_1).getBench().get(0);
        assertThat(bench.getAttachedEnergyIds()).contains(DARK_ENERGY);
    }

    @Test
    void yCyclone_shouldDoNothing_whenNoPayload() {
        AttackContext ctx = buildYCycloneContext(
                List.of(DARK_ENERGY), true, null, null);

        effect.apply(ctx);

        ActivePokemon yveltal = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        assertThat(yveltal.getAttachedEnergyIds()).containsExactly(DARK_ENERGY);
    }

    @Test
    void yCyclone_shouldDoNothing_whenEnergyNotAttachedToYveltal() {
        AttackContext ctx = buildYCycloneContext(
                List.of(DARK_ENERGY), true, "bench-1", OTHER_ENERGY); // not attached

        effect.apply(ctx);

        ActivePokemon yveltal = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        assertThat(yveltal.getAttachedEnergyIds()).containsExactly(DARK_ENERGY);
        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getBench().get(0)
                .getAttachedEnergyIds()).isEmpty();
    }

    @Test
    void yCyclone_shouldDoNothing_whenTargetBenchNotFound() {
        // Real bench exists as "bench-1", but payload points to a different id
        AttackContext ctx = buildYCycloneContext(
                List.of(DARK_ENERGY), true, "nonexistent-bench", DARK_ENERGY);

        effect.apply(ctx);

        ActivePokemon yveltal = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        assertThat(yveltal.getAttachedEnergyIds()).containsExactly(DARK_ENERGY);
    }

    @Test
    void yCyclone_shouldDoNothing_whenNoBenchInPlay() {
        AttackContext ctx = buildYCycloneContext(
                List.of(DARK_ENERGY), false, "bench-1", DARK_ENERGY);

        effect.apply(ctx);

        ActivePokemon yveltal = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        assertThat(yveltal.getAttachedEnergyIds()).containsExactly(DARK_ENERGY);
    }

    @Test
    void yCyclone_shouldNotAddDamageModifiers() {
        AttackContext ctx = buildYCycloneContext(
                List.of(DARK_ENERGY), true, "bench-1", DARK_ENERGY);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    // ─── unknown attack ───────────────────────────────────────────────────────

    @Test
    void unknownAttack_shouldDoNothing() {
        AttackContext ctx = buildEvilBallContext(List.of(DARK_ENERGY), List.of());
        ctx.setAttackName("unknown");

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private AttackContext buildEvilBallContext(List<String> yveltalEnergies,
                                               List<String> defenderEnergies) {
        return buildContext("evil ball", yveltalEnergies, defenderEnergies,
                false, null, null);
    }

    private AttackContext buildYCycloneContext(List<String> yveltalEnergies,
                                               boolean hasBench,
                                               String targetBenchInstanceId,
                                               String energyCardId) {
        return buildContext("y cyclone", yveltalEnergies, List.of(),
                hasBench, targetBenchInstanceId, energyCardId);
    }

    private AttackContext buildContext(String attackName,
                                       List<String> yveltalEnergies,
                                       List<String> defenderEnergies,
                                       boolean hasBench,
                                       String targetBenchInstanceId,
                                       String energyCardId) {
        ActivePokemon yveltal = ActivePokemon.builder()
                .instanceId("yveltal-ex-1")
                .cardId("xy1-79")
                .types(new ArrayList<>(List.of(EnergyType.DARKNESS)))
                .attachedEnergyIds(new ArrayList<>(yveltalEnergies))
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
                .attachedEnergyIds(new ArrayList<>(defenderEnergies))
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();

        PlayerState attackerState = playerState(PLAYER_1, List.of(), cardIds(5));
        attackerState.setActivePokemon(yveltal);

        if (hasBench) {
            BenchPokemon bench = BenchPokemon.builder()
                    .instanceId("bench-1")
                    .cardId("xy1-2")
                    .attachedEnergyIds(new ArrayList<>())
                    .evolutionStack(new ArrayList<>())
                    .damageCounters(0)
                    .build();
            attackerState.setBench(new ArrayList<>(List.of(bench)));
        } else {
            attackerState.setBench(new ArrayList<>());
        }

        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(defender);

        BoardState state = boardState(attackerState, defenderState);

        Map<String, Object> payload = new HashMap<>();
        payload.put("attackName", attackName);
        if (energyCardId != null) payload.put("energyCardId", energyCardId);
        if (targetBenchInstanceId != null) payload.put("targetBenchInstanceId", targetBenchInstanceId);

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