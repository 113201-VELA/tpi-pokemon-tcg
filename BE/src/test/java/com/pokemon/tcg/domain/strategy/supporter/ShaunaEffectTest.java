package com.pokemon.tcg.domain.strategy.supporter;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.fixtures.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

class ShaunaEffectTest {

    private ShaunaEffect effect;

    @BeforeEach
    void setUp() {
        effect = new ShaunaEffect();
    }

    // ── canApply ───────────────────────────────────────────────────────────────

    @Test
    void canApply_shouldFail_whenDeckIsEmpty() {
        PlayerState ps = playerState(PLAYER_1, cardIds(3), List.of());
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(10)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-127");

        ValidationResult result = effect.canApply(state, act);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Your deck is empty.");
    }

    @Test
    void canApply_shouldSucceed_whenDeckHasCards() {
        PlayerState ps = playerState(PLAYER_1, List.of(), cardIds(5));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(10)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-127");

        ValidationResult result = effect.canApply(state, act);

        assertThat(result.isValid()).isTrue();
    }

    // ── apply ──────────────────────────────────────────────────────────────────

    @Test
    void apply_shouldShuffleHandIntoDeck_thenDraw5() {
        PlayerState ps = playerState(PLAYER_1, cardIds(3), cardIds(10));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(10)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-127");

        effect.apply(state, act);

        assertThat(ps.getHand()).hasSize(5);
        assertThat(ps.getDeck()).hasSize(8);
    }

    @Test
    void apply_shouldDraw5_whenHandIsEmpty() {
        PlayerState ps = playerState(PLAYER_1, List.of(), cardIds(10));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(10)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-127");

        effect.apply(state, act);

        assertThat(ps.getHand()).hasSize(5);
        assertThat(ps.getDeck()).hasSize(5);
    }

    @Test
    void apply_shouldDrawAllCards_whenCombinedTotalLessThan5() {
        PlayerState ps = playerState(PLAYER_1, cardIds(1), cardIds(2));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(10)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-127");

        EngineResult result = effect.apply(state, act);

        assertThat(ps.getHand()).hasSize(3);
        assertThat(ps.getDeck()).isEmpty();
        assertThat(result.events().get(0).getData()).containsEntry("cardsDrawn", 3);
    }

    @Test
    void apply_shouldMarkSupporterPlayedThisTurn() {
        PlayerState ps = playerState(PLAYER_1, cardIds(2), cardIds(10));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(10)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-127");

        effect.apply(state, act);

        assertThat(state.getTurnFlags().isSupporterPlayedThisTurn()).isTrue();
    }

    @Test
    void apply_shouldEmitTrainerPlayedEvent_withCorrectData() {
        PlayerState ps = playerState(PLAYER_1, List.of(), cardIds(10));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(10)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-127");

        EngineResult result = effect.apply(state, act);

        assertThat(result.events()).hasSize(1);
        GameEvent event = result.events().get(0);
        assertThat(event.getType()).isEqualTo(GameEventType.TRAINER_PLAYED);
        assertThat(event.getData()).containsEntry("cardId", "xy1-127");
        assertThat(event.getData()).containsEntry("cardsDrawn", 5);
    }
}
