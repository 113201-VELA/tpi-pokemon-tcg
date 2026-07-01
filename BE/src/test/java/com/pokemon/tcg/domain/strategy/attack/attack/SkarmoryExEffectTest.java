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
class SkarmoryExEffectTest {

    private SkarmoryExEffect effect;

    @BeforeEach
    void setUp() {
        effect = new SkarmoryExEffect();
    }

    @Test
    void joust_shouldDiscardOpponentsAttachedTool() {
        AttackContext ctx = buildContext("Joust");
        ctx.getBoardState().getStateFor(PLAYER_2).getActivePokemon()
                .setAttachedToolId("xy1-121");

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState().getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getAttachedToolId()).isNull();
        assertThat(ctx.getBoardState().getStateFor(PLAYER_2).getDiscard()).contains("xy1-121");
    }

    @Test
    void joust_shouldDoNothing_whenDefenderHasNoTool() {
        AttackContext ctx = buildContext("Joust");

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState().getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getAttachedToolId()).isNull();
        assertThat(ctx.getBoardState().getStateFor(PLAYER_2).getDiscard()).isEmpty();
    }

    @Test
    void tailspinPiledriver_shouldAdd40Damage_whenDefenderHasDamageCounters() {
        AttackContext ctx = buildContext("Tailspin Piledriver");
        ctx.getBoardState().getStateFor(PLAYER_2).getActivePokemon().setDamageCounters(3);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).hasSize(1);
        DamageModifierAssertHelper(ctx);
    }

    @Test
    void tailspinPiledriver_shouldNotAddDamage_whenDefenderHasNoDamageCounters() {
        AttackContext ctx = buildContext("Tailspin Piledriver");

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    private void DamageModifierAssertHelper(AttackContext ctx) {
        var mod = ctx.getModifiers().get(0);
        assertThat(mod.amount()).isEqualTo(40);
        assertThat(mod.beforeWeakness()).isTrue();
    }

    private AttackContext buildContext(String attackName) {
        ActivePokemon skarmory = ActivePokemon.builder()
                .instanceId("skarmory-1")
                .cardId("xy1-80")
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
        attackerState.setActivePokemon(skarmory);

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