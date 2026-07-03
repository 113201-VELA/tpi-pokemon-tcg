package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.Card;
import com.pokemon.tcg.domain.model.card.CardType;
import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.engine.CardLookupPort;
import com.pokemon.tcg.engine.StatusEffectManager;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VivillonEffectTest {

    @Mock
    private CardLookupPort cardLookupPort;
    @Mock
    private StatusEffectManager statusEffectManager;

    private VivillonEffect effect;

    private static final String FIRE_ENERGY_ID   = "xy1-133";
    private static final String WATER_ENERGY_ID  = "xy1-134";
    private static final String RAINBOW_ID       = "xy1-131";

    @BeforeEach
    void setUp() {
        effect = new VivillonEffect(cardLookupPort, statusEffectManager);
        doAnswer(invocation -> {
            ActivePokemon pokemon = invocation.getArgument(0);
            SpecialCondition condition = invocation.getArgument(1);
            Set<SpecialCondition> conditions = new HashSet<>(
                    pokemon.getConditions() != null ? pokemon.getConditions() : new HashSet<>());
            if (condition == SpecialCondition.ASLEEP
                    || condition == SpecialCondition.CONFUSED
                    || condition == SpecialCondition.PARALYZED) {
                conditions.remove(SpecialCondition.ASLEEP);
                conditions.remove(SpecialCondition.CONFUSED);
                conditions.remove(SpecialCondition.PARALYZED);
            }
            conditions.add(condition);
            pokemon.setConditions(conditions);
            return null;
        }).when(statusEffectManager).applyCondition(any(), any());
    }

    @Test
    void conversionPowder_shouldApplyAsleep_whenChosenConditionIsAsleep() {
        AttackContext ctx = buildContext("Conversion Powder", List.of(), "ASLEEP");

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getConditions())
                .contains(SpecialCondition.ASLEEP);
    }

    @Test
    void conversionPowder_shouldApplyPoisoned_whenChosenConditionIsPoisoned() {
        AttackContext ctx = buildContext("Conversion Powder", List.of(), "POISONED");

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getConditions())
                .contains(SpecialCondition.POISONED);
    }

    @Test
    void conversionPowder_shouldReplaceConfused_whenApplyingAsleep() {
        AttackContext ctx = buildContext("Conversion Powder", List.of(), "ASLEEP");
        ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getConditions().add(SpecialCondition.CONFUSED);

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState()
                .getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getConditions()).contains(SpecialCondition.ASLEEP);
        assertThat(defender.getConditions()).doesNotContain(SpecialCondition.CONFUSED);
    }

    @Test
    void conversionPowder_shouldReplaceParalyzed_whenApplyingAsleep() {
        AttackContext ctx = buildContext("Conversion Powder", List.of(), "ASLEEP");
        ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getConditions().add(SpecialCondition.PARALYZED);

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState()
                .getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getConditions()).contains(SpecialCondition.ASLEEP);
        assertThat(defender.getConditions()).doesNotContain(SpecialCondition.PARALYZED);
    }

    @Test
    void conversionPowder_shouldCoexistWithBurned_whenApplyingPoisoned() {
        AttackContext ctx = buildContext("Conversion Powder", List.of(), "POISONED");
        ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getConditions().add(SpecialCondition.BURNED);

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState()
                .getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getConditions())
                .contains(SpecialCondition.POISONED, SpecialCondition.BURNED);
    }

    @Test
    void conversionPowder_shouldDoNothing_whenChosenConditionIsNull() {
        AttackContext ctx = buildContext("Conversion Powder", List.of(), null);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getConditions()).isEmpty();
    }

    @Test
    void conversionPowder_shouldDoNothing_whenChosenConditionIsInvalid() {
        AttackContext ctx = buildContext("Conversion Powder", List.of(), "PARALYZED");

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getConditions()).isEmpty();
    }

    @Test
    void colorfulWind_shouldAdd30Damage_whenOneTypeAttached() {
        stubEnergy(FIRE_ENERGY_ID, "Fire Energy", EnergyType.FIRE);
        AttackContext ctx = buildContext("Colorful Wind", List.of(FIRE_ENERGY_ID), null);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).hasSize(1);
        assertThat(ctx.getModifiers().get(0).amount()).isEqualTo(30);
    }

    @Test
    void colorfulWind_shouldAdd60Damage_whenTwoDifferentTypesAttached() {
        stubEnergy(FIRE_ENERGY_ID, "Fire Energy", EnergyType.FIRE);
        stubEnergy(WATER_ENERGY_ID, "Water Energy", EnergyType.WATER);
        AttackContext ctx = buildContext("Colorful Wind",
                List.of(FIRE_ENERGY_ID, WATER_ENERGY_ID), null);

        effect.apply(ctx);

        assertThat(ctx.getModifiers().get(0).amount()).isEqualTo(60);
    }

    @Test
    void colorfulWind_shouldNotDoublecountSameType_whenTwoSameEnergyAttached() {
        stubEnergy(FIRE_ENERGY_ID, "Fire Energy", EnergyType.FIRE);
        AttackContext ctx = buildContext("Colorful Wind",
                List.of(FIRE_ENERGY_ID, FIRE_ENERGY_ID), null);

        effect.apply(ctx);

        assertThat(ctx.getModifiers().get(0).amount()).isEqualTo(30);
    }

    @Test
    void colorfulWind_shouldAddZeroDamage_whenNoEnergiesAttached() {
        AttackContext ctx = buildContext("Colorful Wind", List.of(), null);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    @Test
    void colorfulWind_shouldCountRainbowAsNewType_whenNotAllTypesCovered() {
        stubEnergy(FIRE_ENERGY_ID, "Fire Energy", EnergyType.FIRE);
        stubRainbow(RAINBOW_ID);
        AttackContext ctx = buildContext("Colorful Wind",
                List.of(FIRE_ENERGY_ID, RAINBOW_ID), null);

        effect.apply(ctx);

        assertThat(ctx.getModifiers().get(0).amount()).isEqualTo(60);
    }

    @Test
    void colorfulWind_shouldIgnoreRainbow_whenAllTypesCovered() {
        stubEnergy("e1",  "Grass Energy",     EnergyType.GRASS);
        stubEnergy("e2",  "Fire Energy",      EnergyType.FIRE);
        stubEnergy("e3",  "Water Energy",     EnergyType.WATER);
        stubEnergy("e4",  "Lightning Energy", EnergyType.LIGHTNING);
        stubEnergy("e5",  "Psychic Energy",   EnergyType.PSYCHIC);
        stubEnergy("e6",  "Fighting Energy",  EnergyType.FIGHTING);
        stubEnergy("e7",  "Darkness Energy",  EnergyType.DARKNESS);
        stubEnergy("e8",  "Metal Energy",     EnergyType.METAL);
        stubEnergy("e9",  "Fairy Energy",     EnergyType.FAIRY);
        stubEnergy("e10", "Dragon Energy",    EnergyType.DRAGON);
        stubEnergy("e11", "Colorless Energy", EnergyType.COLORLESS);
        stubRainbow(RAINBOW_ID);

        AttackContext ctx = buildContext("Colorful Wind",
                List.of("e1","e2","e3","e4","e5","e6","e7","e8","e9","e10","e11", RAINBOW_ID),
                null);

        effect.apply(ctx);

        assertThat(ctx.getModifiers().get(0).amount()).isEqualTo(330);
    }

    private AttackContext buildContext(String attackName,
                                      List<String> attachedEnergyIds,
                                      String chosenCondition) {
        ActivePokemon vivillon = ActivePokemon.builder()
                .instanceId("vivillon-1")
                .cardId("xy1-17")
                .types(new ArrayList<>(List.of(EnergyType.COLORLESS)))
                .attachedEnergyIds(new ArrayList<>(attachedEnergyIds))
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(0)
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
                .damageCounters(0)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();

        PlayerState attackerState = playerState(PLAYER_1, List.of(), cardIds(5));
        attackerState.setActivePokemon(vivillon);
        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(defender);

        BoardState state = boardState(attackerState, defenderState);

        Map<String, Object> payload = new HashMap<>();
        payload.put("attackName", attackName);
        if (chosenCondition != null) {
            payload.put("chosenCondition", chosenCondition);
        }

        GameAction act = GameAction.builder()
                .type(GameActionType.DECLARE_ATTACK)
                .playerId(PLAYER_1)
                .payload(payload)
                .build();

        return AttackContext.builder()
                .boardState(state)
                .action(act)
                .attackName(attackName)
                .modifiers(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
    }

    private void stubEnergy(String cardId, String name, EnergyType type) {
        when(cardLookupPort.findCardById(cardId)).thenReturn(Optional.of(
                Card.builder()
                        .id(cardId)
                        .name(name)
                        .supertype(CardType.ENERGY)
                        .basicEnergy(true)
                        .types(new ArrayList<>(List.of(type.name())))
                        .build()));
    }

    private void stubRainbow(String cardId) {
        when(cardLookupPort.findCardById(cardId)).thenReturn(Optional.of(
                Card.builder()
                        .id(cardId)
                        .name("Rainbow Energy")
                        .supertype(CardType.ENERGY)
                        .basicEnergy(false)
                        .types(new ArrayList<>())
                        .build()));
    }
}
