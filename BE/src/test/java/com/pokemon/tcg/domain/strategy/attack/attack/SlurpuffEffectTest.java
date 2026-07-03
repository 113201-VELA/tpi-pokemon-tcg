package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

class SlurpuffEffectTest {

    private SlurpuffEffect effect;

    @BeforeEach
    void setUp() {
        effect = new SlurpuffEffect();
    }

    @Test
    void shouldSupportOnlyDrainingKiss() {
        assertThat(effect.getSupportedAttacks()).containsExactly("slurpuff|draining kiss");
    }

    // ─── Draining Kiss ────────────────────────────────────────────────────────

    @Test
    void drainingKiss_shouldHealThreeCounters_fromSelf() {
        AttackContext ctx = buildContext("draining kiss", 5);

        effect.apply(ctx);

        ActivePokemon slurpuff = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        assertThat(slurpuff.getDamageCounters()).isEqualTo(2); // 5 - 3
    }

    @Test
    void drainingKiss_shouldClampAtZero_whenHealExceedsDamage() {
        AttackContext ctx = buildContext("draining kiss", 1);

        effect.apply(ctx);

        ActivePokemon slurpuff = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        assertThat(slurpuff.getDamageCounters()).isEqualTo(0);
    }

    @Test
    void drainingKiss_shouldNotAffectDefender() {
        AttackContext ctx = buildContext("draining kiss", 5);
        int defenderBefore = ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getDamageCounters();

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getDamageCounters()).isEqualTo(defenderBefore);
    }

    @Test
    void drainingKiss_shouldNotAddDamageModifiers() {
        AttackContext ctx = buildContext("draining kiss", 5);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    // ─── unknown attack ───────────────────────────────────────────────────────

    @Test
    void unknownAttack_shouldDoNothing() {
        AttackContext ctx = buildContext("unknown", 5);

        effect.apply(ctx);

        ActivePokemon slurpuff = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        assertThat(slurpuff.getDamageCounters()).isEqualTo(5);
    }

    // ─── helper ───────────────────────────────────────────────────────────────

    private AttackContext buildContext(String attackName, int slurpuffDamageCounters) {
        ActivePokemon slurpuff = ActivePokemon.builder()
                .instanceId("slurpuff-1")
                .cardId("xy1-95")
                .types(new ArrayList<>(List.of(EnergyType.FAIRY)))
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(slurpuffDamageCounters)
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
        attackerState.setActivePokemon(slurpuff);

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