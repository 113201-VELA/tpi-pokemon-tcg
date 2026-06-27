package com.pokemon.tcg.domain.strategy.trainer.supporter;

import com.pokemon.tcg.domain.model.game.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

class CassiusEffectTest {

    private CassiusEffect effect;

    @BeforeEach
    void setUp() {
        effect = new CassiusEffect();
    }

    // ── canApply ───────────────────────────────────────────────────────────────

    @Test
    void canApply_shouldFail_whenNoTargetSpecified() {
        PlayerState ps = playerState(PLAYER_1, List.of(), cardIds(5));
        ps.setActivePokemon(activeWithId("inst-1", "xy1-3"));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-115");

        ValidationResult result = effect.canApply(state, act);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("No target Pokémon specified for Cassius.");
    }

    @Test
    void canApply_shouldFail_whenTargetNotInPlay() {
        PlayerState ps = playerState(PLAYER_1, List.of(), cardIds(5));
        ps.setActivePokemon(activeWithId("inst-1", "xy1-3"));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1,
                "cardId", "xy1-115",
                "targetInstanceId", "nonexistent-id");

        ValidationResult result = effect.canApply(state, act);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Target Pokémon is not in play.");
    }

    @Test
    void canApply_shouldSucceed_whenActiveIsTargeted() {
        PlayerState ps = playerState(PLAYER_1, List.of(), cardIds(5));
        ps.setActivePokemon(activeWithId("inst-1", "xy1-3"));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1,
                "cardId", "xy1-115",
                "targetInstanceId", "inst-1");

        assertThat(effect.canApply(state, act).isValid()).isTrue();
    }

    @Test
    void canApply_shouldSucceed_whenBenchPokemonIsTargeted() {
        PlayerState ps = playerState(PLAYER_1, List.of(), cardIds(5));
        ps.setActivePokemon(activeWithId("inst-1", "xy1-3"));
        ps.setBench(List.of(benchWithId("inst-2", "xy1-12")));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1,
                "cardId", "xy1-115",
                "targetInstanceId", "inst-2");

        assertThat(effect.canApply(state, act).isValid()).isTrue();
    }

    // ── apply ──────────────────────────────────────────────────────────────────

    @Test
    void apply_shouldReturnActivePokemonAndAttachedCardsToDeck() {
        PlayerState ps = playerState(PLAYER_1, List.of(), cardIds(5));
        ActivePokemon active = activeWithId("inst-1", "xy1-3");
        active.setAttachedEnergyIds(new ArrayList<>(List.of("xy1-132", "xy1-132")));
        active.setAttachedToolId("xy1-121");
        ps.setActivePokemon(active);
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1,
                "cardId", "xy1-115",
                "targetInstanceId", "inst-1");

        effect.apply(state, act);

        // Active slot cleared
        assertThat(ps.getActivePokemon()).isNull();
        // Deck contains Pokémon card + 2 energies + 1 tool = 4 extra cards
        assertThat(ps.getDeck()).hasSize(5 + 4);
        assertThat(ps.getDeck()).contains("xy1-3", "xy1-132", "xy1-121");
    }

    @Test
    void apply_shouldReturnBenchPokemonAndAttachedCardsToDeck() {
        PlayerState ps = playerState(PLAYER_1, List.of(), cardIds(5));
        ps.setActivePokemon(activeWithId("inst-1", "xy1-3"));
        BenchPokemon benched = benchWithId("inst-2", "xy1-12");
        benched.setAttachedEnergyIds(new ArrayList<>(List.of("xy1-132")));
        ps.setBench(new ArrayList<>(List.of(benched)));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1,
                "cardId", "xy1-115",
                "targetInstanceId", "inst-2");

        effect.apply(state, act);

        // Bench Pokémon removed
        assertThat(ps.getBench()).isEmpty();
        // Active untouched
        assertThat(ps.getActivePokemon()).isNotNull();
        // Deck contains Pokémon card + 1 energy = 2 extra cards
        assertThat(ps.getDeck()).hasSize(5 + 2);
        assertThat(ps.getDeck()).contains("xy1-12", "xy1-132");
    }

    @Test
    void apply_shouldMarkSupporterPlayedThisTurn() {
        PlayerState ps = playerState(PLAYER_1, List.of(), cardIds(5));
        ps.setActivePokemon(activeWithId("inst-1", "xy1-3"));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1,
                "cardId", "xy1-115",
                "targetInstanceId", "inst-1");

        effect.apply(state, act);

        assertThat(state.getTurnFlags().isSupporterPlayedThisTurn()).isTrue();
    }

    @Test
    void apply_shouldEmitTrainerPlayedEvent_withCorrectData() {
        PlayerState ps = playerState(PLAYER_1, List.of(), cardIds(5));
        ps.setActivePokemon(activeWithId("inst-1", "xy1-3"));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1,
                "cardId", "xy1-115",
                "targetInstanceId", "inst-1");

        EngineResult result = effect.apply(state, act);

        assertThat(result.events()).hasSize(1);
        GameEvent event = result.events().get(0);
        assertThat(event.getType()).isEqualTo(GameEventType.TRAINER_PLAYED);
        assertThat(event.getData()).containsEntry("cardId", "xy1-115");
        assertThat(event.getData()).containsEntry("returnedInstanceId", "inst-1");
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
