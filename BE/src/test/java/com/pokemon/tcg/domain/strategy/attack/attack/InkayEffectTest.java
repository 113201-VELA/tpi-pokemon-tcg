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
class InkayEffectTest {

    private InkayEffect effect;

    @BeforeEach
    void setUp() {
        effect = new InkayEffect();
    }

    @Test
    void confusionWave_shouldConfuseBothActivePokemon() {
        AttackContext ctx = buildContext("Confusion Wave");

        effect.apply(ctx);

        ActivePokemon attacker = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        ActivePokemon defender = ctx.getBoardState().getStateFor(PLAYER_2).getActivePokemon();

        assertThat(attacker.getConditions()).contains(SpecialCondition.CONFUSED);
        assertThat(defender.getConditions()).contains(SpecialCondition.CONFUSED);
    }

    @Test
    void confusionWave_shouldNotRemoveExistingConditions() {
        AttackContext ctx = buildContext("Confusion Wave");
        ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon()
                .getConditions().add(SpecialCondition.POISONED);

        effect.apply(ctx);

        ActivePokemon attacker = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        assertThat(attacker.getConditions())
                .contains(SpecialCondition.CONFUSED, SpecialCondition.POISONED);
    }

    @Test
    void otherAttack_shouldDoNothing() {
        AttackContext ctx = buildContext("Scratch");

        effect.apply(ctx);

        ActivePokemon attacker = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        ActivePokemon defender = ctx.getBoardState().getStateFor(PLAYER_2).getActivePokemon();

        assertThat(attacker.getConditions()).isEmpty();
        assertThat(defender.getConditions()).isEmpty();
    }

    private AttackContext buildContext(String attackName) {
        ActivePokemon inkay = ActivePokemon.builder()
                .instanceId("inkay-1")
                .cardId("xy1-74")
                .types(new ArrayList<>(List.of(EnergyType.DARKNESS)))
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();

        PlayerState attackerState = playerState(PLAYER_1, List.of(), new ArrayList<>());
        attackerState.setActivePokemon(inkay);

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