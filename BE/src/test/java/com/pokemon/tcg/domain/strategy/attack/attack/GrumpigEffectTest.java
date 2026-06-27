package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

class GrumpigEffectTest {

    private GrumpigEffect effect;

    private static final String PSYCHIC_ENERGY = "xy1-95";

    @BeforeEach
    void setUp() {
        effect = new GrumpigEffect();
    }

    @Test
    void shouldSupportBothAttacks() {
        assertThat(effect.getSupportedAttacks())
                .contains("grumpig|tricky steps", "grumpig|psybeam");
    }

    // ─── Tricky Steps ─────────────────────────────────────────────────────────

    @Test
    void trickySteps_shouldMoveEnergyFromActiveToTargetBench() {
        AttackContext ctx = buildContext("tricky steps",
                List.of(PSYCHIC_ENERGY), true, PSYCHIC_ENERGY, "bench-opp-1");

        effect.apply(ctx);

        PlayerState opponent = ctx.getBoardState().getStateFor(PLAYER_2);
        assertThat(opponent.getActivePokemon().getAttachedEnergyIds()).isEmpty();
        assertThat(opponent.getBench().get(0).getAttachedEnergyIds())
                .contains(PSYCHIC_ENERGY);
    }

    @Test
    void trickySteps_shouldDoNothing_whenEnergyCardIdNull() {
        AttackContext ctx = buildContext("tricky steps",
                List.of(PSYCHIC_ENERGY), true, null, "bench-opp-1");

        effect.apply(ctx);

        PlayerState opponent = ctx.getBoardState().getStateFor(PLAYER_2);
        assertThat(opponent.getActivePokemon().getAttachedEnergyIds())
                .contains(PSYCHIC_ENERGY);
    }

    @Test
    void trickySteps_shouldDoNothing_whenTargetBenchNull() {
        AttackContext ctx = buildContext("tricky steps",
                List.of(PSYCHIC_ENERGY), true, PSYCHIC_ENERGY, null);

        effect.apply(ctx);

        PlayerState opponent = ctx.getBoardState().getStateFor(PLAYER_2);
        assertThat(opponent.getActivePokemon().getAttachedEnergyIds())
                .contains(PSYCHIC_ENERGY);
    }

    @Test
    void trickySteps_shouldDoNothing_whenEnergyNotAttachedToActive() {
        AttackContext ctx = buildContext("tricky steps",
                List.of(), true, PSYCHIC_ENERGY, "bench-opp-1");

        effect.apply(ctx);

        PlayerState opponent = ctx.getBoardState().getStateFor(PLAYER_2);
        assertThat(opponent.getBench().get(0).getAttachedEnergyIds()).isEmpty();
    }

    @Test
    void trickySteps_shouldDoNothing_whenNoBench() {
        AttackContext ctx = buildContext("tricky steps",
                List.of(PSYCHIC_ENERGY), false, PSYCHIC_ENERGY, "bench-opp-1");

        effect.apply(ctx);

        PlayerState opponent = ctx.getBoardState().getStateFor(PLAYER_2);
        assertThat(opponent.getActivePokemon().getAttachedEnergyIds())
                .contains(PSYCHIC_ENERGY);
    }

    @Test
    void trickySteps_shouldNotAffectAttacker() {
        AttackContext ctx = buildContext("tricky steps",
                List.of(PSYCHIC_ENERGY), true, PSYCHIC_ENERGY, "bench-opp-1");

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getDamageCounters()).isEqualTo(0);
    }

    // ─── Psybeam ──────────────────────────────────────────────────────────────

    @Test
    void psybeam_shouldConfuseDefender() {
        AttackContext ctx = buildContext("psybeam",
                List.of(), false, null, null);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getConditions())
                .contains(SpecialCondition.CONFUSED);
    }

    @Test
    void psybeam_shouldReplaceAsleep() {
        AttackContext ctx = buildContext("psybeam",
                List.of(), false, null, null);
        ctx.getBoardState().getStateFor(PLAYER_2).getActivePokemon()
                .getConditions().add(SpecialCondition.ASLEEP);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getConditions())
                .contains(SpecialCondition.CONFUSED)
                .doesNotContain(SpecialCondition.ASLEEP);
    }

    @Test
    void psybeam_shouldNotAffectAttacker() {
        AttackContext ctx = buildContext("psybeam",
                List.of(), false, null, null);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getConditions()).isEmpty();
    }

    @Test
    void psybeam_shouldNotAddModifiers() {
        AttackContext ctx = buildContext("psybeam",
                List.of(), false, null, null);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    // ─── unknown attack ───────────────────────────────────────────────────────

    @Test
    void unknownAttack_shouldDoNothing() {
        AttackContext ctx = buildContext("unknown",
                List.of(), false, null, null);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private AttackContext buildContext(String attackName,
                                       List<String> defenderActiveEnergies,
                                       boolean defenderHasBench,
                                       String energyCardId,
                                       String targetBenchInstanceId) {
        ActivePokemon grumpig = ActivePokemon.builder()
                .instanceId("grumpig-1")
                .cardId("xy1-50")
                .types(new ArrayList<>(List.of(EnergyType.PSYCHIC)))
                .attachedEnergyIds(new ArrayList<>(
                        List.of(PSYCHIC_ENERGY, PSYCHIC_ENERGY)))
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
                .attachedEnergyIds(new ArrayList<>(defenderActiveEnergies))
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();

        PlayerState attackerState = playerState(PLAYER_1, List.of(), cardIds(5));
        attackerState.setActivePokemon(grumpig);

        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(defender);

        if (defenderHasBench) {
            BenchPokemon bench = BenchPokemon.builder()
                    .instanceId("bench-opp-1")
                    .cardId("xy1-2")
                    .attachedEnergyIds(new ArrayList<>())
                    .evolutionStack(new ArrayList<>())
                    .damageCounters(0)
                    .build();
            defenderState.setBench(new ArrayList<>(List.of(bench)));
        }

        BoardState state = boardState(attackerState, defenderState);

        Map<String, Object> payload = new HashMap<>();
        payload.put("attackName", attackName);
        if (energyCardId != null) payload.put("energyCardId", energyCardId);
        if (targetBenchInstanceId != null)
            payload.put("targetBenchInstanceId", targetBenchInstanceId);

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