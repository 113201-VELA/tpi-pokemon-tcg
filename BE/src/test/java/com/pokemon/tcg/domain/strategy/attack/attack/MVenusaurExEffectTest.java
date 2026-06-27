package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

class MVenusaurExEffectTest {

    private MVenusaurExEffect effect;

    @BeforeEach
    void setUp() {
        effect = new MVenusaurExEffect();
    }

    @Test
    void getSupportedAttacks_shouldReturn_mVenusaurExCrisisVine() {
        assertThat(effect.getSupportedAttacks())
                .containsExactly("m venusaur-ex|crisis vine");
    }

    @Test
    void apply_shouldParalyzeAndPoisonDefender() {
        AttackContext ctx = buildContext();

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState()
                .getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getConditions())
                .contains(SpecialCondition.PARALYZED, SpecialCondition.POISONED);
    }

    @Test
    void apply_shouldReplaceAsleep_withParalyzed_whileAddingPoisoned() {
        AttackContext ctx = buildContext();
        ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getConditions().add(SpecialCondition.ASLEEP);

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState()
                .getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getConditions())
                .contains(SpecialCondition.PARALYZED, SpecialCondition.POISONED);
        assertThat(defender.getConditions())
                .doesNotContain(SpecialCondition.ASLEEP);
    }

    @Test
    void apply_shouldReplaceConfused_withParalyzed_whileAddingPoisoned() {
        AttackContext ctx = buildContext();
        ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getConditions().add(SpecialCondition.CONFUSED);

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState()
                .getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getConditions())
                .contains(SpecialCondition.PARALYZED, SpecialCondition.POISONED);
        assertThat(defender.getConditions())
                .doesNotContain(SpecialCondition.CONFUSED);
    }

    @Test
    void apply_shouldCoexistWithBurned() {
        AttackContext ctx = buildContext();
        ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getConditions().add(SpecialCondition.BURNED);

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState()
                .getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getConditions())
                .containsExactlyInAnyOrder(
                        SpecialCondition.PARALYZED,
                        SpecialCondition.POISONED,
                        SpecialCondition.BURNED);
    }

    @Test
    void apply_shouldNotAffectAttacker() {
        AttackContext ctx = buildContext();

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getConditions()).isEmpty();
    }

    @Test
    void apply_shouldNotAddModifiers() {
        AttackContext ctx = buildContext();

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    private AttackContext buildContext() {
        ActivePokemon mVenusaur = ActivePokemon.builder()
                .instanceId("m-venusaur-ex-1")
                .cardId("xy1-2")
                .types(new ArrayList<>(List.of(EnergyType.GRASS)))
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(0)
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
                .damageCounters(0)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();

        PlayerState attackerState = playerState(PLAYER_1, List.of(), cardIds(5));
        attackerState.setActivePokemon(mVenusaur);
        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(defender);

        BoardState state = boardState(attackerState, defenderState);
        GameAction act = action(GameActionType.DECLARE_ATTACK, PLAYER_1,
                "attackName", "Crisis Vine");

        return AttackContext.builder()
                .boardState(state)
                .action(act)
                .attackName("Crisis Vine")
                .modifiers(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
    }
}
