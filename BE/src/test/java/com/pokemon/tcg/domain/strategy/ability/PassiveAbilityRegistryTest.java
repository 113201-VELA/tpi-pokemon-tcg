package com.pokemon.tcg.domain.strategy.ability;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PassiveAbilityRegistryTest {

    private PassiveAbilityRegistry registry;

    @BeforeEach
    void setUp() {
        PassiveAbilityEffect chesnaughtAbility = mock(PassiveAbilityEffect.class);
        when(chesnaughtAbility.getIdentifier()).thenReturn("chesnaught");

        registry = new PassiveAbilityRegistry(List.of(chesnaughtAbility));
    }

    @Test
    void findAbility_shouldReturnEffectForKnownCard() {
        assertThat(registry.findAbility("chesnaught")).isPresent();
    }

    @Test
    void findAbility_shouldBeCaseInsensitive() {
        assertThat(registry.findAbility("Chesnaught")).isPresent();
        assertThat(registry.findAbility("CHESNAUGHT")).isPresent();
    }

    @Test
    void findAbility_shouldReturnEmptyForUnknownCard() {
        assertThat(registry.findAbility("pikachu")).isEmpty();
    }

    @Test
    void findAbility_shouldReturnEmptyForNull() {
        assertThat(registry.findAbility(null)).isEmpty();
    }
}