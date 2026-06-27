package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

class TrevenantEffectTest {

    private TrevenantEffect effect;

    @BeforeEach
    void setUp() {
        effect = new TrevenantEffect();
    }

    @Test
    void shouldSupportTreeSlam() {
        assertThat(effect.getSupportedAttacks()).containsExactly("trevenant|tree slam");
    }

    @Test
    void treeSlam_shouldDamage2BenchedPokemon_when2OrMoreOnBench() {
        AttackContext ctx = buildContext("tree slam", 3);

        effect.apply(ctx);

        PlayerState opponent = ctx.getBoardState().getStateFor(PLAYER_2);
        assertThat(opponent.getBench().get(0).getDamageCounters()).isEqualTo(2);
        assertThat(opponent.getBench().get(1).getDamageCounters()).isEqualTo(2);
        assertThat(opponent.getBench().get(2).getDamageCounters()).isEqualTo(0);
    }

    @Test
    void treeSlam_shouldDamage1BenchedPokemon_whenOnly1OnBench() {
        AttackContext ctx = buildContext("tree slam", 1);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_2)
                .getBench().get(0).getDamageCounters()).isEqualTo(2);
    }

    @Test
    void treeSlam_shouldDoNoBenchDamage_whenNoBench() {
        AttackContext ctx = buildContext("tree slam", 0);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_2).getBench()).isEmpty();
    }

    @Test
    void treeSlam_shouldNotAffectOpponentActive() {
        AttackContext ctx = buildContext("tree slam", 2);
        int defenderCounters = ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getDamageCounters();

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getDamageCounters()).isEqualTo(defenderCounters);
    }

    @Test
    void treeSlam_shouldNotAffectAttacker() {
        AttackContext ctx = buildContext("tree slam", 2);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getDamageCounters()).isEqualTo(0);
    }

    @Test
    void treeSlam_shouldNotAddModifiers() {
        AttackContext ctx = buildContext("tree slam", 2);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    @Test
    void unknownAttack_shouldDoNothing() {
        AttackContext ctx = buildContext("unknown", 2);

        effect.apply(ctx);

        ctx.getBoardState().getStateFor(PLAYER_2).getBench()
                .forEach(b -> assertThat(b.getDamageCounters()).isEqualTo(0));
    }

    private AttackContext buildContext(String attackName, int benchSize) {
        ActivePokemon trevenant = ActivePokemon.builder()
                .instanceId("trevenant-1")
                .cardId("xy1-55")
                .types(new ArrayList<>(List.of(EnergyType.PSYCHIC)))
                .attachedEnergyIds(new ArrayList<>(
                        List.of("xy1-95", "xy1-95", "xy1-95")))
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
                .types(new ArrayList<>(List.of(EnergyType.DARKNESS)))
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();

        PlayerState attackerState = playerState(PLAYER_1, List.of(), cardIds(5));
        attackerState.setActivePokemon(trevenant);

        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(defender);

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
        defenderState.setBench(bench);

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