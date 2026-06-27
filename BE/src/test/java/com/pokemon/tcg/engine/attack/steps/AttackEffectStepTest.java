package com.pokemon.tcg.engine.attack.steps;

import com.pokemon.tcg.domain.model.card.Card;
import com.pokemon.tcg.domain.model.card.CardType;
import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.engine.CardLookupPort;
import com.pokemon.tcg.domain.strategy.attack.DamageModifier;
import com.pokemon.tcg.engine.attack.AttackChain;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.AttackEffect;
import com.pokemon.tcg.domain.strategy.attack.AttackEffectRegistry;
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
class AttackEffectStepTest {

    @Mock private AttackEffectRegistry registry;
    @Mock private CardLookupPort cardLookupPort;
    @Mock private AttackChain chain;

    private AttackEffectStep step;

    @BeforeEach
    void setUp() {
        step = new AttackEffectStep(registry, cardLookupPort);
    }

    @Test
    void execute_shouldApplyEffect_whenRegisteredForCardAndAttack() {
        AttackEffect mockEffect = ctx ->
                ctx.getModifiers().add(new DamageModifier("test", 20, true));

        when(cardLookupPort.findCardById("xy1-3"))
                .thenReturn(Optional.of(pokemonCard("xy1-3", "Weedle")));
        when(registry.findEffect("Weedle", "Poison Sting"))
                .thenReturn(Optional.of(mockEffect));

        AttackContext ctx = contextWithAttackerCard("xy1-3", "Poison Sting");

        step.execute(ctx, chain);

        assertThat(ctx.getModifiers()).hasSize(1);
        assertThat(ctx.getModifiers().get(0).amount()).isEqualTo(20);
        verify(chain).next(ctx);
    }

    @Test
    void execute_shouldProceedWithNoModifiers_whenNoEffectRegistered() {
        when(cardLookupPort.findCardById("xy1-3"))
                .thenReturn(Optional.of(pokemonCard("xy1-3", "Weedle")));
        when(registry.findEffect("Weedle", "Poison Sting"))
                .thenReturn(Optional.empty());

        AttackContext ctx = contextWithAttackerCard("xy1-3", "Poison Sting");

        step.execute(ctx, chain);

        assertThat(ctx.getModifiers()).isEmpty();
        verify(chain).next(ctx);
    }

    @Test
    void execute_shouldProceedWithNoModifiers_whenAttackIsNull() {
        AttackContext ctx = AttackContext.builder()
                .boardState(boardState(
                        playerState(PLAYER_1, List.of(), cardIds(5)),
                        playerState(PLAYER_2, List.of(), cardIds(5))))
                .action(action(GameActionType.DECLARE_ATTACK, PLAYER_1))
                .attack(null)
                .modifiers(new ArrayList<>())
                .events(new ArrayList<>())
                .build();

        step.execute(ctx, chain);

        assertThat(ctx.getModifiers()).isEmpty();
        verify(chain).next(ctx);
        verifyNoInteractions(registry, cardLookupPort);
    }

    private AttackContext contextWithAttackerCard(String cardId, String attackName) {
        ActivePokemon attacker = ActivePokemon.builder()
                .instanceId("att-1")
                .cardId(cardId)
                .types(new ArrayList<>(List.of(EnergyType.COLORLESS)))
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .build();

        PlayerState ps = playerState(PLAYER_1, List.of(), cardIds(5));
        ps.setActivePokemon(attacker);

        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.DECLARE_ATTACK, PLAYER_1,
                "attackName", attackName);

        return AttackContext.builder()
                .boardState(state)
                .action(act)
                .attackName(attackName)
                .attack(com.pokemon.tcg.domain.model.card.Attack.builder()
                        .name(attackName)
                        .cost(List.of(EnergyType.COLORLESS))
                        .damage("10")
                        .build())
                .modifiers(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
    }

    private Card pokemonCard(String id, String name) {
        return Card.builder()
                .id(id)
                .name(name)
                .supertype(CardType.POKEMON)
                .build();
    }
}
