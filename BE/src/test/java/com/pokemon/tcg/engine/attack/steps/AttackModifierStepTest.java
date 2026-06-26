package com.pokemon.tcg.engine.attack.steps;

import com.pokemon.tcg.domain.model.card.Card;
import com.pokemon.tcg.domain.model.card.CardType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.engine.CardLookupPort;
import com.pokemon.tcg.engine.DamageModifier;
import com.pokemon.tcg.engine.attack.AttackChain;
import com.pokemon.tcg.engine.attack.AttackContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttackModifierStepTest {

    @Mock
    private CardLookupPort cardLookupPort;

    @Mock
    private AttackChain chain;

    private AttackModifierStep step;

    private static final String MUSCLE_BAND_ID = "xy1-121";
    private static final String HARD_CHARM_ID  = "xy1-119";

    @BeforeEach
    void setUp() {
        step = new AttackModifierStep(cardLookupPort);
    }

    @Test
    void execute_shouldAddMuscleBandModifier_whenAttackerHasMuscleBand() {
        ActivePokemon attacker = pokemonWithTool("inst-att", MUSCLE_BAND_ID);
        ActivePokemon defender = pokemonWithTool("inst-def", null);
        AttackContext ctx = contextWith(attacker, defender);

        when(cardLookupPort.findCardById(MUSCLE_BAND_ID))
                .thenReturn(Optional.of(toolCard(MUSCLE_BAND_ID, "Muscle Band")));

        step.execute(ctx, chain);

        assertThat(ctx.getModifiers()).hasSize(1);
        DamageModifier mod = ctx.getModifiers().get(0);
        assertThat(mod.amount()).isEqualTo(20);
        assertThat(mod.beforeWeakness()).isTrue();
        verify(chain).next(ctx);
    }

    @Test
    void execute_shouldAddHardCharmModifier_whenDefenderHasHardCharm() {
        ActivePokemon attacker = pokemonWithTool("inst-att", null);
        ActivePokemon defender = pokemonWithTool("inst-def", HARD_CHARM_ID);
        AttackContext ctx = contextWith(attacker, defender);

        when(cardLookupPort.findCardById(HARD_CHARM_ID))
                .thenReturn(Optional.of(toolCard(HARD_CHARM_ID, "Hard Charm")));

        step.execute(ctx, chain);

        assertThat(ctx.getModifiers()).hasSize(1);
        DamageModifier mod = ctx.getModifiers().get(0);
        assertThat(mod.amount()).isEqualTo(-20);
        assertThat(mod.beforeWeakness()).isFalse();
        verify(chain).next(ctx);
    }

    @Test
    void execute_shouldAddBothModifiers_whenBothHaveTools() {
        ActivePokemon attacker = pokemonWithTool("inst-att", MUSCLE_BAND_ID);
        ActivePokemon defender = pokemonWithTool("inst-def", HARD_CHARM_ID);
        AttackContext ctx = contextWith(attacker, defender);

        when(cardLookupPort.findCardById(MUSCLE_BAND_ID))
                .thenReturn(Optional.of(toolCard(MUSCLE_BAND_ID, "Muscle Band")));
        when(cardLookupPort.findCardById(HARD_CHARM_ID))
                .thenReturn(Optional.of(toolCard(HARD_CHARM_ID, "Hard Charm")));

        step.execute(ctx, chain);

        assertThat(ctx.getModifiers()).hasSize(2);
        verify(chain).next(ctx);
    }

    @Test
    void execute_shouldAddNoModifiers_whenNeitherHasTool() {
        ActivePokemon attacker = pokemonWithTool("inst-att", null);
        ActivePokemon defender = pokemonWithTool("inst-def", null);
        AttackContext ctx = contextWith(attacker, defender);

        step.execute(ctx, chain);

        assertThat(ctx.getModifiers()).isEmpty();
        verify(chain).next(ctx);
        verifyNoInteractions(cardLookupPort);
    }

    private ActivePokemon pokemonWithTool(String instanceId, String toolId) {
        return ActivePokemon.builder()
                .instanceId(instanceId)
                .cardId("xy1-1")
                .attachedToolId(toolId)
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .build();
    }

    private AttackContext contextWith(ActivePokemon attacker, ActivePokemon defender) {
        PlayerState attackerState = playerState(PLAYER_1, List.of(), cardIds(5));
        attackerState.setActivePokemon(attacker);
        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(defender);
        BoardState state = boardState(attackerState, defenderState);
        GameAction act = action(GameActionType.DECLARE_ATTACK, PLAYER_1, "attackName", "Tackle");
        return AttackContext.builder()
                .boardState(state)
                .action(act)
                .modifiers(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
    }

    private Card toolCard(String id, String name) {
        return Card.builder()
                .id(id)
                .name(name)
                .supertype(CardType.TRAINER)
                .build();
    }
}
