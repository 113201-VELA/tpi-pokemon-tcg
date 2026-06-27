package com.pokemon.tcg.engine.attack.steps;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.engine.attack.AttackChain;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.fixtures.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SelectionStepTest {

    private SelectionStep step;
    private AttackChain chain;

    @BeforeEach
    void setUp() {
        step  = new SelectionStep();
        chain = mock(AttackChain.class);
    }

    private AttackContext buildCtx() {
        PlayerState p1 = TestDataBuilder.playerState(TestDataBuilder.PLAYER_1,
                new ArrayList<>(), new ArrayList<>());
        PlayerState p2 = TestDataBuilder.playerState(TestDataBuilder.PLAYER_2,
                new ArrayList<>(), new ArrayList<>());
        BoardState board = TestDataBuilder.boardState(p1, p2);
        GameAction action = TestDataBuilder.action(
                GameActionType.DECLARE_ATTACK, TestDataBuilder.PLAYER_1);
        return AttackContext.builder()
                .boardState(board).action(action).events(new ArrayList<>()).build();
    }

    @Test
    void shouldAlwaysCallChain() {
        AttackContext ctx = buildCtx();
        step.execute(ctx, chain);
        verify(chain).next(ctx);
    }

    @Test
    void shouldNotCancelContext() {
        AttackContext ctx = buildCtx();
        step.execute(ctx, chain);
        assertThat(ctx.isCancelled()).isFalse();
    }

    @Test
    void shouldNotModifyBoardState() {
        AttackContext ctx = buildCtx();
        BoardState before = ctx.getBoardState();
        step.execute(ctx, chain);
        assertThat(ctx.getBoardState()).isSameAs(before);
    }
}