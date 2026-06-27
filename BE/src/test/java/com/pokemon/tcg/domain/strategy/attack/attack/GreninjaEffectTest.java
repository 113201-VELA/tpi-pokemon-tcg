package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

class GreninjaEffectTest {

    private GreninjaEffect effect;

    @BeforeEach
    void setUp() {
        effect = new GreninjaEffect();
    }

    @Test
    void shouldSupportMistSlash() {
        assertThat(effect.getSupportedAttacks()).containsExactly("greninja|mist slash");
    }

    @Test
    void mistSlash_shouldSetIgnoreDefenderEffects() {
        AttackContext ctx = buildContext("mist slash");

        effect.apply(ctx);

        assertThat(ctx.isIgnoreDefenderEffects()).isTrue();
    }

    @Test
    void unknownAttack_shouldNotSetIgnoreDefenderEffects() {
        AttackContext ctx = buildContext("unknown");

        effect.apply(ctx);

        assertThat(ctx.isIgnoreDefenderEffects()).isFalse();
    }

    @Test
    void mistSlash_shouldNotAddModifiers() {
        AttackContext ctx = buildContext("mist slash");

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    private AttackContext buildContext(String attackName) {
        ActivePokemon greninja = ActivePokemon.builder()
                .instanceId("greninja-1")
                .cardId("xy1-41")
                .types(new ArrayList<>(List.of(EnergyType.WATER)))
                .attachedEnergyIds(new ArrayList<>(List.of("xy1-131")))
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
        attackerState.setActivePokemon(greninja);
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