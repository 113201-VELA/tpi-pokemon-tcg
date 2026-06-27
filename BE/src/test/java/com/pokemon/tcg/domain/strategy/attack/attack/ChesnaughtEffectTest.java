package com.pokemon.tcg.domain.strategy.attack.attack;

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

class ChesnaughtEffectTest {

    private ChesnaughtEffect effect;

    @BeforeEach
    void setUp() {
        effect = new ChesnaughtEffect();
    }

    @Test
    void apply_shouldRemoveTwoDamageCounters_whenChesnaughtIsDamaged() {
        AttackContext ctx = buildContext(5);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getDamageCounters()).isEqualTo(3);
    }

    @Test
    void apply_shouldNotGoBelowZero_whenChesnaughtHasLessThanTwoCounters() {
        AttackContext ctx = buildContext(1);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getDamageCounters()).isEqualTo(0);
    }

    @Test
    void apply_shouldNotGoBelowZero_whenChesnaughtHasNoDamage() {
        AttackContext ctx = buildContext(0);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getDamageCounters()).isEqualTo(0);
    }

    @Test
    void apply_shouldRemoveExactlyTwo_whenChesnaughtHasTwoCounters() {
        AttackContext ctx = buildContext(2);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getDamageCounters()).isEqualTo(0);
    }

    @Test
    void apply_shouldNotAffectDefender() {
        AttackContext ctx = buildContext(3);
        int defenderCounters = ctx.getBoardState()
                .getStateFor(PLAYER_2).getActivePokemon().getDamageCounters();

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getDamageCounters()).isEqualTo(defenderCounters);
    }

    private AttackContext buildContext(int chesnaughtDamageCounters) {
        ActivePokemon chesnaught = ActivePokemon.builder()
                .instanceId("chesnaught-1")
                .cardId("xy1-14")
                .types(new ArrayList<>(List.of(EnergyType.GRASS)))
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(chesnaughtDamageCounters)
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
        attackerState.setActivePokemon(chesnaught);
        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(defender);

        BoardState state = boardState(attackerState, defenderState);
        GameAction act = action(GameActionType.DECLARE_ATTACK, PLAYER_1,
                "attackName", "Touchdown");

        return AttackContext.builder()
                .boardState(state)
                .action(act)
                .attackName("Touchdown")
                .modifiers(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
    }
}
