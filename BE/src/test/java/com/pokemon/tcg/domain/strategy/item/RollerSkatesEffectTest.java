package com.pokemon.tcg.domain.strategy.item;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.engine.CoinFlipService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RollerSkatesEffectTest {

    @Mock
    private CoinFlipService coinFlipService;

    private RollerSkatesEffect effect;

    @BeforeEach
    void setUp() {
        effect = new RollerSkatesEffect(coinFlipService);
    }

    @Test
    void canApply_shouldAlwaysSucceed() {
        PlayerState ps = playerState(PLAYER_1, List.of(), List.of());
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-125");

        assertThat(effect.canApply(state, act).isValid()).isTrue();
    }

    @Test
    void apply_shouldDraw3Cards_whenFlipIsHeads() {
        when(coinFlipService.flip()).thenReturn(CoinResult.HEADS);
        PlayerState ps = playerState(PLAYER_1, List.of(), cardIds(10));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-125");

        effect.apply(state, act);

        assertThat(ps.getHand()).hasSize(3);
        assertThat(ps.getDeck()).hasSize(7);
    }

    @Test
    void apply_shouldNotDraw_whenFlipIsTails() {
        when(coinFlipService.flip()).thenReturn(CoinResult.TAILS);
        PlayerState ps = playerState(PLAYER_1, List.of(), cardIds(10));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-125");

        effect.apply(state, act);

        assertThat(ps.getHand()).isEmpty();
        assertThat(ps.getDeck()).hasSize(10);
    }

    @Test
    void apply_shouldDrawRemainingCards_whenDeckHasLessThan3AndHeads() {
        when(coinFlipService.flip()).thenReturn(CoinResult.HEADS);
        PlayerState ps = playerState(PLAYER_1, List.of(), cardIds(2));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-125");

        EngineResult result = effect.apply(state, act);

        assertThat(ps.getHand()).hasSize(2);
        assertThat(ps.getDeck()).isEmpty();
        assertThat(result.events().get(0).getData()).containsEntry("cardsDrawn", 2);
    }

    @Test
    void apply_shouldNotDraw_whenDeckIsEmptyAndHeads() {
        when(coinFlipService.flip()).thenReturn(CoinResult.HEADS);
        PlayerState ps = playerState(PLAYER_1, List.of(), List.of());
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-125");

        EngineResult result = effect.apply(state, act);

        assertThat(ps.getHand()).isEmpty();
        assertThat(result.events().get(0).getData()).containsEntry("cardsDrawn", 0);
    }

    @Test
    void apply_shouldEmitTrainerPlayedEvent_withCoinResultAndCardsDrawn() {
        when(coinFlipService.flip()).thenReturn(CoinResult.HEADS);
        PlayerState ps = playerState(PLAYER_1, List.of(), cardIds(10));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-125");

        EngineResult result = effect.apply(state, act);

        assertThat(result.events()).hasSize(1);
        GameEvent event = result.events().get(0);
        assertThat(event.getType()).isEqualTo(GameEventType.TRAINER_PLAYED);
        assertThat(event.getData()).containsEntry("cardId", "xy1-125");
        assertThat(event.getData()).containsEntry("coinResult", "HEADS");
        assertThat(event.getData()).containsEntry("cardsDrawn", 3);
    }

    @Test
    void apply_shouldEmitTrainerPlayedEvent_withTailsAndZeroDrawn() {
        when(coinFlipService.flip()).thenReturn(CoinResult.TAILS);
        PlayerState ps = playerState(PLAYER_1, List.of(), cardIds(10));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-125");

        EngineResult result = effect.apply(state, act);

        assertThat(result.events().get(0).getData()).containsEntry("coinResult", "TAILS");
        assertThat(result.events().get(0).getData()).containsEntry("cardsDrawn", 0);
    }
}
