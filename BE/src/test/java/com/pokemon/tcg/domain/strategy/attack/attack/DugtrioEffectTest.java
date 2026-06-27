package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

class DugtrioEffectTest {

    private DugtrioEffect effect;

    @BeforeEach
    void setUp() {
        effect = new DugtrioEffect();
    }

    @Test
    void shouldSupportBothAttacks() {
        assertThat(effect.getSupportedAttacks())
                .contains("dugtrio|earthquake", "dugtrio|rock tumble");
    }

    // ─── Earthquake ───────────────────────────────────────────────────────────

    @Test
    void earthquake_shouldAddOneCounterToEachOwnBenchPokemon() {
        AttackContext ctx = buildContext("earthquake", 2, 0);

        effect.apply(ctx);

        List<BenchPokemon> bench = ctx.getBoardState()
                .getStateFor(PLAYER_1).getBench();
        assertThat(bench).allSatisfy(bp ->
                assertThat(bp.getDamageCounters()).isEqualTo(1));
    }

    @Test
    void earthquake_shouldStackOnExistingBenchCounters() {
        AttackContext ctx = buildContext("earthquake", 1, 0);
        ctx.getBoardState().getStateFor(PLAYER_1).getBench()
                .get(0).setDamageCounters(3);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getBench().get(0).getDamageCounters()).isEqualTo(4);
    }

    @Test
    void earthquake_shouldNotAffectOpponentBench() {
        AttackContext ctx = buildContext("earthquake", 1, 2);

        effect.apply(ctx);

        List<BenchPokemon> opponentBench = ctx.getBoardState()
                .getOpponentState(PLAYER_1).getBench();
        assertThat(opponentBench).allSatisfy(bp ->
                assertThat(bp.getDamageCounters()).isEqualTo(0));
    }

    @Test
    void earthquake_shouldDoNothingIfOwnBenchIsEmpty() {
        AttackContext ctx = buildContext("earthquake", 0, 0);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getBench()).isEmpty();
    }

    @Test
    void earthquake_shouldNotAddModifiers() {
        AttackContext ctx = buildContext("earthquake", 1, 0);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    // ─── Rock Tumble ──────────────────────────────────────────────────────────

    @Test
    void rockTumble_shouldSetIgnoreDefenderEffects() {
        AttackContext ctx = buildContext("rock tumble", 0, 0);

        effect.apply(ctx);

        assertThat(ctx.isIgnoreDefenderEffects()).isTrue();
    }

    @Test
    void rockTumble_shouldNotAddModifiers() {
        AttackContext ctx = buildContext("rock tumble", 0, 0);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    @Test
    void rockTumble_shouldNotAffectBench() {
        AttackContext ctx = buildContext("rock tumble", 2, 0);

        effect.apply(ctx);

        List<BenchPokemon> bench = ctx.getBoardState()
                .getStateFor(PLAYER_1).getBench();
        assertThat(bench).allSatisfy(bp ->
                assertThat(bp.getDamageCounters()).isEqualTo(0));
    }

    // ─── unknown attack ───────────────────────────────────────────────────────

    @Test
    void unknownAttack_shouldDoNothing() {
        AttackContext ctx = buildContext("unknown", 0, 0);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
        assertThat(ctx.isIgnoreDefenderEffects()).isFalse();
    }

    // ─── helper ───────────────────────────────────────────────────────────────

    private AttackContext buildContext(String attackName,
                                       int attackerBenchSize,
                                       int opponentBenchSize) {
        ActivePokemon dugtrio = ActivePokemon.builder()
                .instanceId("dugtrio-1")
                .cardId("xy1-59")
                .types(new ArrayList<>(List.of(EnergyType.FIGHTING)))
                .attachedEnergyIds(new ArrayList<>(
                        List.of("xy1-96", "xy1-95", "xy1-95")))
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
        attackerState.setActivePokemon(dugtrio);
        attackerState.setBench(buildBench(attackerBenchSize));

        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(defender);
        defenderState.setBench(buildBench(opponentBenchSize));

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

    private List<BenchPokemon> buildBench(int size) {
        List<BenchPokemon> bench = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            bench.add(BenchPokemon.builder()
                    .instanceId("bench-" + i)
                    .cardId("xy1-" + (i + 1))
                    .attachedEnergyIds(new ArrayList<>())
                    .evolutionStack(new ArrayList<>())
                    .damageCounters(0)
                    .build());
        }
        return bench;
    }
}