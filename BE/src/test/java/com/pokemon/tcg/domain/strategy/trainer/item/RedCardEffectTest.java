package com.pokemon.tcg.domain.strategy.trainer.item;

import com.pokemon.tcg.domain.model.game.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

class RedCardEffectTest {

    private RedCardEffect effect;

    @BeforeEach
    void setUp() {
        effect = new RedCardEffect();
    }

    @Test
    void canApply_shouldAlwaysSucceed() {
        PlayerState ps       = playerState(PLAYER_1, List.of(), cardIds(5));
        PlayerState opponent = playerState(PLAYER_2, List.of(), List.of());
        BoardState state = boardState(ps, opponent);
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-124");

        assertThat(effect.canApply(state, act).isValid()).isTrue();
    }

    @Test
    void apply_shouldShuffleOpponentHandIntoDeckAndDraw4() {
        PlayerState ps       = playerState(PLAYER_1, List.of(), cardIds(5));
        PlayerState opponent = playerState(PLAYER_2, cardIds(7), cardIds(10));
        BoardState state = boardState(ps, opponent);
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-124");

        effect.apply(state, act);

        assertThat(opponent.getHand()).hasSize(4);
        assertThat(opponent.getDeck()).hasSize(13);
    }

    @Test
    void apply_shouldDrawAllCards_whenCombinedTotalLessThan4() {
        PlayerState ps       = playerState(PLAYER_1, List.of(), cardIds(5));
        PlayerState opponent = playerState(PLAYER_2, cardIds(1), cardIds(2));
        BoardState state = boardState(ps, opponent);
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-124");

        EngineResult result = effect.apply(state, act);

        assertThat(opponent.getHand()).hasSize(3);
        assertThat(opponent.getDeck()).isEmpty();
        assertThat(result.events().get(0).getData()).containsEntry("cardsDrawn", 3);
    }

    @Test
    void apply_shouldDraw4_whenOpponentHandIsEmpty() {
        PlayerState ps       = playerState(PLAYER_1, List.of(), cardIds(5));
        PlayerState opponent = playerState(PLAYER_2, List.of(), cardIds(10));
        BoardState state = boardState(ps, opponent);
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-124");

        effect.apply(state, act);

        assertThat(opponent.getHand()).hasSize(4);
        assertThat(opponent.getDeck()).hasSize(6);
    }

    @Test
    void apply_shouldDrawRemainingCards_whenOpponentDeckIsEmpty() {
        PlayerState ps       = playerState(PLAYER_1, List.of(), cardIds(5));
        PlayerState opponent = playerState(PLAYER_2, cardIds(3), List.of());
        BoardState state = boardState(ps, opponent);
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-124");

        EngineResult result = effect.apply(state, act);

        assertThat(opponent.getHand()).hasSize(3);
        assertThat(opponent.getDeck()).isEmpty();
        assertThat(result.events().get(0).getData()).containsEntry("cardsDrawn", 3);
    }

    @Test
    void apply_shouldNotAffectActingPlayer() {
        PlayerState ps       = playerState(PLAYER_1, cardIds(3), cardIds(5));
        PlayerState opponent = playerState(PLAYER_2, cardIds(7), cardIds(10));
        BoardState state = boardState(ps, opponent);
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-124");

        effect.apply(state, act);

        assertThat(ps.getHand()).hasSize(3);
        assertThat(ps.getDeck()).hasSize(5);
    }

    @Test
    void apply_shouldEmitTrainerPlayedEvent_withCorrectData() {
        PlayerState ps       = playerState(PLAYER_1, List.of(), cardIds(5));
        PlayerState opponent = playerState(PLAYER_2, cardIds(7), cardIds(10));
        BoardState state = boardState(ps, opponent);
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-124");

        EngineResult result = effect.apply(state, act);

        assertThat(result.events()).hasSize(1);
        GameEvent event = result.events().get(0);
        assertThat(event.getType()).isEqualTo(GameEventType.TRAINER_PLAYED);
        assertThat(event.getData()).containsEntry("cardId", "xy1-124");
        assertThat(event.getData()).containsEntry("cardsDrawn", 4);
    }
}
