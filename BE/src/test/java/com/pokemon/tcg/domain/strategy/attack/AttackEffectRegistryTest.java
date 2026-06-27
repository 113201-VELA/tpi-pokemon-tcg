package com.pokemon.tcg.domain.strategy.attack;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AttackEffectRegistryTest {

    private AttackEffectRegistry registry;

    @BeforeEach
    void setUp() {
        AttackEffect weedleEffect = mock(AttackEffect.class);
        when(weedleEffect.getSupportedAttacks()).thenReturn(List.of("weedle|poison sting"));

        AttackEffect beedrillEffect = mock(AttackEffect.class);
        when(beedrillEffect.getSupportedAttacks()).thenReturn(List.of("beedrill|poison jab", "beedrill|flash needle"));

        AttackEffect chesnaughtEffect = mock(AttackEffect.class);
        when(chesnaughtEffect.getSupportedAttacks()).thenReturn(List.of("chesnaught|touchdown"));

        registry = new AttackEffectRegistry(List.of(weedleEffect, beedrillEffect, chesnaughtEffect));
    }

    @Test
    void findEffect_shouldReturnEffectForKnownAttack() {
        assertThat(registry.findEffect("Weedle", "Poison Sting")).isPresent();
        assertThat(registry.findEffect("Beedrill", "Poison Jab")).isPresent();
        assertThat(registry.findEffect("Chesnaught", "Touchdown")).isPresent();
    }

    @Test
    void findEffect_shouldBeCaseInsensitive() {
        assertThat(registry.findEffect("WEEDLE", "POISON STING")).isPresent();
        assertThat(registry.findEffect("weedle", "poison sting")).isPresent();
    }

    @Test
    void findEffect_shouldReturnEmptyForUnknownAttack() {
        assertThat(registry.findEffect("Pikachu", "Thunder")).isEmpty();
    }

    @Test
    void findEffect_shouldReturnEmptyForNullCardName() {
        assertThat(registry.findEffect(null, "Poison Sting")).isEmpty();
    }

    @Test
    void findEffect_shouldReturnEmptyForNullAttackName() {
        assertThat(registry.findEffect("Weedle", null)).isEmpty();
    }

    @Test
    void findEffect_shouldReturnEmptyForBothNull() {
        assertThat(registry.findEffect(null, null)).isEmpty();
    }
}