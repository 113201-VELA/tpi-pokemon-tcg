package com.pokemon.tcg.domain.strategy.trainer.supporter;

import com.pokemon.tcg.domain.model.game.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

class ProfessorSycamoreEffectTest {

    private ProfessorSycamoreEffect effect;

    @BeforeEach
    void setUp() {
        effect = new ProfessorSycamoreEffect();
    }

    // ── canApply ───────────────────────────────────────────────────────────────

    @Test
    void canApply_shouldFail_whenDeckIsEmpty() {
        PlayerState ps = playerState(PLAYER_1, List.of("xy1-1", "xy1-2"), List.of());
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-122");

        ValidationResult result = effect.canApply(state, act);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Your deck is empty.");
    }

    @Test
    void canApply_shouldSucceed_whenDeckHasCards() {
        PlayerState ps = playerState(PLAYER_1, List.of("xy1-1"), cardIds(5));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-122");

        assertThat(effect.canApply(state, act).isValid()).isTrue();
    }

    // ── apply ──────────────────────────────────────────────────────────────────

    @Test
    void apply_shouldDiscardEntireHand() {
        List<String> hand = List.of("xy1-1", "xy1-2", "xy1-3");
        PlayerState ps = playerState(PLAYER_1, hand, cardIds(10));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-122");

        effect.apply(state, act);

        assertThat(ps.getDiscard()).containsExactlyInAnyOrder("xy1-1", "xy1-2", "xy1-3");
    }

    @Test
    void apply_shouldDrawSevenCards_whenDeckHasEnough() {
        List<String> deck = cardIds(10); // 10 distinct cards
        PlayerState ps = playerState(PLAYER_1, List.of("xy1-1"), new ArrayList<>(deck));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-122");

        effect.apply(state, act);

        assertThat(ps.getHand()).hasSize(7);
        assertThat(ps.getDeck()).hasSize(3);
    }

    @Test
    void apply_shouldDrawFewerThanSeven_whenDeckHasLessCards() {
        List<String> deck = cardIds(4);
        PlayerState ps = playerState(PLAYER_1, List.of("xy1-1"), new ArrayList<>(deck));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-122");

        effect.apply(state, act);

        assertThat(ps.getHand()).hasSize(4);
        assertThat(ps.getDeck()).isEmpty();
    }

    @Test
    void apply_shouldPreserveExistingDiscardPile() {
        PlayerState ps = playerState(PLAYER_1, List.of("xy1-1"), cardIds(10));
        ps.setDiscard(new ArrayList<>(List.of("xy1-99")));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-122");

        effect.apply(state, act);

        assertThat(ps.getDiscard()).contains("xy1-99", "xy1-1");
    }

    @Test
    void apply_shouldMarkSupporterPlayedThisTurn() {
        PlayerState ps = playerState(PLAYER_1, List.of("xy1-1"), cardIds(10));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-122");

        effect.apply(state, act);

        assertThat(state.getTurnFlags().isSupporterPlayedThisTurn()).isTrue();
    }

    @Test
    void apply_shouldEmitTrainerPlayedEvent_withCorrectData() {
        PlayerState ps = playerState(PLAYER_1, List.of("xy1-1"), cardIds(10));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-122");

        EngineResult result = effect.apply(state, act);

        assertThat(result.events()).hasSize(1);
        GameEvent event = result.events().get(0);
        assertThat(event.getType()).isEqualTo(GameEventType.TRAINER_PLAYED);
        assertThat(event.getData()).containsEntry("cardId", "xy1-122");
        assertThat(event.getData()).containsEntry("cardsDrawn", 7);
    }

    @Test
    void apply_shouldEmitCorrectCardsDrawn_whenDeckSmallerThanSeven() {
        PlayerState ps = playerState(PLAYER_1, List.of("xy1-1"), new ArrayList<>(cardIds(3)));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-122");

        EngineResult result = effect.apply(state, act);

        GameEvent event = result.events().get(0);
        assertThat(event.getData()).containsEntry("cardsDrawn", 3);
    }
}