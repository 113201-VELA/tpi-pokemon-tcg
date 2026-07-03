package com.pokemon.tcg.domain.strategy.trainer.item;

import com.pokemon.tcg.domain.model.game.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

class SuperPotionEffectTest {

    private SuperPotionEffect effect;

    @BeforeEach
    void setUp() {
        effect = new SuperPotionEffect();
    }

    // ── canApply ───────────────────────────────────────────────────────────────

    @Test
    void canApply_shouldFail_whenNoTargetSpecified() {
        PlayerState ps = playerState(PLAYER_1, List.of(), cardIds(5));
        ps.setActivePokemon(activeWithId("inst-1", "xy1-3", 3, List.of("xy1-132")));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-128");

        ValidationResult result = effect.canApply(state, act);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage())
                .isEqualTo("No target Pokémon specified for Super Potion.");
    }

    @Test
    void canApply_shouldFail_whenNoEnergySpecified() {
        PlayerState ps = playerState(PLAYER_1, List.of(), cardIds(5));
        ps.setActivePokemon(activeWithId("inst-1", "xy1-3", 3, List.of("xy1-132")));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1,
                "cardId", "xy1-128", "targetInstanceId", "inst-1");

        ValidationResult result = effect.canApply(state, act);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage())
                .isEqualTo("No energy specified to discard for Super Potion.");
    }

    @Test
    void canApply_shouldFail_whenTargetNotInPlay() {
        PlayerState ps = playerState(PLAYER_1, List.of(), cardIds(5));
        ps.setActivePokemon(activeWithId("inst-1", "xy1-3", 3, List.of("xy1-132")));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1,
                "cardId", "xy1-128",
                "targetInstanceId", "nonexistent-id",
                "energyToDiscardId", "xy1-132");

        ValidationResult result = effect.canApply(state, act);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Target Pokémon is not in play.");
    }

    @Test
    void canApply_shouldFail_whenActiveTargetHasNoDamage() {
        PlayerState ps = playerState(PLAYER_1, List.of(), cardIds(5));
        ps.setActivePokemon(activeWithId("inst-1", "xy1-3", 0, List.of("xy1-132")));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1,
                "cardId", "xy1-128",
                "targetInstanceId", "inst-1",
                "energyToDiscardId", "xy1-132");

        ValidationResult result = effect.canApply(state, act);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("That Pokémon has no damage to heal.");
    }

    @Test
    void canApply_shouldFail_whenEnergyNotAttachedToActive() {
        PlayerState ps = playerState(PLAYER_1, List.of(), cardIds(5));
        ps.setActivePokemon(activeWithId("inst-1", "xy1-3", 3, List.of("xy1-132")));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1,
                "cardId", "xy1-128",
                "targetInstanceId", "inst-1",
                "energyToDiscardId", "xy1-999");

        ValidationResult result = effect.canApply(state, act);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage())
                .isEqualTo("The specified energy is not attached to that Pokémon.");
    }

    @Test
    void canApply_shouldSucceed_whenActiveIsValidTarget() {
        PlayerState ps = playerState(PLAYER_1, List.of(), cardIds(5));
        ps.setActivePokemon(activeWithId("inst-1", "xy1-3", 3, List.of("xy1-132")));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1,
                "cardId", "xy1-128",
                "targetInstanceId", "inst-1",
                "energyToDiscardId", "xy1-132");

        assertThat(effect.canApply(state, act).isValid()).isTrue();
    }

    @Test
    void canApply_shouldSucceed_whenBenchPokemonIsValidTarget() {
        PlayerState ps = playerState(PLAYER_1, List.of(), cardIds(5));
        ps.setActivePokemon(activeWithId("inst-1", "xy1-3", 0, List.of()));
        ps.setBench(new ArrayList<>(List.of(benchWithId("inst-2", "xy1-12", 4, List.of("xy1-131")))));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1,
                "cardId", "xy1-128",
                "targetInstanceId", "inst-2",
                "energyToDiscardId", "xy1-131");

        assertThat(effect.canApply(state, act).isValid()).isTrue();
    }

    @Test
    void canApply_shouldFail_whenBenchTargetHasNoDamage() {
        PlayerState ps = playerState(PLAYER_1, List.of(), cardIds(5));
        ps.setActivePokemon(activeWithId("inst-1", "xy1-3", 0, List.of()));
        ps.setBench(new ArrayList<>(List.of(benchWithId("inst-2", "xy1-12", 0, List.of("xy1-131")))));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1,
                "cardId", "xy1-128",
                "targetInstanceId", "inst-2",
                "energyToDiscardId", "xy1-131");

        ValidationResult result = effect.canApply(state, act);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("That Pokémon has no damage to heal.");
    }

    @Test
    void canApply_shouldFail_whenEnergyNotAttachedToBenchTarget() {
        PlayerState ps = playerState(PLAYER_1, List.of(), cardIds(5));
        ps.setActivePokemon(activeWithId("inst-1", "xy1-3", 0, List.of()));
        ps.setBench(new ArrayList<>(List.of(benchWithId("inst-2", "xy1-12", 4, List.of("xy1-131")))));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1,
                "cardId", "xy1-128",
                "targetInstanceId", "inst-2",
                "energyToDiscardId", "xy1-999");

        ValidationResult result = effect.canApply(state, act);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage())
                .isEqualTo("The specified energy is not attached to that Pokémon.");
    }

    // ── apply ──────────────────────────────────────────────────────────────────

    @Test
    void apply_shouldHealActivePokemon_andDiscardEnergy() {
        PlayerState ps = playerState(PLAYER_1, List.of(), cardIds(5));
        ps.setActivePokemon(activeWithId("inst-1", "xy1-3", 8, List.of("xy1-132")));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1,
                "cardId", "xy1-128",
                "targetInstanceId", "inst-1",
                "energyToDiscardId", "xy1-132");

        effect.apply(state, act);

        assertThat(ps.getActivePokemon().getDamageCounters()).isEqualTo(2); // 8 - 6
        assertThat(ps.getActivePokemon().getAttachedEnergyIds()).isEmpty();
        assertThat(ps.getDiscard()).contains("xy1-132");
    }

    @Test
    void apply_shouldClampHealAtZero_whenDamageLessThanHealAmount() {
        PlayerState ps = playerState(PLAYER_1, List.of(), cardIds(5));
        ps.setActivePokemon(activeWithId("inst-1", "xy1-3", 3, List.of("xy1-132")));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1,
                "cardId", "xy1-128",
                "targetInstanceId", "inst-1",
                "energyToDiscardId", "xy1-132");

        effect.apply(state, act);

        assertThat(ps.getActivePokemon().getDamageCounters()).isEqualTo(0);
    }

    @Test
    void apply_shouldHealBenchPokemon_andDiscardEnergy() {
        PlayerState ps = playerState(PLAYER_1, List.of(), cardIds(5));
        ps.setActivePokemon(activeWithId("inst-1", "xy1-3", 0, List.of()));
        BenchPokemon bench = benchWithId("inst-2", "xy1-12", 5, List.of("xy1-131", "xy1-132"));
        ps.setBench(new ArrayList<>(List.of(bench)));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1,
                "cardId", "xy1-128",
                "targetInstanceId", "inst-2",
                "energyToDiscardId", "xy1-131");

        effect.apply(state, act);

        BenchPokemon healedBench = ps.getBench().get(0);
        assertThat(healedBench.getDamageCounters()).isEqualTo(0); // 5 - 6, clamped
        assertThat(healedBench.getAttachedEnergyIds()).containsExactly("xy1-132");
        assertThat(ps.getDiscard()).contains("xy1-131");
    }

    @Test
    void apply_shouldNotAffectActive_whenHealingBenchTarget() {
        PlayerState ps = playerState(PLAYER_1, List.of(), cardIds(5));
        ps.setActivePokemon(activeWithId("inst-1", "xy1-3", 4, List.of()));
        ps.setBench(new ArrayList<>(List.of(benchWithId("inst-2", "xy1-12", 5, List.of("xy1-131")))));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1,
                "cardId", "xy1-128",
                "targetInstanceId", "inst-2",
                "energyToDiscardId", "xy1-131");

        effect.apply(state, act);

        assertThat(ps.getActivePokemon().getDamageCounters()).isEqualTo(4);
    }

    @Test
    void apply_shouldEmitTrainerPlayedEvent_withCorrectData() {
        PlayerState ps = playerState(PLAYER_1, List.of(), cardIds(5));
        ps.setActivePokemon(activeWithId("inst-1", "xy1-3", 8, List.of("xy1-132")));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1,
                "cardId", "xy1-128",
                "targetInstanceId", "inst-1",
                "energyToDiscardId", "xy1-132");

        EngineResult result = effect.apply(state, act);

        assertThat(result.events()).hasSize(1);
        GameEvent event = result.events().get(0);
        assertThat(event.getType()).isEqualTo(GameEventType.TRAINER_PLAYED);
        assertThat(event.getData()).containsEntry("cardId", "xy1-128");
        assertThat(event.getData()).containsEntry("healedInstanceId", "inst-1");
        assertThat(event.getData()).containsEntry("healAmount", 60);
    }

    // ── Test helpers ───────────────────────────────────────────────────────────

    private ActivePokemon activeWithId(String instanceId, String cardId,
                                       int damageCounters, List<String> attachedEnergies) {
        return ActivePokemon.builder()
                .instanceId(instanceId)
                .cardId(cardId)
                .attachedEnergyIds(new ArrayList<>(attachedEnergies))
                .evolutionStack(new ArrayList<>(List.of(cardId)))
                .damageCounters(damageCounters)
                .conditions(new HashSet<>())
                .enteredThisTurn(false)
                .build();
    }

    private BenchPokemon benchWithId(String instanceId, String cardId,
                                     int damageCounters, List<String> attachedEnergies) {
        return BenchPokemon.builder()
                .instanceId(instanceId)
                .cardId(cardId)
                .attachedEnergyIds(new ArrayList<>(attachedEnergies))
                .evolutionStack(new ArrayList<>(List.of(cardId)))
                .damageCounters(damageCounters)
                .enteredThisTurn(false)
                .build();
    }
}