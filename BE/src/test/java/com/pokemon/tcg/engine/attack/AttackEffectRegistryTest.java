package com.pokemon.tcg.engine.attack;

import com.pokemon.tcg.domain.attack.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AttackEffectRegistryTest {

    private AttackEffectRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new AttackEffectRegistry(
                mock(WeedleEffect.class),
                mock(KakunaEffect.class),
                mock(BeedrillEffect.class),
                mock(LedianEffect.class),
                mock(VolbeatEffect.class),
                mock(IllumiseEffect.class),
                mock(PansageEffect.class),
                mock(SimisageEffect.class),
                mock(ChespinEffect.class),
                mock(QuilladinEffect.class),
                mock(ChesnaughtEffect.class)
        );
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