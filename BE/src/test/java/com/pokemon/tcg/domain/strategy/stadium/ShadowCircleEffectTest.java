package com.pokemon.tcg.domain.strategy.stadium;

import com.pokemon.tcg.domain.model.game.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

class ShadowCircleEffectTest {

    private ShadowCircleEffect effect;

    @BeforeEach
    void setUp() {
        effect = new ShadowCircleEffect();
    }

    @Test
    void canApply_shouldAlwaysReturnValid() {
        BoardState state = boardState(
                playerState(PLAYER_1, List.of("xy1-126"), cardIds(5)),
                playerState(PLAYER_2, List.of(), cardIds(5))
        );
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-126");

        assertThat(effect.canApply(state, act).isValid()).isTrue();
    }

    @Test
    void apply_shouldSetActiveStadiumCardId() {
        BoardState state = boardState(
                playerState(PLAYER_1, List.of("xy1-126"), cardIds(5)),
                playerState(PLAYER_2, List.of(), cardIds(5))
        );
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-126");

        effect.apply(state, act);

        assertThat(state.getActiveStadiumCardId()).isEqualTo("shadow circle");
    }
}
