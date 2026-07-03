package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.engine.StatusEffectManager;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class VenusaurExEffectTest {

    private StatusEffectManager statusEffectManager;
    private VenusaurExEffect effect;

    @BeforeEach
    void setUp() {
        statusEffectManager = mock(StatusEffectManager.class);
        effect = new VenusaurExEffect(statusEffectManager);
        doAnswer(invocation -> {
            ActivePokemon pokemon = invocation.getArgument(0);
            SpecialCondition condition = invocation.getArgument(1);
            Set<SpecialCondition> conditions = new HashSet<>(
                    pokemon.getConditions() != null ? pokemon.getConditions() : new HashSet<>());
            if (condition == SpecialCondition.ASLEEP
                    || condition == SpecialCondition.CONFUSED
                    || condition == SpecialCondition.PARALYZED) {
                conditions.remove(SpecialCondition.ASLEEP);
                conditions.remove(SpecialCondition.CONFUSED);
                conditions.remove(SpecialCondition.PARALYZED);
            }
            conditions.add(condition);
            pokemon.setConditions(conditions);
            return null;
        }).when(statusEffectManager).applyCondition(any(), any());
    }

    @Test
    void getSupportedAttacks_shouldReturnBothAttacks() {
        assertThat(effect.getSupportedAttacks()).containsExactlyInAnyOrder(
                "venusaur-ex|poison powder",
                "venusaur-ex|jungle hammer"
        );
    }

    @Test
    void poisonPowder_shouldPoisonDefender() {
        AttackContext ctx = buildContext("Poison Powder", 0);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getConditions())
                .contains(SpecialCondition.POISONED);
    }

    @Test
    void poisonPowder_shouldNotAddModifiers() {
        AttackContext ctx = buildContext("Poison Powder", 0);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    @Test
    void poisonPowder_shouldNotAffectAttacker() {
        AttackContext ctx = buildContext("Poison Powder", 0);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getConditions()).isEmpty();
    }

    @Test
    void poisonPowder_shouldCoexistWithBurned() {
        AttackContext ctx = buildContext("Poison Powder", 0);
        ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getConditions().add(SpecialCondition.BURNED);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getConditions())
                .containsExactlyInAnyOrder(
                        SpecialCondition.POISONED, SpecialCondition.BURNED);
    }

    @Test
    void jungleHammer_shouldRemove3DamageCounters() {
        AttackContext ctx = buildContext("Jungle Hammer", 5);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getDamageCounters()).isEqualTo(2);
    }

    @Test
    void jungleHammer_shouldNotGoBelowZero_whenLessThan3Counters() {
        AttackContext ctx = buildContext("Jungle Hammer", 2);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getDamageCounters()).isEqualTo(0);
    }

    @Test
    void jungleHammer_shouldNotGoBelowZero_whenNoDamage() {
        AttackContext ctx = buildContext("Jungle Hammer", 0);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getDamageCounters()).isEqualTo(0);
    }

    @Test
    void jungleHammer_shouldNotAffectDefender() {
        AttackContext ctx = buildContext("Jungle Hammer", 3);
        int defenderCounters = ctx.getBoardState()
                .getStateFor(PLAYER_2).getActivePokemon().getDamageCounters();

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getDamageCounters()).isEqualTo(defenderCounters);
    }

    @Test
    void jungleHammer_shouldNotAddModifiers() {
        AttackContext ctx = buildContext("Jungle Hammer", 3);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    private AttackContext buildContext(String attackName, int venusaurCounters) {
        ActivePokemon venusaur = ActivePokemon.builder()
                .instanceId("venusaur-ex-1")
                .cardId("xy1-1")
                .types(new ArrayList<>(List.of(EnergyType.GRASS)))
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(venusaurCounters)
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
                .damageCounters(2)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();

        PlayerState attackerState = playerState(PLAYER_1, List.of(), cardIds(5));
        attackerState.setActivePokemon(venusaur);
        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(defender);

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
