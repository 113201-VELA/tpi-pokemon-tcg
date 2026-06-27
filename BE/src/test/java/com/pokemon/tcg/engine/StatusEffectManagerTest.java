package com.pokemon.tcg.engine;

import com.pokemon.tcg.domain.model.game.ActivePokemon;
import com.pokemon.tcg.domain.model.game.CoinResult;
import com.pokemon.tcg.domain.model.game.SpecialCondition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class StatusEffectManagerTest {

    private CoinFlipService coinFlipService;
    private StatusEffectManager manager;

    @BeforeEach
    void setUp() {
        coinFlipService = mock(CoinFlipService.class);
        manager = new StatusEffectManager(coinFlipService);
    }

    // ─── POISON ───────────────────────────────────────────────────────────────

    @Test
    void processBetweenTurns_poisoned_shouldAddOneDamageCounter() {
        ActivePokemon pokemon = buildPokemon(Set.of(SpecialCondition.POISONED), 0);

        manager.processBetweenTurns(pokemon);

        assertThat(pokemon.getDamageCounters()).isEqualTo(1);
        assertThat(pokemon.getConditions()).contains(SpecialCondition.POISONED);
    }

    @Test
    void processBetweenTurns_poisoned_shouldAccumulateDamage() {
        ActivePokemon pokemon = buildPokemon(Set.of(SpecialCondition.POISONED), 3);

        manager.processBetweenTurns(pokemon);

        assertThat(pokemon.getDamageCounters()).isEqualTo(4);
    }

    // ─── BURN ─────────────────────────────────────────────────────────────────

    @Test
    void processBetweenTurns_burned_tailsShouldAddTwoDamageCounters() {
        when(coinFlipService.flip()).thenReturn(CoinResult.TAILS);
        ActivePokemon pokemon = buildPokemon(Set.of(SpecialCondition.BURNED), 0);

        manager.processBetweenTurns(pokemon);

        assertThat(pokemon.getDamageCounters()).isEqualTo(2);
        assertThat(pokemon.getConditions()).contains(SpecialCondition.BURNED);
    }

    @Test
    void processBetweenTurns_burned_headsShouldNotAddDamage() {
        when(coinFlipService.flip()).thenReturn(CoinResult.HEADS);
        ActivePokemon pokemon = buildPokemon(Set.of(SpecialCondition.BURNED), 0);

        manager.processBetweenTurns(pokemon);

        assertThat(pokemon.getDamageCounters()).isEqualTo(0);
        assertThat(pokemon.getConditions()).contains(SpecialCondition.BURNED);
    }

    // ─── SLEEP ────────────────────────────────────────────────────────────────

    @Test
    void processBetweenTurns_asleep_headsShouldWakeUp() {
        when(coinFlipService.flip()).thenReturn(CoinResult.HEADS);
        ActivePokemon pokemon = buildPokemon(Set.of(SpecialCondition.ASLEEP), 0);

        manager.processBetweenTurns(pokemon);

        assertThat(pokemon.getConditions()).doesNotContain(SpecialCondition.ASLEEP);
    }

    @Test
    void processBetweenTurns_asleep_tailsShouldRemainAsleep() {
        when(coinFlipService.flip()).thenReturn(CoinResult.TAILS);
        ActivePokemon pokemon = buildPokemon(Set.of(SpecialCondition.ASLEEP), 0);

        manager.processBetweenTurns(pokemon);

        assertThat(pokemon.getConditions()).contains(SpecialCondition.ASLEEP);
    }

    // ─── PARALYSIS ────────────────────────────────────────────────────────────

    @Test
    void processBetweenTurns_paralyzed_shouldCureAutomatically() {
        ActivePokemon pokemon = buildPokemon(Set.of(SpecialCondition.PARALYZED), 0);

        manager.processBetweenTurns(pokemon);

        assertThat(pokemon.getConditions()).doesNotContain(SpecialCondition.PARALYZED);
    }

    // ─── COMBINATIONS ─────────────────────────────────────────────────────────

    @Test
    void processBetweenTurns_poisonedAndBurned_shouldApplyBoth() {
        when(coinFlipService.flip()).thenReturn(CoinResult.TAILS);
        ActivePokemon pokemon = buildPokemon(
                Set.of(SpecialCondition.POISONED, SpecialCondition.BURNED), 0);

        manager.processBetweenTurns(pokemon);

        // Poison +1, Burn +2 = 3 total
        assertThat(pokemon.getDamageCounters()).isEqualTo(3);
    }

    @Test
    void processBetweenTurns_nullPokemon_shouldReturnNull() {
        assertThat(manager.processBetweenTurns(null)).isNull();
    }

    // ─── APPLY CONDITION ──────────────────────────────────────────────────────

    @Test
    void applyCondition_paralyzed_shouldReplaceAsleep() {
        ActivePokemon pokemon = buildPokemon(Set.of(SpecialCondition.ASLEEP), 0);

        manager.applyCondition(pokemon, SpecialCondition.PARALYZED);

        assertThat(pokemon.getConditions())
                .contains(SpecialCondition.PARALYZED)
                .doesNotContain(SpecialCondition.ASLEEP);
    }

    @Test
    void applyCondition_confused_shouldReplaceParalyzed() {
        ActivePokemon pokemon = buildPokemon(Set.of(SpecialCondition.PARALYZED), 0);

        manager.applyCondition(pokemon, SpecialCondition.CONFUSED);

        assertThat(pokemon.getConditions())
                .contains(SpecialCondition.CONFUSED)
                .doesNotContain(SpecialCondition.PARALYZED);
    }

    @Test
    void applyCondition_asleep_shouldReplaceConfused() {
        ActivePokemon pokemon = buildPokemon(Set.of(SpecialCondition.CONFUSED), 0);

        manager.applyCondition(pokemon, SpecialCondition.ASLEEP);

        assertThat(pokemon.getConditions())
                .contains(SpecialCondition.ASLEEP)
                .doesNotContain(SpecialCondition.CONFUSED);
    }

    @Test
    void applyCondition_poisoned_shouldCoexistWithBurned() {
        ActivePokemon pokemon = buildPokemon(Set.of(SpecialCondition.BURNED), 0);

        manager.applyCondition(pokemon, SpecialCondition.POISONED);

        assertThat(pokemon.getConditions())
                .contains(SpecialCondition.POISONED)
                .contains(SpecialCondition.BURNED);
    }

    @Test
    void applyCondition_burned_shouldCoexistWithParalyzed() {
        ActivePokemon pokemon = buildPokemon(Set.of(SpecialCondition.PARALYZED), 0);

        manager.applyCondition(pokemon, SpecialCondition.BURNED);

        assertThat(pokemon.getConditions())
                .contains(SpecialCondition.BURNED)
                .contains(SpecialCondition.PARALYZED);
    }

    // ─── CLEAR CONDITIONS ─────────────────────────────────────────────────────

    @Test
    void clearAllConditions_shouldRemoveAllConditions() {
        ActivePokemon pokemon = buildPokemon(
                Set.of(SpecialCondition.POISONED, SpecialCondition.BURNED), 0);

        manager.clearAllConditions(pokemon);

        assertThat(pokemon.getConditions()).isEmpty();
    }

    @Test
    void clearAllConditions_nullPokemon_shouldReturnNull() {
        assertThat(manager.clearAllConditions(null)).isNull();
    }

    // ─── HELPER ───────────────────────────────────────────────────────────────

    private ActivePokemon buildPokemon(Set<SpecialCondition> conditions, int damageCounters) {
        return ActivePokemon.builder()
                .instanceId("inst-1")
                .cardId("xy1-1")
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .damageCounters(damageCounters)
                .conditions(new HashSet<>(conditions))
                .build();
    }
}