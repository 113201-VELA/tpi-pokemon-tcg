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

class FroakieEffectTest {

    private CoinFlipService coinFlipService;
    private FroakieEffect effect;

    @BeforeEach
    void setUp() {
        coinFlipService = mock(CoinFlipService.class);
        effect = new FroakieEffect(coinFlipService);
    }

    @Test
    void shouldSupportBounce() {
        assertThat(effect.getSupportedAttacks()).containsExactly("froakie|bounce");
    }

    @Test
    void bounce_onHeads_shouldSwapActiveWithBench() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext(true, "bench-1");

        effect.apply(ctx);

        PlayerState attacker = ctx.getBoardState().getStateFor(PLAYER_1);
        assertThat(attacker.getActivePokemon().getCardId()).isEqualTo("xy1-2");
        assertThat(attacker.getBench()).hasSize(1);
        assertThat(attacker.getBench().get(0).getCardId()).isEqualTo("xy1-39");
    }

    @Test
    void bounce_onHeads_shouldFallBackToFirstBench_whenNoReplacementSpecified() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext(true, null);

        effect.apply(ctx);

        PlayerState attacker = ctx.getBoardState().getStateFor(PLAYER_1);
        assertThat(attacker.getActivePokemon().getCardId()).isEqualTo("xy1-2");
    }

    @Test
    void bounce_onTails_shouldNotSwap() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.TAILS);
        AttackContext ctx = buildContext(true, "bench-1");

        effect.apply(ctx);

        PlayerState attacker = ctx.getBoardState().getStateFor(PLAYER_1);
        assertThat(attacker.getActivePokemon().getCardId()).isEqualTo("xy1-39");
    }

    @Test
    void bounce_onHeads_shouldDoNothing_whenNoBench() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext(false, null);

        effect.apply(ctx);

        PlayerState attacker = ctx.getBoardState().getStateFor(PLAYER_1);
        assertThat(attacker.getActivePokemon().getCardId()).isEqualTo("xy1-39");
    }

    @Test
    void bounce_shouldNotAffectOpponent() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext(true, "bench-1");
        String opponentActiveCardId = ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getCardId();

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getCardId()).isEqualTo(opponentActiveCardId);
    }

    @Test
    void bounce_shouldNotAddModifiers() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext(true, "bench-1");

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    private AttackContext buildContext(boolean hasBench, String replacementInstanceId) {
        ActivePokemon froakie = ActivePokemon.builder()
                .instanceId("froakie-1")
                .cardId("xy1-39")
                .types(new ArrayList<>(List.of(EnergyType.WATER)))
                .attachedEnergyIds(new ArrayList<>(List.of("xy1-131")))
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();

        PlayerState attackerState = playerState(PLAYER_1, List.of(), cardIds(5));
        attackerState.setActivePokemon(froakie);

        if (hasBench) {
            BenchPokemon bench = BenchPokemon.builder()
                    .instanceId("bench-1")
                    .cardId("xy1-2")
                    .attachedEnergyIds(new ArrayList<>())
                    .evolutionStack(new ArrayList<>())
                    .damageCounters(0)
                    .build();
            attackerState.setBench(new ArrayList<>(List.of(bench)));
        }

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

        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(defender);

        BoardState state = boardState(attackerState, defenderState);

        Map<String, Object> payload = new HashMap<>();
        payload.put("attackName", "Bounce");
        if (replacementInstanceId != null) {
            payload.put("replacementInstanceId", replacementInstanceId);
        }

        GameAction act = GameAction.builder()
                .type(GameActionType.DECLARE_ATTACK)
                .playerId(PLAYER_1)
                .payload(payload)
                .build();

        return AttackContext.builder()
                .boardState(state)
                .action(act)
                .attackName("bounce")
                .modifiers(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
    }
}