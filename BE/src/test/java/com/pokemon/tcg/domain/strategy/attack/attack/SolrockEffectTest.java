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

class SolrockEffectTest {

    private SolrockEffect effect;

    @BeforeEach
    void setUp() {
        effect = new SolrockEffect();
    }

    @Test
    void shouldSupportBothAttacks() {
        assertThat(effect.getSupportedAttacks())
                .contains("solrock|cosmic spin", "solrock|solar beam");
    }

    // ─── Cosmic Spin — Lunatone on bench ─────────────────────────────────────

    @Test
    void cosmicSpin_shouldAdd30Modifier_whenLunatoneIsOnBench() {
        AttackContext ctx = buildContext("cosmic spin", true);

        effect.apply(ctx);

        int bonus = ctx.getModifiers().stream()
                .mapToInt(DamageModifier::amount).sum();
        assertThat(bonus).isEqualTo(30);
    }

    @Test
    void cosmicSpin_shouldNotAddModifier_whenLunatoneIsNotOnBench() {
        AttackContext ctx = buildContext("cosmic spin", false);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    @Test
    void cosmicSpin_shouldNotAddModifier_whenBenchIsEmpty() {
        AttackContext ctx = buildContext("cosmic spin", false);
        ctx.getBoardState().getStateFor(PLAYER_1).setBench(new ArrayList<>());

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    // ─── Solar Beam ───────────────────────────────────────────────────────────

    @Test
    void solarBeam_shouldNotAddModifiers() {
        AttackContext ctx = buildContext("solar beam", false);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    // ─── unknown attack ───────────────────────────────────────────────────────

    @Test
    void unknownAttack_shouldDoNothing() {
        AttackContext ctx = buildContext("unknown", true);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    // ─── helper ───────────────────────────────────────────────────────────────

    private AttackContext buildContext(String attackName, boolean lunatoneOnBench) {
        ActivePokemon solrock = ActivePokemon.builder()
                .instanceId("solrock-1")
                .cardId("xy1-64")
                .types(new ArrayList<>(List.of(EnergyType.FIGHTING)))
                .attachedEnergyIds(new ArrayList<>(List.of("xy1-96", "xy1-95")))
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

        List<BenchPokemon> bench = new ArrayList<>();
        if (lunatoneOnBench) {
            bench.add(BenchPokemon.builder()
                    .instanceId("lunatone-bench-1")
                    .cardId("xy1-63")
                    .attachedEnergyIds(new ArrayList<>())
                    .evolutionStack(new ArrayList<>())
                    .damageCounters(0)
                    .build());
        } else {
            bench.add(BenchPokemon.builder()
                    .instanceId("other-bench-1")
                    .cardId("xy1-1")
                    .attachedEnergyIds(new ArrayList<>())
                    .evolutionStack(new ArrayList<>())
                    .damageCounters(0)
                    .build());
        }

        PlayerState attackerState = playerState(PLAYER_1, List.of(), cardIds(5));
        attackerState.setActivePokemon(solrock);
        attackerState.setBench(bench);

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