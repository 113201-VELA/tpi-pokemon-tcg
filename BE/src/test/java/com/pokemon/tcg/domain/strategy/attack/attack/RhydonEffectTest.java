package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.engine.CoinFlipService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RhydonEffectTest {

    private CoinFlipService coinFlipService;
    private RhydonEffect effect;

    @BeforeEach
    void setUp() {
        coinFlipService = mock(CoinFlipService.class);
        effect = new RhydonEffect(coinFlipService);
    }

    @Test
    void shouldSupportBothAttacks() {
        assertThat(effect.getSupportedAttacks())
                .contains("rhydon|horn drill", "rhydon|mad mountain");
    }

    // ─── Horn Drill ───────────────────────────────────────────────────────────

    @Test
    void hornDrill_shouldNotAddModifiers() {
        AttackContext ctx = buildContext("horn drill", 3, 5);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
        verifyNoInteractions(coinFlipService);
    }

    // ─── Mad Mountain — both heads ────────────────────────────────────────────

    @Test
    void madMountain_shouldDiscardOneCardPerCounter_onDoubleHeads() {
        when(coinFlipService.flip()).thenReturn(CoinResult.HEADS, CoinResult.HEADS);
        AttackContext ctx = buildContext("mad mountain", 3, 5);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getOpponentState(PLAYER_1).getDeck()).hasSize(2);
        assertThat(ctx.getBoardState().getOpponentState(PLAYER_1).getDiscard()).hasSize(3);
    }

    @Test
    void madMountain_shouldNotDiscardMoreThanDeckSize_onDoubleHeads() {
        when(coinFlipService.flip()).thenReturn(CoinResult.HEADS, CoinResult.HEADS);
        // Rhydon has 5 counters but opponent only has 3 cards in deck
        AttackContext ctx = buildContext("mad mountain", 5, 3);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getOpponentState(PLAYER_1).getDeck()).isEmpty();
        assertThat(ctx.getBoardState().getOpponentState(PLAYER_1).getDiscard()).hasSize(3);
    }

    @Test
    void madMountain_shouldDoNothing_whenRhydonHasNoCounters() {
        when(coinFlipService.flip()).thenReturn(CoinResult.HEADS, CoinResult.HEADS);
        AttackContext ctx = buildContext("mad mountain", 0, 5);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getOpponentState(PLAYER_1).getDeck()).hasSize(5);
    }

    // ─── Mad Mountain — not both heads ───────────────────────────────────────

    @Test
    void madMountain_shouldDoNothing_onHeadsThenTails() {
        when(coinFlipService.flip()).thenReturn(CoinResult.HEADS, CoinResult.TAILS);
        AttackContext ctx = buildContext("mad mountain", 3, 5);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getOpponentState(PLAYER_1).getDeck()).hasSize(5);
    }

    @Test
    void madMountain_shouldDoNothing_onTailsThenHeads() {
        when(coinFlipService.flip()).thenReturn(CoinResult.TAILS, CoinResult.HEADS);
        AttackContext ctx = buildContext("mad mountain", 3, 5);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getOpponentState(PLAYER_1).getDeck()).hasSize(5);
    }

    @Test
    void madMountain_shouldDoNothing_onDoubleTails() {
        when(coinFlipService.flip()).thenReturn(CoinResult.TAILS, CoinResult.TAILS);
        AttackContext ctx = buildContext("mad mountain", 3, 5);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getOpponentState(PLAYER_1).getDeck()).hasSize(5);
    }

    // ─── unknown attack ───────────────────────────────────────────────────────

    @Test
    void unknownAttack_shouldDoNothing() {
        AttackContext ctx = buildContext("unknown", 3, 5);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
        verifyNoInteractions(coinFlipService);
    }

    // ─── helper ───────────────────────────────────────────────────────────────

    private AttackContext buildContext(String attackName,
                                       int rhydonCounters,
                                       int opponentDeckSize) {
        ActivePokemon rhydon = ActivePokemon.builder()
                .instanceId("rhydon-1")
                .cardId("xy1-61")
                .types(new ArrayList<>(List.of(EnergyType.FIGHTING)))
                .attachedEnergyIds(new ArrayList<>(
                        List.of("xy1-96", "xy1-95", "xy1-95")))
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(rhydonCounters)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();

        ActivePokemon defender = ActivePokemon.builder()
                .instanceId("def-1")
                .cardId("xy1-1")
                .types(new ArrayList<>(List.of(EnergyType.GRASS)))
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();

        PlayerState attackerState = playerState(PLAYER_1, List.of(), cardIds(5));
        attackerState.setActivePokemon(rhydon);

        PlayerState defenderState = playerState(PLAYER_2, List.of(),
                cardIds(opponentDeckSize));
        defenderState.setActivePokemon(defender);
        defenderState.setDiscard(new ArrayList<>());

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
}