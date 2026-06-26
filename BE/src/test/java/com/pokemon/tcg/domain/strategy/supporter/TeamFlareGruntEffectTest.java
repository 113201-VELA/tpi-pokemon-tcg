package com.pokemon.tcg.domain.strategy.supporter;

import com.pokemon.tcg.domain.model.game.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

class TeamFlareGruntEffectTest {

    private TeamFlareGruntEffect effect;

    @BeforeEach
    void setUp() {
        effect = new TeamFlareGruntEffect();
    }

    // ── canApply ───────────────────────────────────────────────────────────────

    @Test
    void canApply_shouldFail_whenOpponentHasNoActivePokemon() {
        PlayerState ps       = playerState(PLAYER_1, List.of(), cardIds(5));
        PlayerState opponent = playerState(PLAYER_2, List.of(), cardIds(5));
        // opponent has no active Pokémon
        BoardState state = boardState(ps, opponent);
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-129");

        ValidationResult result = effect.canApply(state, act);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Opponent has no Active Pokémon.");
    }

    @Test
    void canApply_shouldSucceed_whenOpponentHasActivePokemon() {
        PlayerState ps       = playerState(PLAYER_1, List.of(), cardIds(5));
        PlayerState opponent = playerState(PLAYER_2, List.of(), cardIds(5));
        opponent.setActivePokemon(activeWithId("inst-opp", "xy1-78"));
        BoardState state = boardState(ps, opponent);
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-129");

        assertThat(effect.canApply(state, act).isValid()).isTrue();
    }

    @Test
    void canApply_shouldSucceed_whenOpponentActiveHasNoEnergy() {
        // Playing the card is allowed even if there is no Energy to discard
        PlayerState ps       = playerState(PLAYER_1, List.of(), cardIds(5));
        PlayerState opponent = playerState(PLAYER_2, List.of(), cardIds(5));
        ActivePokemon active = activeWithId("inst-opp", "xy1-78");
        active.setAttachedEnergyIds(new ArrayList<>());
        opponent.setActivePokemon(active);
        BoardState state = boardState(ps, opponent);
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-129");

        assertThat(effect.canApply(state, act).isValid()).isTrue();
    }

    // ── apply ──────────────────────────────────────────────────────────────────

    @Test
    void apply_shouldDiscardOneEnergyFromOpponentActive() {
        PlayerState ps       = playerState(PLAYER_1, List.of(), cardIds(5));
        PlayerState opponent = playerState(PLAYER_2, List.of(), cardIds(5));
        ActivePokemon active = activeWithId("inst-opp", "xy1-78");
        active.setAttachedEnergyIds(new ArrayList<>(List.of("xy1-132", "xy1-133")));
        opponent.setActivePokemon(active);
        BoardState state = boardState(ps, opponent);
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-129");

        effect.apply(state, act);

        assertThat(active.getAttachedEnergyIds()).hasSize(1);
        assertThat(opponent.getDiscard()).containsExactly("xy1-132");
    }

    @Test
    void apply_shouldHaveNoEffect_whenOpponentActiveHasNoEnergy() {
        PlayerState ps       = playerState(PLAYER_1, List.of(), cardIds(5));
        PlayerState opponent = playerState(PLAYER_2, List.of(), cardIds(5));
        ActivePokemon active = activeWithId("inst-opp", "xy1-78");
        active.setAttachedEnergyIds(new ArrayList<>());
        opponent.setActivePokemon(active);
        BoardState state = boardState(ps, opponent);
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-129");

        effect.apply(state, act);

        assertThat(active.getAttachedEnergyIds()).isEmpty();
        assertThat(opponent.getDiscard()).isEmpty();
    }

    @Test
    void apply_shouldMarkSupporterPlayedThisTurn() {
        PlayerState ps       = playerState(PLAYER_1, List.of(), cardIds(5));
        PlayerState opponent = playerState(PLAYER_2, List.of(), cardIds(5));
        opponent.setActivePokemon(activeWithId("inst-opp", "xy1-78"));
        BoardState state = boardState(ps, opponent);
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-129");

        effect.apply(state, act);

        assertThat(state.getTurnFlags().isSupporterPlayedThisTurn()).isTrue();
    }

    @Test
    void apply_shouldEmitTrainerPlayedEvent_withDiscardedEnergyId() {
        PlayerState ps       = playerState(PLAYER_1, List.of(), cardIds(5));
        PlayerState opponent = playerState(PLAYER_2, List.of(), cardIds(5));
        ActivePokemon active = activeWithId("inst-opp", "xy1-78");
        active.setAttachedEnergyIds(new ArrayList<>(List.of("xy1-132")));
        opponent.setActivePokemon(active);
        BoardState state = boardState(ps, opponent);
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-129");

        EngineResult result = effect.apply(state, act);

        assertThat(result.events()).hasSize(1);
        GameEvent event = result.events().get(0);
        assertThat(event.getType()).isEqualTo(GameEventType.TRAINER_PLAYED);
        assertThat(event.getData()).containsEntry("cardId", "xy1-129");
        assertThat(event.getData()).containsEntry("discardedEnergyId", "xy1-132");
    }

    @Test
    void apply_shouldEmitTrainerPlayedEvent_withoutDiscardedEnergyId_whenNoneAttached() {
        PlayerState ps       = playerState(PLAYER_1, List.of(), cardIds(5));
        PlayerState opponent = playerState(PLAYER_2, List.of(), cardIds(5));
        opponent.setActivePokemon(activeWithId("inst-opp", "xy1-78"));
        BoardState state = boardState(ps, opponent);
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-129");

        EngineResult result = effect.apply(state, act);

        assertThat(result.events()).hasSize(1);
        assertThat(result.events().get(0).getData()).doesNotContainKey("discardedEnergyId");
    }

    // ── Test helpers ───────────────────────────────────────────────────────────

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
}
