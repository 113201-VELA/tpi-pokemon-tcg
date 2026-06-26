package com.pokemon.tcg.engine;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.card.TypeModifier;
import com.pokemon.tcg.domain.model.game.ActivePokemon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DamageCalculatorTest {

    private DamageCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new DamageCalculator();
    }

    @Test
    void calculate_shouldReturnZero_whenBaseDamageIsZero() {
        ActivePokemon attacker = attackerWithType(EnergyType.FIRE);
        ActivePokemon defender = defenderWithWeakness(EnergyType.FIRE);

        assertThat(calculator.calculate(attacker, defender, 0, List.of())).isEqualTo(0);
    }

    @Test
    void calculate_shouldApplyWeakness() {
        ActivePokemon attacker = attackerWithType(EnergyType.FIRE);
        ActivePokemon defender = defenderWithWeakness(EnergyType.FIRE);

        assertThat(calculator.calculate(attacker, defender, 80, List.of())).isEqualTo(160);
    }

    @Test
    void calculate_shouldApplyResistance() {
        ActivePokemon attacker = attackerWithType(EnergyType.WATER);
        ActivePokemon defender = defenderWithResistance(EnergyType.WATER);

        assertThat(calculator.calculate(attacker, defender, 50, List.of())).isEqualTo(30);
    }

    @Test
    void calculate_shouldNotGoBelowZeroAfterResistance() {
        ActivePokemon attacker = attackerWithType(EnergyType.LIGHTNING);
        ActivePokemon defender = defenderWithResistance(EnergyType.LIGHTNING);

        assertThat(calculator.calculate(attacker, defender, 10, List.of())).isEqualTo(0);
    }

    @Test
    void calculate_shouldApplyWeaknessBeforeResistance() {
        ActivePokemon attacker = attackerWithType(EnergyType.FIRE);
        ActivePokemon defender = defenderWithWeaknessAndResistance(EnergyType.FIRE, EnergyType.FIRE);

        assertThat(calculator.calculate(attacker, defender, 50, List.of())).isEqualTo(80);
    }

    @Test
    void calculate_shouldApplyPreWeaknessModifier() {
        ActivePokemon attacker = attackerWithType(EnergyType.FIRE);
        ActivePokemon defender = defenderWithWeakness(EnergyType.FIRE);
        List<DamageModifier> modifiers = List.of(new DamageModifier("muscle-band", +20, true));

        assertThat(calculator.calculate(attacker, defender, 50, modifiers)).isEqualTo(140);
    }

    @Test
    void calculate_shouldApplyPostWeaknessModifier() {
        ActivePokemon attacker = attackerWithType(EnergyType.FIRE);
        ActivePokemon defender = defenderWithWeakness(EnergyType.FIRE);
        List<DamageModifier> modifiers = List.of(new DamageModifier("hard-charm", -20, false));

        assertThat(calculator.calculate(attacker, defender, 80, modifiers)).isEqualTo(140);
    }

    @Test
    void calculate_shouldNotApplyWeakness_whenTypesDoNotMatch() {
        ActivePokemon attacker = attackerWithType(EnergyType.WATER);
        ActivePokemon defender = defenderWithWeakness(EnergyType.FIRE);

        assertThat(calculator.calculate(attacker, defender, 60, List.of())).isEqualTo(60);
    }

    @Test
    void calculate_shouldReturnBaseDamage_whenNoWeaknessOrResistance() {
        ActivePokemon attacker = attackerWithType(EnergyType.COLORLESS);
        ActivePokemon defender = plainDefender();

        assertThat(calculator.calculate(attacker, defender, 70, List.of())).isEqualTo(70);
    }

    private ActivePokemon attackerWithType(EnergyType type) {
        return ActivePokemon.builder()
                .instanceId("attacker")
                .cardId("xy1-1")
                .types(new ArrayList<>(List.of(type)))
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .build();
    }

    private ActivePokemon defenderWithWeakness(EnergyType weaknessType) {
        return ActivePokemon.builder()
                .instanceId("defender")
                .cardId("xy1-2")
                .types(new ArrayList<>())
                .weaknesses(new ArrayList<>(List.of(
                        TypeModifier.builder().type(weaknessType).value("×2").build())))
                .resistances(new ArrayList<>())
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .build();
    }

    private ActivePokemon defenderWithResistance(EnergyType resistanceType) {
        return ActivePokemon.builder()
                .instanceId("defender")
                .cardId("xy1-2")
                .types(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>(List.of(
                        TypeModifier.builder().type(resistanceType).value("-20").build())))
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .build();
    }

    private ActivePokemon defenderWithWeaknessAndResistance(EnergyType weaknessType,
                                                             EnergyType resistanceType) {
        return ActivePokemon.builder()
                .instanceId("defender")
                .cardId("xy1-2")
                .types(new ArrayList<>())
                .weaknesses(new ArrayList<>(List.of(
                        TypeModifier.builder().type(weaknessType).value("×2").build())))
                .resistances(new ArrayList<>(List.of(
                        TypeModifier.builder().type(resistanceType).value("-20").build())))
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .build();
    }

    private ActivePokemon plainDefender() {
        return ActivePokemon.builder()
                .instanceId("defender")
                .cardId("xy1-2")
                .types(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .build();
    }
}
