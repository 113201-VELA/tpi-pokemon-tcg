package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

class MBlastoiseExEffectTest {

    private MBlastoiseExEffect effect;

    @BeforeEach
    void setUp() {
        effect = new MBlastoiseExEffect();
    }

    // ─── getSupportedAttacks ──────────────────────────────────────────────────

    @Test
    void shouldSupportHydroBombard() {
        assertThat(effect.getSupportedAttacks())
                .containsExactly("m blastoise-ex|hydro bombard");
    }

    // ─── Hydro Bombard ────────────────────────────────────────────────────────

    @Test
    void hydroBombard_shouldDamage2BenchedPokemon_whenOpponentHas2OrMore() {
        AttackContext ctx = buildContext("hydro bombard", 3);

        effect.apply(ctx);

        PlayerState opponent = ctx.getBoardState().getStateFor(PLAYER_2);
        assertThat(opponent.getBench().get(0).getDamageCounters()).isEqualTo(3);
        assertThat(opponent.getBench().get(1).getDamageCounters()).isEqualTo(3);
        assertThat(opponent.getBench().get(2).getDamageCounters()).isEqualTo(0); // untouched
    }

    @Test
    void hydroBombard_shouldDamage1BenchedPokemon_whenOpponentHasOnly1() {
        AttackContext ctx = buildContext("hydro bombard", 1);

        effect.apply(ctx);

        PlayerState opponent = ctx.getBoardState().getStateFor(PLAYER_2);
        assertThat(opponent.getBench().get(0).getDamageCounters()).isEqualTo(3);
    }

    @Test
    void hydroBombard_shouldDoNoBenchDamage_whenOpponentHasNoBench() {
        AttackContext ctx = buildContext("hydro bombard", 0);

        effect.apply(ctx);

        // No exception and active is unaffected by bench damage
        PlayerState opponent = ctx.getBoardState().getStateFor(PLAYER_2);
        assertThat(opponent.getBench()).isEmpty();
    }

    @Test
    void hydroBombard_shouldNotAffectAttacker() {
        AttackContext ctx = buildContext("hydro bombard", 2);
        int attackerCounters = ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getDamageCounters();

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getDamageCounters())
                .isEqualTo(attackerCounters);
    }

    @Test
    void hydroBombard_shouldNotAffectOpponentActive() {
        AttackContext ctx = buildContext("hydro bombard", 2);
        int defenderCounters = ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getDamageCounters();

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getDamageCounters())
                .isEqualTo(defenderCounters);
    }

    @Test
    void hydroBombard_shouldNotAddModifiers() {
        AttackContext ctx = buildContext("hydro bombard", 2);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    @Test
    void unknownAttack_shouldDoNothing() {
        AttackContext ctx = buildContext("unknown", 2);

        effect.apply(ctx);

        PlayerState opponent = ctx.getBoardState().getStateFor(PLAYER_2);
        opponent.getBench().forEach(b ->
                assertThat(b.getDamageCounters()).isEqualTo(0));
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private AttackContext buildContext(String attackName, int opponentBenchSize) {
        ActivePokemon mBlastoiseEx = ActivePokemon.builder()
                .instanceId("m-blastoise-ex-1")
                .cardId("xy1-30")
                .types(new ArrayList<>(List.of(EnergyType.WATER)))
                .attachedEnergyIds(new ArrayList<>(
                        List.of("xy1-131", "xy1-131", "xy1-131")))
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
        attackerState.setActivePokemon(mBlastoiseEx);

        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(defender);

        List<BenchPokemon> bench = new ArrayList<>();
        for (int i = 0; i < opponentBenchSize; i++) {
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