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

class Wigglytuff90EffectTest {

    private Wigglytuff90Effect effect;

    private static final String ENERGY_1 = "xy1-136";
    private static final String ENERGY_2 = "xy1-131";

    @BeforeEach
    void setUp() {
        effect = new Wigglytuff90Effect();
    }

    @Test
    void shouldSupportBothAttacks() {
        assertThat(effect.getSupportedAttacks())
                .containsExactly("wigglytuff|balloon barrage", "wigglytuff|double-edge");
    }

    // ─── Balloon Barrage ──────────────────────────────────────────────────────

    @Test
    void balloonBarrage_shouldAdd20PerEnergy_attachedToSelf() {
        AttackContext ctx = buildContext("balloon barrage", List.of(ENERGY_1, ENERGY_2), 0);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).hasSize(1);
        DamageModifier mod = ctx.getModifiers().get(0);
        assertThat(mod.amount()).isEqualTo(40);
        assertThat(mod.beforeWeakness()).isTrue();
    }

    @Test
    void balloonBarrage_shouldIgnoreDefenderEnergy() {
        // Only own energy counts, defender has energy too but shouldn't matter
        AttackContext ctx = buildContext("balloon barrage", List.of(ENERGY_1), 3);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).hasSize(1);
        assertThat(ctx.getModifiers().get(0).amount()).isEqualTo(20);
    }

    @Test
    void balloonBarrage_shouldNotAddModifier_whenNoEnergyAttached() {
        AttackContext ctx = buildContext("balloon barrage", List.of(), 0);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    // ─── Double-Edge ──────────────────────────────────────────────────────────

    @Test
    void doubleEdge_shouldAddOneDamageCounterToSelf() {
        AttackContext ctx = buildContext("double-edge", List.of(), 0);

        effect.apply(ctx);

        ActivePokemon wigglytuff = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        assertThat(wigglytuff.getDamageCounters()).isEqualTo(1);
    }

    @Test
    void doubleEdge_shouldAccumulateOnExistingDamage() {
        AttackContext ctx = buildContext("double-edge", List.of(), 0);
        ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon().setDamageCounters(3);

        effect.apply(ctx);

        ActivePokemon wigglytuff = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        assertThat(wigglytuff.getDamageCounters()).isEqualTo(4);
    }

    @Test
    void doubleEdge_shouldNotAffectDefender() {
        AttackContext ctx = buildContext("double-edge", List.of(), 0);

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState().getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getDamageCounters()).isEqualTo(0);
    }

    @Test
    void doubleEdge_shouldNotAddDamageModifiers() {
        AttackContext ctx = buildContext("double-edge", List.of(), 0);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    // ─── unknown attack ───────────────────────────────────────────────────────

    @Test
    void unknownAttack_shouldDoNothing() {
        AttackContext ctx = buildContext("unknown", List.of(ENERGY_1), 0);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
        ActivePokemon wigglytuff = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        assertThat(wigglytuff.getDamageCounters()).isEqualTo(0);
    }

    // ─── helper ───────────────────────────────────────────────────────────────

    private AttackContext buildContext(String attackName,
                                       List<String> wigglytuffEnergies,
                                       int defenderDamageCounters) {
        ActivePokemon wigglytuff = ActivePokemon.builder()
                .instanceId("wigglytuff-1")
                .cardId("xy1-90")
                .types(new ArrayList<>(List.of(EnergyType.FAIRY)))
                .attachedEnergyIds(new ArrayList<>(wigglytuffEnergies))
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
                .damageCounters(defenderDamageCounters)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();

        PlayerState attackerState = playerState(PLAYER_1, List.of(), cardIds(5));
        attackerState.setActivePokemon(wigglytuff);

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