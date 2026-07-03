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

class PumpkabooEffectTest {

    private StatusEffectManager statusEffectManager;
    private PumpkabooEffect effect;

    @BeforeEach
    void setUp() {
        statusEffectManager = mock(StatusEffectManager.class);
        effect = new PumpkabooEffect(statusEffectManager);
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
    void shouldSupportConfuseRay() {
        assertThat(effect.getSupportedAttacks()).containsExactly("pumpkaboo|confuse ray");
    }

    @Test
    void confuseRay_shouldConfuseDefender() {
        AttackContext ctx = buildContext();

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getConditions())
                .contains(SpecialCondition.CONFUSED);
    }

    @Test
    void confuseRay_shouldNotAffectAttacker() {
        AttackContext ctx = buildContext();

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getConditions()).isEmpty();
    }

    @Test
    void confuseRay_shouldNotAddModifiers() {
        AttackContext ctx = buildContext();

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    @Test
    void confuseRay_shouldLeaveOtherConditionsIntact() {
        AttackContext ctx = buildContext();
        ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getConditions().add(SpecialCondition.POISONED);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getConditions())
                .contains(SpecialCondition.CONFUSED, SpecialCondition.POISONED);
    }

    // ─── helper ───────────────────────────────────────────────────────────────

    private AttackContext buildContext() {
        ActivePokemon pumpkaboo = ActivePokemon.builder()
                .instanceId("pumpkaboo-1")
                .cardId("xy1-56")
                .types(new ArrayList<>(List.of(EnergyType.PSYCHIC)))
                .attachedEnergyIds(new ArrayList<>(List.of("xy1-95")))
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
        attackerState.setActivePokemon(pumpkaboo);
        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(defender);

        BoardState state = boardState(attackerState, defenderState);

        GameAction act = GameAction.builder()
                .type(GameActionType.DECLARE_ATTACK)
                .playerId(PLAYER_1)
                .payload(Map.of("attackName", "Confuse Ray"))
                .build();

        return AttackContext.builder()
                .boardState(state)
                .action(act)
                .attackName("Confuse Ray")
                .modifiers(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
    }
}