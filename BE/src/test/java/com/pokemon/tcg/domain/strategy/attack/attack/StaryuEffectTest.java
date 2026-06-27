package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

class StaryuEffectTest {

    private StaryuEffect effect;

    @BeforeEach
    void setUp() {
        effect = new StaryuEffect();
    }

    @Test
    void shouldSupportRecklessCharge() {
        assertThat(effect.getSupportedAttacks())
                .containsExactly("staryu|reckless charge");
    }

    @Test
    void recklessCharge_shouldAdd1DamageCounter_toSelf() {
        AttackContext ctx = buildContext(0);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getDamageCounters()).isEqualTo(1);
    }

    @Test
    void recklessCharge_shouldAccumulateDamage_whenAlreadyDamaged() {
        AttackContext ctx = buildContext(2);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getDamageCounters()).isEqualTo(3);
    }

    @Test
    void recklessCharge_shouldNotAffectOpponent() {
        AttackContext ctx = buildContext(0);
        int defenderCounters = ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getDamageCounters();

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getDamageCounters()).isEqualTo(defenderCounters);
    }

    @Test
    void recklessCharge_shouldNotAddModifiers() {
        AttackContext ctx = buildContext(0);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    private AttackContext buildContext(int initialSelfCounters) {
        ActivePokemon staryu = ActivePokemon.builder()
                .instanceId("staryu-1")
                .cardId("xy1-33")
                .types(new ArrayList<>(List.of(EnergyType.WATER)))
                .attachedEnergyIds(new ArrayList<>(List.of("xy1-131", "xy1-131")))
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(initialSelfCounters)
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
        attackerState.setActivePokemon(staryu);
        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(defender);

        BoardState state = boardState(attackerState, defenderState);

        GameAction act = GameAction.builder()
                .type(GameActionType.DECLARE_ATTACK)
                .playerId(PLAYER_1)
                .payload(Map.of("attackName", "Reckless Charge"))
                .build();

        return AttackContext.builder()
                .boardState(state)
                .action(act)
                .attackName("reckless charge")
                .modifiers(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
    }
}