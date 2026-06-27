package com.pokemon.tcg.domain.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.engine.attack.AttackContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

class LedianEffectTest {

    private LedianEffect effect;

    @BeforeEach
    void setUp() {
        effect = new LedianEffect();
    }

    @Test
    void apply_shouldPlace1DamageCounterOnTargetBenchPokemon() {
        BenchPokemon benchTarget = benchWithId("bench-1", "xy1-10");
        AttackContext ctx = buildContext("bench-1", List.of(benchTarget));

        effect.apply(ctx);

        assertThat(benchTarget.getDamageCounters()).isEqualTo(1);
    }

    @Test
    void apply_shouldNotAffectOtherBenchPokemon() {
        BenchPokemon benchTarget = benchWithId("bench-1", "xy1-10");
        BenchPokemon otherBench  = benchWithId("bench-2", "xy1-11");
        AttackContext ctx = buildContext("bench-1", List.of(benchTarget, otherBench));

        effect.apply(ctx);

        assertThat(benchTarget.getDamageCounters()).isEqualTo(1);
        assertThat(otherBench.getDamageCounters()).isEqualTo(0);
    }

    @Test
    void apply_shouldAccumulateCounters_whenBenchPokemonAlreadyDamaged() {
        BenchPokemon benchTarget = benchWithId("bench-1", "xy1-10");
        benchTarget.setDamageCounters(3);
        AttackContext ctx = buildContext("bench-1", List.of(benchTarget));

        effect.apply(ctx);

        assertThat(benchTarget.getDamageCounters()).isEqualTo(4);
    }

    @Test
    void apply_shouldDoNothing_whenOpponentHasNoBench() {
        AttackContext ctx = buildContext(null, List.of());

        effect.apply(ctx);
    }

    @Test
    void apply_shouldDoNothing_whenBenchTargetIdIsNull() {
        BenchPokemon benchTarget = benchWithId("bench-1", "xy1-10");
        AttackContext ctx = buildContext(null, List.of(benchTarget));

        effect.apply(ctx);

        assertThat(benchTarget.getDamageCounters()).isEqualTo(0);
    }

    @Test
    void apply_shouldDoNothing_whenBenchTargetIdDoesNotMatch() {
        BenchPokemon benchTarget = benchWithId("bench-1", "xy1-10");
        AttackContext ctx = buildContext("nonexistent-id", List.of(benchTarget));

        effect.apply(ctx);

        assertThat(benchTarget.getDamageCounters()).isEqualTo(0);
    }

    @Test
    void apply_shouldNotAffectActivePokemon() {
        BenchPokemon benchTarget = benchWithId("bench-1", "xy1-10");
        AttackContext ctx = buildContext("bench-1", List.of(benchTarget));

        ActivePokemon opponentActive = ctx.getBoardState()
                .getStateFor(PLAYER_2).getActivePokemon();
        int initialCounters = opponentActive.getDamageCounters();

        effect.apply(ctx);

        assertThat(opponentActive.getDamageCounters()).isEqualTo(initialCounters);
    }

    private AttackContext buildContext(String benchTargetInstanceId,
                                      List<BenchPokemon> opponentBench) {
        ActivePokemon ledian = ActivePokemon.builder()
                .instanceId("ledian-1")
                .cardId("xy1-7")
                .types(new ArrayList<>(List.of(EnergyType.COLORLESS)))
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
                .cardId("xy1-20")
                .types(new ArrayList<>(List.of(EnergyType.FIRE)))
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();

        PlayerState attackerState = playerState(PLAYER_1, List.of(), cardIds(5));
        attackerState.setActivePokemon(ledian);

        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(defender);
        defenderState.setBench(new ArrayList<>(opponentBench));

        BoardState state = boardState(attackerState, defenderState);

        Map<String, Object> payload = new HashMap<>();
        payload.put("attackName", "Mach Punch");
        if (benchTargetInstanceId != null) {
            payload.put("benchTargetInstanceId", benchTargetInstanceId);
        }

        GameAction act = GameAction.builder()
                .type(GameActionType.DECLARE_ATTACK)
                .playerId(PLAYER_1)
                .payload(payload)
                .build();

        return AttackContext.builder()
                .boardState(state)
                .action(act)
                .attackName("Mach Punch")
                .modifiers(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
    }

    private BenchPokemon benchWithId(String instanceId, String cardId) {
        return BenchPokemon.builder()
                .instanceId(instanceId)
                .cardId(cardId)
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>(List.of(cardId)))
                .damageCounters(0)
                .enteredThisTurn(false)
                .build();
    }
}
