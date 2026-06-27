package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.Card;
import com.pokemon.tcg.domain.model.card.CardType;
import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.DamageModifier;
import com.pokemon.tcg.engine.CardLookupPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DelphoxEffectTest {

    @Mock
    private CardLookupPort cardLookupPort;

    private DelphoxEffect effect;

    private static final String FIRE_ENERGY_1 = "xy1-133";
    private static final String FIRE_ENERGY_2 = "xy1-133b";
    private static final String WATER_ENERGY  = "xy1-134";

    @BeforeEach
    void setUp() {
        effect = new DelphoxEffect(cardLookupPort);
    }

    @Test
    void getSupportedAttacks_shouldReturn_delphoxBlazeBall() {
        assertThat(effect.getSupportedAttacks()).containsExactly("delphox|blaze ball");
    }

    @Test
    void apply_shouldAddNoModifier_whenNoFireEnergyAttached() {
        when(cardLookupPort.findCardById(WATER_ENERGY))
                .thenReturn(Optional.of(waterEnergyCard(WATER_ENERGY)));
        AttackContext ctx = buildContext(List.of(WATER_ENERGY));

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    @Test
    void apply_shouldAdd20Modifier_whenOneFireEnergyAttached() {
        when(cardLookupPort.findCardById(FIRE_ENERGY_1))
                .thenReturn(Optional.of(fireEnergyCard(FIRE_ENERGY_1)));
        when(cardLookupPort.findCardById(WATER_ENERGY))
                .thenReturn(Optional.of(waterEnergyCard(WATER_ENERGY)));
        AttackContext ctx = buildContext(List.of(FIRE_ENERGY_1, WATER_ENERGY));

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).hasSize(1);
        assertThat(ctx.getModifiers().get(0).amount()).isEqualTo(20);
        assertThat(ctx.getModifiers().get(0).beforeWeakness()).isTrue();
    }

    @Test
    void apply_shouldAdd40Modifier_whenTwoFireEnergiesAttached() {
        when(cardLookupPort.findCardById(FIRE_ENERGY_1))
                .thenReturn(Optional.of(fireEnergyCard(FIRE_ENERGY_1)));
        when(cardLookupPort.findCardById(FIRE_ENERGY_2))
                .thenReturn(Optional.of(fireEnergyCard(FIRE_ENERGY_2)));
        AttackContext ctx = buildContext(List.of(FIRE_ENERGY_1, FIRE_ENERGY_2));

        effect.apply(ctx);

        assertThat(ctx.getModifiers().get(0).amount()).isEqualTo(40);
    }

    @Test
    void apply_shouldAddNoModifier_whenNoEnergiesAttached() {
        AttackContext ctx = buildContext(List.of());

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    private AttackContext buildContext(List<String> attachedEnergies) {
        ActivePokemon delphox = ActivePokemon.builder()
                .instanceId("delphox-1")
                .cardId("xy1-26")
                .types(new ArrayList<>(List.of(EnergyType.FIRE)))
                .attachedEnergyIds(new ArrayList<>(attachedEnergies))
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();

        ActivePokemon defender = ActivePokemon.builder()
                .instanceId("def-1")
                .cardId("xy1-30")
                .types(new ArrayList<>(List.of(EnergyType.WATER)))
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();

        PlayerState attackerState = playerState(PLAYER_1, List.of(), cardIds(5));
        attackerState.setActivePokemon(delphox);
        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(defender);

        BoardState state = boardState(attackerState, defenderState);
        GameAction act = action(GameActionType.DECLARE_ATTACK, PLAYER_1,
                "attackName", "Blaze Ball");

        return AttackContext.builder()
                .boardState(state)
                .action(act)
                .attackName("Blaze Ball")
                .modifiers(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
    }

    private Card fireEnergyCard(String id) {
        return Card.builder()
                .id(id)
                .supertype(CardType.ENERGY)
                .basicEnergy(true)
                .types(new ArrayList<>(List.of(EnergyType.FIRE.name())))
                .build();
    }

    private Card waterEnergyCard(String id) {
        return Card.builder()
                .id(id)
                .supertype(CardType.ENERGY)
                .basicEnergy(true)
                .types(new ArrayList<>(List.of(EnergyType.WATER.name())))
                .build();
    }
}
