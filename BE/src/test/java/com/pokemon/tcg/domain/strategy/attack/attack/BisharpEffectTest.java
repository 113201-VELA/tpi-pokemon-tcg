package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class BisharpEffectTest {

    private BisharpEffect effect;

    @BeforeEach
    void setUp() {
        effect = new BisharpEffect();
    }

    @Test
    void metalSound_shouldConfuseDefender() {
        AttackContext ctx = buildContext("Metal Sound");

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState().getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getConditions()).contains(SpecialCondition.CONFUSED);
    }

    @Test
    void metalWallop_firstUse_shouldSetPendingBoost_withoutAddingModifier() {
        AttackContext ctx = buildContext("Metal Wallop");

        effect.apply(ctx);

        ActivePokemon attacker = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        assertThat(attacker.getPendingAttackDamageBoostName()).isEqualTo("metal wallop");
        assertThat(attacker.getPendingAttackDamageBoostAmount()).isEqualTo(40);
        assertThat(ctx.getModifiers()).isEmpty();
    }

    @Test
    void metalWallop_secondUse_shouldApplyBoostModifier_andClearPending() {
        AttackContext ctx = buildContext("Metal Wallop");
        ActivePokemon attacker = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        attacker.setPendingAttackDamageBoostName("metal wallop");
        attacker.setPendingAttackDamageBoostAmount(40);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).hasSize(1);
        var mod = ctx.getModifiers().get(0);
        assertThat(mod.amount()).isEqualTo(40);
        assertThat(mod.beforeWeakness()).isTrue();
        assertThat(attacker.getPendingAttackDamageBoostName()).isNull();
        assertThat(attacker.getPendingAttackDamageBoostAmount()).isZero();
    }

    @Test
    void metalWallop_pendingBoostFromDifferentAttack_shouldNotApply() {
        AttackContext ctx = buildContext("Metal Wallop");
        ActivePokemon attacker = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        attacker.setPendingAttackDamageBoostName("some other attack");
        attacker.setPendingAttackDamageBoostAmount(999);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
        assertThat(attacker.getPendingAttackDamageBoostName()).isEqualTo("metal wallop");
        assertThat(attacker.getPendingAttackDamageBoostAmount()).isEqualTo(40);
    }

    private AttackContext buildContext(String attackName) {
        ActivePokemon bisharp = ActivePokemon.builder()
                .instanceId("bisharp-1")
                .cardId("xy1-82")
                .types(new ArrayList<>(List.of(EnergyType.METAL)))
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();

        PlayerState attackerState = playerState(PLAYER_1, List.of(), new ArrayList<>());
        attackerState.setActivePokemon(bisharp);

        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(ActivePokemon.builder()
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
                .build());

        BoardState state = boardState(attackerState, defenderState);
        GameAction act = action(GameActionType.DECLARE_ATTACK, PLAYER_1,
                "attackName", attackName);

        return AttackContext.builder()
                .boardState(state)
                .action(act)
                .attackName(attackName)
                .modifiers(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
    }
}