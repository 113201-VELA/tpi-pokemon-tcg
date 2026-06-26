package com.pokemon.tcg.domain.strategy.item;

import com.pokemon.tcg.domain.model.game.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

class HardCharmEffectTest {

    private HardCharmEffect effect;

    @BeforeEach
    void setUp() {
        effect = new HardCharmEffect();
    }

    @Test
    void canApply_shouldFail_whenNoTargetSpecified() {
        PlayerState ps = playerState(PLAYER_1, List.of(), cardIds(5));
        ps.setActivePokemon(activeWithId("inst-1", "xy1-3"));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-119");

        ValidationResult result = effect.canApply(state, act);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage())
                .isEqualTo("No target Pokémon specified for Hard Charm.");
    }

    @Test
    void canApply_shouldFail_whenTargetNotInPlay() {
        PlayerState ps = playerState(PLAYER_1, List.of(), cardIds(5));
        ps.setActivePokemon(activeWithId("inst-1", "xy1-3"));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1,
                "cardId", "xy1-119", "targetInstanceId", "nonexistent");

        assertThat(effect.canApply(state, act).isValid()).isFalse();
        assertThat(effect.canApply(state, act).getErrorMessage())
                .isEqualTo("Target Pokémon is not in play.");
    }

    @Test
    void canApply_shouldFail_whenTargetAlreadyHasTool() {
        PlayerState ps = playerState(PLAYER_1, List.of(), cardIds(5));
        ActivePokemon active = activeWithId("inst-1", "xy1-3");
        active.setAttachedToolId("xy1-121");
        ps.setActivePokemon(active);
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1,
                "cardId", "xy1-119", "targetInstanceId", "inst-1");

        assertThat(effect.canApply(state, act).isValid()).isFalse();
        assertThat(effect.canApply(state, act).getErrorMessage())
                .isEqualTo("That Pokémon already has a Pokémon Tool attached.");
    }

    @Test
    void canApply_shouldSucceed_whenActiveHasNoTool() {
        PlayerState ps = playerState(PLAYER_1, List.of(), cardIds(5));
        ps.setActivePokemon(activeWithId("inst-1", "xy1-3"));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1,
                "cardId", "xy1-119", "targetInstanceId", "inst-1");

        assertThat(effect.canApply(state, act).isValid()).isTrue();
    }

    @Test
    void canApply_shouldSucceed_whenBenchPokemonHasNoTool() {
        PlayerState ps = playerState(PLAYER_1, List.of(), cardIds(5));
        ps.setActivePokemon(activeWithId("inst-1", "xy1-3"));
        ps.setBench(new ArrayList<>(List.of(benchWithId("inst-2", "xy1-12"))));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1,
                "cardId", "xy1-119", "targetInstanceId", "inst-2");

        assertThat(effect.canApply(state, act).isValid()).isTrue();
    }

    @Test
    void apply_shouldAttachToolToActivePokemon() {
        PlayerState ps = playerState(PLAYER_1, List.of(), cardIds(5));
        ps.setActivePokemon(activeWithId("inst-1", "xy1-3"));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1,
                "cardId", "xy1-119", "targetInstanceId", "inst-1");

        effect.apply(state, act);

        assertThat(ps.getActivePokemon().getAttachedToolId()).isEqualTo("xy1-119");
    }

    @Test
    void apply_shouldAttachToolToBenchPokemon() {
        PlayerState ps = playerState(PLAYER_1, List.of(), cardIds(5));
        ps.setActivePokemon(activeWithId("inst-1", "xy1-3"));
        BenchPokemon benched = benchWithId("inst-2", "xy1-12");
        ps.setBench(new ArrayList<>(List.of(benched)));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1,
                "cardId", "xy1-119", "targetInstanceId", "inst-2");

        effect.apply(state, act);

        assertThat(benched.getAttachedToolId()).isEqualTo("xy1-119");
    }

    @Test
    void apply_shouldEmitTrainerPlayedEvent() {
        PlayerState ps = playerState(PLAYER_1, List.of(), cardIds(5));
        ps.setActivePokemon(activeWithId("inst-1", "xy1-3"));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1,
                "cardId", "xy1-119", "targetInstanceId", "inst-1");

        EngineResult result = effect.apply(state, act);

        assertThat(result.events()).hasSize(1);
        GameEvent event = result.events().get(0);
        assertThat(event.getType()).isEqualTo(GameEventType.TRAINER_PLAYED);
        assertThat(event.getData()).containsEntry("cardId", "xy1-119");
        assertThat(event.getData()).containsEntry("targetInstanceId", "inst-1");
    }

    private ActivePokemon activeWithId(String instanceId, String cardId) {
        return ActivePokemon.builder()
                .instanceId(instanceId)
                .cardId(cardId)
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>(List.of(cardId)))
                .damageCounters(0)
                .conditions(new HashSet<>())
                .enteredThisTurn(false)
                .build();
    }

    private BenchPokemon benchWithId(String instanceId, String cardId) {
        return BenchPokemon.builder()
                .instanceId(instanceId)
                .cardId(cardId)
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>(List.of(cardId)))
                .damageCounters(0)
                .enteredThisTurn(false)
                .build();
    }
}
