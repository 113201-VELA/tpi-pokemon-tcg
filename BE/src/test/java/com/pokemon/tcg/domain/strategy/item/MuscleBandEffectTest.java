package com.pokemon.tcg.domain.strategy.item;

import com.pokemon.tcg.domain.model.game.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

class MuscleBandEffectTest {

    private MuscleBandEffect effect;

    @BeforeEach
    void setUp() {
        effect = new MuscleBandEffect();
    }

    @Test
    void canApply_shouldFail_whenNoTargetSpecified() {
        PlayerState ps = playerState(PLAYER_1, List.of(), cardIds(5));
        ps.setActivePokemon(activeWithId("inst-1", "xy1-3"));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-121");

        assertThat(effect.canApply(state, act).isValid()).isFalse();
        assertThat(effect.canApply(state, act).getErrorMessage())
                .isEqualTo("No target Pokémon specified for Muscle Band.");
    }

    @Test
    void canApply_shouldFail_whenTargetNotInPlay() {
        PlayerState ps = playerState(PLAYER_1, List.of(), cardIds(5));
        ps.setActivePokemon(activeWithId("inst-1", "xy1-3"));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1,
                "cardId", "xy1-121", "targetInstanceId", "nonexistent");

        assertThat(effect.canApply(state, act).isValid()).isFalse();
    }

    @Test
    void canApply_shouldFail_whenTargetAlreadyHasTool() {
        PlayerState ps = playerState(PLAYER_1, List.of(), cardIds(5));
        ActivePokemon active = activeWithId("inst-1", "xy1-3");
        active.setAttachedToolId("xy1-119");
        ps.setActivePokemon(active);
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1,
                "cardId", "xy1-121", "targetInstanceId", "inst-1");

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
                "cardId", "xy1-121", "targetInstanceId", "inst-1");

        assertThat(effect.canApply(state, act).isValid()).isTrue();
    }

    @Test
    void apply_shouldAttachToolToActivePokemon() {
        PlayerState ps = playerState(PLAYER_1, List.of(), cardIds(5));
        ps.setActivePokemon(activeWithId("inst-1", "xy1-3"));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1,
                "cardId", "xy1-121", "targetInstanceId", "inst-1");

        effect.apply(state, act);

        assertThat(ps.getActivePokemon().getAttachedToolId()).isEqualTo("xy1-121");
    }

    @Test
    void apply_shouldAttachToolToBenchPokemon() {
        PlayerState ps = playerState(PLAYER_1, List.of(), cardIds(5));
        ps.setActivePokemon(activeWithId("inst-1", "xy1-3"));
        BenchPokemon benched = benchWithId("inst-2", "xy1-12");
        ps.setBench(new ArrayList<>(List.of(benched)));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1,
                "cardId", "xy1-121", "targetInstanceId", "inst-2");

        effect.apply(state, act);

        assertThat(benched.getAttachedToolId()).isEqualTo("xy1-121");
    }

    @Test
    void apply_shouldEmitTrainerPlayedEvent() {
        PlayerState ps = playerState(PLAYER_1, List.of(), cardIds(5));
        ps.setActivePokemon(activeWithId("inst-1", "xy1-3"));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1,
                "cardId", "xy1-121", "targetInstanceId", "inst-1");

        EngineResult result = effect.apply(state, act);

        assertThat(result.events()).hasSize(1);
        assertThat(result.events().get(0).getType()).isEqualTo(GameEventType.TRAINER_PLAYED);
        assertThat(result.events().get(0).getData()).containsEntry("cardId", "xy1-121");
        assertThat(result.events().get(0).getData()).containsEntry("targetInstanceId", "inst-1");
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
