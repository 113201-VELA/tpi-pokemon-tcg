package com.pokemon.tcg.domain.strategy.ability.ability;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

class ChesnaughtAbilityTest {

    private ChesnaughtAbility ability;

    @BeforeEach
    void setUp() {
        ability = new ChesnaughtAbility();
    }

    @Test
    void onDamageReceived_shouldAdd3CountersToAttacker_whenDamageIsApplied() {
        AttackContext ctx = buildContext(30, 0);

        ability.onDamageReceived(ctx, buildDefender());

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getDamageCounters()).isEqualTo(3);
    }

    @Test
    void onDamageReceived_shouldAccumulateCounters_whenAttackerAlreadyDamaged() {
        AttackContext ctx = buildContext(30, 2);

        ability.onDamageReceived(ctx, buildDefender());

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getDamageCounters()).isEqualTo(5);
    }

    @Test
    void onDamageReceived_shouldDoNothing_whenDamageToApplyIsZero() {
        AttackContext ctx = buildContext(0, 0);

        ability.onDamageReceived(ctx, buildDefender());

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getDamageCounters()).isEqualTo(0);
    }

    @Test
    void onDamageReceived_shouldStillTrigger_whenDefenderIsKnockedOut() {
        AttackContext ctx = buildContext(30, 0);
        ActivePokemon knockedOutSnapshot = buildDefender();

        ctx.getBoardState().getStateFor(PLAYER_2).setActivePokemon(null);

        ability.onDamageReceived(ctx, knockedOutSnapshot);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getDamageCounters()).isEqualTo(3);
    }

    private AttackContext buildContext(int damageToApply, int attackerCounters) {
        ActivePokemon attacker = ActivePokemon.builder()
                .instanceId("att-1")
                .cardId("xy1-20")
                .types(new ArrayList<>(List.of(EnergyType.FIRE)))
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(attackerCounters)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();

        ActivePokemon chesnaught = ActivePokemon.builder()
                .instanceId("chesnaught-1")
                .cardId("xy1-14")
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
        attackerState.setActivePokemon(attacker);
        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(chesnaught);

        BoardState state = boardState(attackerState, defenderState);
        GameAction act = action(GameActionType.DECLARE_ATTACK, PLAYER_1,
                "attackName", "Tackle");

        return AttackContext.builder()
                .boardState(state)
                .action(act)
                .attackName("Tackle")
                .damageToApply(damageToApply)
                .modifiers(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
    }

    private ActivePokemon buildDefender() {
        return ActivePokemon.builder()
                .instanceId("chesnaught-1")
                .cardId("xy1-14")
                .types(new ArrayList<>(List.of(EnergyType.GRASS)))
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();
    }
}
