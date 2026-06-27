package com.pokemon.tcg.engine.ability;

import com.pokemon.tcg.domain.strategy.ability.ability.ChesnaughtAbility;
import com.pokemon.tcg.domain.strategy.ability.PassiveAbilityRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PassiveAbilityRegistryTest {

    private PassiveAbilityRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new PassiveAbilityRegistry(mock(ChesnaughtAbility.class));
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