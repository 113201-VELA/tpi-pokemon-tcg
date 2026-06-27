package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

class RaichuEffectTest {

    private RaichuEffect effect;

    private static final String LIGHTNING_ENERGY = "xy1-135";

    @BeforeEach
    void setUp() {
        effect = new RaichuEffect();
    }

    @Test
    void shouldSupportBothAttacks() {
        assertThat(effect.getSupportedAttacks())
                .contains("raichu|circle circuit", "raichu|thunderbolt");
    }

    // ─── Circle Circuit ───────────────────────────────────────────────────────

    @Test
    void circleCircuit_shouldAdd20PerBenchPokemon() {
        AttackContext ctx = buildContext("circle circuit", 3,
                List.of(LIGHTNING_ENERGY, LIGHTNING_ENERGY, LIGHTNING_ENERGY));

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).hasSize(1);
        assertThat(ctx.getModifiers().get(0).amount()).isEqualTo(60); // 3 × 20
    }

    @Test
    void circleCircuit_shouldAdd0_whenNoBench() {
        AttackContext ctx = buildContext("circle circuit", 0,
                List.of(LIGHTNING_ENERGY, LIGHTNING_ENERGY));

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    @Test
    void circleCircuit_shouldAdd20_whenOneBenchPokemon() {
        AttackContext ctx = buildContext("circle circuit", 1,
                List.of(LIGHTNING_ENERGY, LIGHTNING_ENERGY));

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).hasSize(1);
        assertThat(ctx.getModifiers().get(0).amount()).isEqualTo(20);
    }

    @Test
    void circleCircuit_shouldAdd100_whenFiveBenchPokemon() {
        AttackContext ctx = buildContext("circle circuit", 5,
                List.of(LIGHTNING_ENERGY, LIGHTNING_ENERGY));

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).hasSize(1);
        assertThat(ctx.getModifiers().get(0).amount()).isEqualTo(100); // 5 × 20
    }

    @Test
    void circleCircuit_shouldNotAffectOpponent() {
        AttackContext ctx = buildContext("circle circuit", 3,
                List.of(LIGHTNING_ENERGY));
        int defenderCounters = ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getDamageCounters();

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getDamageCounters()).isEqualTo(defenderCounters);
    }

    // ─── Thunderbolt ──────────────────────────────────────────────────────────

    @Test
    void thunderbolt_shouldDiscardAllEnergies() {
        AttackContext ctx = buildContext("thunderbolt", 0,
                List.of(LIGHTNING_ENERGY, LIGHTNING_ENERGY, LIGHTNING_ENERGY));

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getAttachedEnergyIds()).isEmpty();
        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDiscard())
                .containsExactlyInAnyOrder(
                        LIGHTNING_ENERGY, LIGHTNING_ENERGY, LIGHTNING_ENERGY);
    }

    @Test
    void thunderbolt_shouldDoNothing_whenNoEnergyAttached() {
        AttackContext ctx = buildContext("thunderbolt", 0, List.of());

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDiscard()).isEmpty();
    }

    @Test
    void thunderbolt_shouldNotAddModifiers() {
        AttackContext ctx = buildContext("thunderbolt", 0,
                List.of(LIGHTNING_ENERGY));

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    @Test
    void thunderbolt_shouldNotAffectOpponent() {
        AttackContext ctx = buildContext("thunderbolt", 0,
                List.of(LIGHTNING_ENERGY));
        int defenderCounters = ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getDamageCounters();

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getDamageCounters()).isEqualTo(defenderCounters);
    }

    // ─── unknown attack ───────────────────────────────────────────────────────

    @Test
    void unknownAttack_shouldDoNothing() {
        AttackContext ctx = buildContext("unknown", 2, List.of(LIGHTNING_ENERGY));

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getAttachedEnergyIds()).contains(LIGHTNING_ENERGY);
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private AttackContext buildContext(String attackName, int benchSize,
                                       List<String> attachedEnergies) {
        ActivePokemon raichu = ActivePokemon.builder()
                .instanceId("raichu-1")
                .cardId("xy1-43")
                .types(new ArrayList<>(List.of(EnergyType.LIGHTNING)))
                .attachedEnergyIds(new ArrayList<>(attachedEnergies))
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();

        PlayerState attackerState = playerState(PLAYER_1, List.of(), cardIds(5));
        attackerState.setActivePokemon(raichu);
        attackerState.setDiscard(new ArrayList<>());

        List<BenchPokemon> bench = new ArrayList<>();
        for (int i = 0; i < benchSize; i++) {
            bench.add(BenchPokemon.builder()
                    .instanceId("bench-" + i)
                    .cardId("xy1-2")
                    .attachedEnergyIds(new ArrayList<>())
                    .evolutionStack(new ArrayList<>())
                    .damageCounters(0)
                    .build());
        }
        attackerState.setBench(bench);

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