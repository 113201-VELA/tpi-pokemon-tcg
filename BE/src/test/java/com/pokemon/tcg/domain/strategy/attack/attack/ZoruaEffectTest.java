package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.model.card.EnergyType;
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
class ZoruaEffectTest {

    private ZoruaEffect effect;

    @BeforeEach
    void setUp() {
        effect = new ZoruaEffect();
    }

    @Test
    void nastyPlot_shouldSetPendingAttackSelection_whenDeckHasCards() {
        AttackContext ctx = buildContext("Nasty Plot", List.of("xy1-1", "xy1-132"));

        effect.apply(ctx);

        assertThat(ctx.getBoardState().isPendingAttackSelection()).isTrue();
        assertThat(ctx.getBoardState().getPendingAttackSelectionKey())
                .isEqualTo("zorua|nasty plot");
        assertThat(ctx.getBoardState().getPendingAttackSelectionPlayerId())
                .isEqualTo(PLAYER_1);
    }

    @Test
    void nastyPlot_shouldIncludeAllDeckCards_inPendingCards_withoutFiltering() {
        List<String> deck = List.of("xy1-1", "xy1-50", "xy1-132");
        AttackContext ctx = buildContext("Nasty Plot", deck);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getPendingDeckSelectionCardIds())
                .containsExactlyElementsOf(deck);
    }

    @Test
    void nastyPlot_shouldDoNothing_whenDeckIsEmpty() {
        AttackContext ctx = buildContext("Nasty Plot", List.of());

        effect.apply(ctx);

        assertThat(ctx.getBoardState().isPendingAttackSelection()).isFalse();
    }

    @Test
    void scratch_shouldDoNothing() {
        AttackContext ctx = buildContext("Scratch", List.of("xy1-1"));

        effect.apply(ctx);

        assertThat(ctx.getBoardState().isPendingAttackSelection()).isFalse();
        assertThat(ctx.getModifiers()).isEmpty();
    }

    private AttackContext buildContext(String attackName, List<String> deck) {
        ActivePokemon zorua = ActivePokemon.builder()
                .instanceId("zorua-1")
                .cardId("xy1-72")
                .types(new ArrayList<>(List.of(EnergyType.DARKNESS)))
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();

        PlayerState attackerState = playerState(PLAYER_1, List.of(), new ArrayList<>(deck));
        attackerState.setActivePokemon(zorua);

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