package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.engine.CoinFlipService;
import com.pokemon.tcg.engine.StatusEffectManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class Malamar76EffectTest {

    private CoinFlipService coinFlipService;
    private StatusEffectManager statusEffectManager;
    private Malamar76Effect effect;

    @BeforeEach
    void setUp() {
        coinFlipService = mock(CoinFlipService.class);
        statusEffectManager = mock(StatusEffectManager.class);
        effect = new Malamar76Effect(coinFlipService, statusEffectManager);
        doAnswer(invocation -> {
            ActivePokemon pokemon = invocation.getArgument(0);
            SpecialCondition condition = invocation.getArgument(1);
            Set<SpecialCondition> conditions = new HashSet<>(
                    pokemon.getConditions() != null ? pokemon.getConditions() : new HashSet<>());
            if (condition == SpecialCondition.ASLEEP
                    || condition == SpecialCondition.CONFUSED
                    || condition == SpecialCondition.PARALYZED) {
                conditions.remove(SpecialCondition.ASLEEP);
                conditions.remove(SpecialCondition.CONFUSED);
                conditions.remove(SpecialCondition.PARALYZED);
            }
            conditions.add(condition);
            pokemon.setConditions(conditions);
            return null;
        }).when(statusEffectManager).applyCondition(any(), any());
    }

    private ActivePokemon buildPokemon(String instanceId) {
        return ActivePokemon.builder()
                .instanceId(instanceId)
                .cardId("xy1-76")
                .types(new ArrayList<>(List.of(EnergyType.DARKNESS)))
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>(List.of("xy1-74", "xy1-76")))
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();
    }

    private AttackContext buildContext(String attackName, List<String> defenderHand) {
        ActivePokemon attacker = buildPokemon("attacker-instance");
        ActivePokemon defender = buildPokemon("defender-instance");

        PlayerState attackerState = playerState(PLAYER_1, List.of(), cardIds(5));
        attackerState.setActivePokemon(attacker);

        PlayerState defenderState = playerState(PLAYER_2, defenderHand, cardIds(5));
        defenderState.setActivePokemon(defender);

        BoardState state = boardState(attackerState, defenderState);

        GameAction act = GameAction.builder()
                .type(GameActionType.DECLARE_ATTACK)
                .playerId(PLAYER_1)
                .payload(Map.of("attackName", attackName))
                .build();

        return AttackContext.builder()
                .boardState(state)
                .action(act)
                .attackName(attackName)
                .modifiers(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
    }

    @Test
    void getSupportedAttacksShouldReturnBothAttacks() {
        assertThat(effect.getSupportedAttacks())
                .containsExactlyInAnyOrder("malamar|mental trash", "malamar|distortion beam");
    }

    @Test
    void applyShouldDoNothingForUnsupportedAttack() {
        AttackContext ctx = buildContext("unknown attack", List.of());

        effect.apply(ctx);

        verifyNoInteractions(coinFlipService);
    }

    @Test
    void applyShouldHandleNullAttackName() {
        AttackContext ctx = AttackContext.builder()
                .boardState(boardState(
                        playerState(PLAYER_1, List.of(), cardIds(5)),
                        playerState(PLAYER_2, List.of(), cardIds(5))))
                .action(GameAction.builder()
                        .type(GameActionType.DECLARE_ATTACK)
                        .playerId(PLAYER_1)
                        .payload(Map.of())
                        .build())
                .attackName(null)
                .modifiers(new ArrayList<>())
                .events(new ArrayList<>())
                .build();

        effect.apply(ctx);

        verifyNoInteractions(coinFlipService);
    }

    // ---------- Mental Trash ----------

    @Test
    void mentalTrashShouldSetPendingHandDiscardForTailsCount() {
        List<String> hand = new ArrayList<>(List.of("xy1-1", "xy1-2", "xy1-3", "xy1-4"));
        AttackContext ctx = buildContext("mental trash", hand);

        // 4 flips: 2 tails, 2 heads
        when(coinFlipService.flipAndEmit(any(AttackContext.class), eq(PLAYER_1)))
                .thenReturn(CoinResult.TAILS, CoinResult.HEADS, CoinResult.TAILS, CoinResult.HEADS);

        effect.apply(ctx);

        BoardState resultState = ctx.getBoardState();
        PlayerState defenderState = resultState.getOpponentState(PLAYER_1);

        verify(coinFlipService, times(4)).flipAndEmit(any(AttackContext.class), eq(PLAYER_1));
        assertThat(resultState.isPendingHandDiscard()).isTrue();
        assertThat(resultState.getPendingHandDiscardPlayerId()).isEqualTo(PLAYER_2);
        assertThat(resultState.getPendingHandDiscardCount()).isEqualTo(2);
        // Nothing is discarded yet — the defender still has to choose
        assertThat(defenderState.getHand()).hasSize(4);
        assertThat(defenderState.getDiscard()).isEmpty();
    }

    @Test
    void mentalTrashShouldNotSetPendingDiscardWhenAllHeads() {
        List<String> hand = new ArrayList<>(List.of("xy1-1", "xy1-2"));
        AttackContext ctx = buildContext("mental trash", hand);

        when(coinFlipService.flipAndEmit(any(AttackContext.class), eq(PLAYER_1)))
                .thenReturn(CoinResult.HEADS);

        effect.apply(ctx);

        BoardState resultState = ctx.getBoardState();

        verify(coinFlipService, times(4)).flipAndEmit(any(AttackContext.class), eq(PLAYER_1));
        assertThat(resultState.isPendingHandDiscard()).isFalse();
    }

    @Test
    void mentalTrashShouldCapPendingCountAtHandSize() {
        List<String> hand = new ArrayList<>(List.of("xy1-1"));
        AttackContext ctx = buildContext("mental trash", hand);

        // 4 tails, but hand only has 1 card
        when(coinFlipService.flipAndEmit(any(AttackContext.class), eq(PLAYER_1)))
                .thenReturn(CoinResult.TAILS);

        effect.apply(ctx);

        BoardState resultState = ctx.getBoardState();

        assertThat(resultState.isPendingHandDiscard()).isTrue();
        assertThat(resultState.getPendingHandDiscardCount()).isEqualTo(1);
    }

    @Test
    void mentalTrashShouldNotSetPendingDiscardWhenHandIsEmpty() {
        AttackContext ctx = buildContext("mental trash", List.of());

        when(coinFlipService.flipAndEmit(any(AttackContext.class), eq(PLAYER_1)))
                .thenReturn(CoinResult.TAILS);

        effect.apply(ctx);

        BoardState resultState = ctx.getBoardState();

        assertThat(resultState.isPendingHandDiscard()).isFalse();
    }

    // ---------- Distortion Beam ----------

    @Test
    void distortionBeamShouldApplyAsleepOnHeads() {
        AttackContext ctx = buildContext("distortion beam", List.of());

        when(coinFlipService.flipAndEmit(any(AttackContext.class), eq(PLAYER_1)))
                .thenReturn(CoinResult.HEADS);

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState().getOpponentState(PLAYER_1).getActivePokemon();
        assertThat(defender.getConditions()).containsExactly(SpecialCondition.ASLEEP);
    }

    @Test
    void distortionBeamShouldApplyConfusedOnTails() {
        AttackContext ctx = buildContext("distortion beam", List.of());

        when(coinFlipService.flipAndEmit(any(AttackContext.class), eq(PLAYER_1)))
                .thenReturn(CoinResult.TAILS);

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState().getOpponentState(PLAYER_1).getActivePokemon();
        assertThat(defender.getConditions()).containsExactly(SpecialCondition.CONFUSED);
    }

    @Test
    void distortionBeamShouldReplacePreviousCondition() {
        AttackContext ctx = buildContext("distortion beam", List.of());
        ActivePokemon defender = ctx.getBoardState().getOpponentState(PLAYER_1).getActivePokemon();
        defender.getConditions().add(SpecialCondition.PARALYZED);

        when(coinFlipService.flipAndEmit(any(AttackContext.class), eq(PLAYER_1)))
                .thenReturn(CoinResult.HEADS);

        effect.apply(ctx);

        assertThat(defender.getConditions())
                .containsExactly(SpecialCondition.ASLEEP)
                .doesNotContain(SpecialCondition.PARALYZED);
    }

    @Test
    void distortionBeamShouldDoNothingWhenNoDefender() {
        AttackContext ctx = buildContext("distortion beam", List.of());
        ctx.getBoardState().getOpponentState(PLAYER_1).setActivePokemon(null);

        effect.apply(ctx);

        verifyNoInteractions(coinFlipService);
    }
}