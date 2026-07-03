package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

class SpritzeeEffectTest {

    private SpritzeeEffect effect;

    @BeforeEach
    void setUp() {
        effect = new SpritzeeEffect();
    }

    @Test
    void shouldSupportOnlySweetScent() {
        assertThat(effect.getSupportedAttacks()).containsExactly("spritzee|sweet scent");
    }

    // ─── Sweet Scent ──────────────────────────────────────────────────────────

    @Test
    void sweetScent_shouldHealTwoCounters_fromOwnActive() {
        AttackContext ctx = buildContext("sweet scent", "spritzee-1", 5, true, 3);

        effect.apply(ctx);

        ActivePokemon spritzee = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        assertThat(spritzee.getDamageCounters()).isEqualTo(3); // 5 - 2
    }

    @Test
    void sweetScent_shouldHealTwoCounters_fromOwnBench() {
        AttackContext ctx = buildContext("sweet scent", "bench-1", 5, true, 4);

        effect.apply(ctx);

        BenchPokemon bench = ctx.getBoardState().getStateFor(PLAYER_1).getBench().get(0);
        assertThat(bench.getDamageCounters()).isEqualTo(2); // 4 - 2
    }

    @Test
    void sweetScent_shouldClampAtZero_whenHealExceedsDamage() {
        AttackContext ctx = buildContext("sweet scent", "spritzee-1", 1, true, 0);

        effect.apply(ctx);

        ActivePokemon spritzee = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        assertThat(spritzee.getDamageCounters()).isEqualTo(0);
    }

    @Test
    void sweetScent_shouldDoNothing_whenNoTargetSpecified() {
        AttackContext ctx = buildContext("sweet scent", null, 5, true, 3);

        effect.apply(ctx);

        ActivePokemon spritzee = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        assertThat(spritzee.getDamageCounters()).isEqualTo(5);
    }

    @Test
    void sweetScent_shouldDoNothing_whenTargetNotFound() {
        AttackContext ctx = buildContext("sweet scent", "nonexistent-id", 5, true, 3);

        effect.apply(ctx);

        ActivePokemon spritzee = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        assertThat(spritzee.getDamageCounters()).isEqualTo(5);
        BenchPokemon bench = ctx.getBoardState().getStateFor(PLAYER_1).getBench().get(0);
        assertThat(bench.getDamageCounters()).isEqualTo(3);
    }

    @Test
    void sweetScent_shouldNotAffectOpponent() {
        AttackContext ctx = buildContext("sweet scent", "spritzee-1", 5, true, 3);
        int defenderCountersBefore = ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getDamageCounters();

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getDamageCounters()).isEqualTo(defenderCountersBefore);
    }

    // ─── Flop / unknown attack ─────────────────────────────────────────────

    @Test
    void flop_shouldDoNothing() {
        AttackContext ctx = buildContext("flop", "spritzee-1", 5, true, 3);

        effect.apply(ctx);

        ActivePokemon spritzee = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        assertThat(spritzee.getDamageCounters()).isEqualTo(5);
    }

    // ─── helper ───────────────────────────────────────────────────────────────

    private AttackContext buildContext(String attackName,
                                       String targetInstanceId,
                                       int activeDamageCounters,
                                       boolean hasBench,
                                       int benchDamageCounters) {
        ActivePokemon spritzee = ActivePokemon.builder()
                .instanceId("spritzee-1")
                .cardId("xy1-92")
                .types(new ArrayList<>(List.of(EnergyType.FAIRY)))
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(activeDamageCounters)
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
        attackerState.setActivePokemon(spritzee);

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

        Map<String, Object> payload = new HashMap<>();
        payload.put("attackName", attackName);
        if (targetInstanceId != null) payload.put("targetInstanceId", targetInstanceId);

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