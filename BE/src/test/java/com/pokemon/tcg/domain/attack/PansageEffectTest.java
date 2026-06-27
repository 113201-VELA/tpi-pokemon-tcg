package com.pokemon.tcg.domain.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.engine.attack.AttackContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

class PansageEffectTest {

    private PansageEffect effect;

    @BeforeEach
    void setUp() {
        effect = new PansageEffect();
    }

    @Test
    void apply_shouldRemoveOneDamageCounter_whenPansageIsDamaged() {
        AttackContext ctx = buildContext(3);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getDamageCounters()).isEqualTo(2);
    }

    @Test
    void apply_shouldNotGoBelowZero_whenPansageHasNoDamage() {
        AttackContext ctx = buildContext(0);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getDamageCounters()).isEqualTo(0);
    }

    @Test
    void apply_shouldRemoveOneDamageCounter_whenPansageHasExactlyOne() {
        AttackContext ctx = buildContext(1);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getDamageCounters()).isEqualTo(0);
    }

    @Test
    void apply_shouldNotAffectDefender() {
        AttackContext ctx = buildContext(3);
        ActivePokemon defender = ctx.getBoardState()
                .getStateFor(PLAYER_2).getActivePokemon();
        int initialCounters = defender.getDamageCounters();

        effect.apply(ctx);

        assertThat(defender.getDamageCounters()).isEqualTo(initialCounters);
    }

    @Test
    void apply_shouldNotAddModifiers() {
        AttackContext ctx = buildContext(3);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    private AttackContext buildContext(int pansageDamageCounters) {
        ActivePokemon pansage = ActivePokemon.builder()
                .instanceId("pansage-1")
                .cardId("xy1-10")
                .types(new ArrayList<>(List.of(EnergyType.GRASS)))
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(pansageDamageCounters)
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
        attackerState.setActivePokemon(pansage);
        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(defender);

        BoardState state = boardState(attackerState, defenderState);
        GameAction act = action(GameActionType.DECLARE_ATTACK, PLAYER_1,
                "attackName", "Leech Seed");

        return AttackContext.builder()
                .boardState(state)
                .action(act)
                .attackName("Leech Seed")
                .modifiers(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
    }
}
