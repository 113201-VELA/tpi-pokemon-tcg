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

class Jigglypuff88EffectTest {

    private CoinFlipService     coinFlipService;
    private Jigglypuff88Effect  effect;

    @BeforeEach
    void setUp() {
        coinFlipService = mock(CoinFlipService.class);
        effect = new Jigglypuff88Effect(coinFlipService);
    }

    @Test
    void shouldSupportOnlyBodySlam() {
        assertThat(effect.getSupportedAttacks()).containsExactly("jigglypuff|body slam");
    }

    // ─── Body Slam ────────────────────────────────────────────────────────────

    @Test
    void bodySlam_shouldParalyzeDefender_onHeads() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString()))
                .thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext("body slam", new HashSet<>());

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState().getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getConditions()).contains(SpecialCondition.PARALYZED);
    }

    @Test
    void bodySlam_shouldNotParalyze_onTails() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString()))
                .thenReturn(CoinResult.TAILS);
        AttackContext ctx = buildContext("body slam", new HashSet<>());

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState().getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getConditions()).doesNotContain(SpecialCondition.PARALYZED);
    }

    @Test
    void bodySlam_shouldReplaceAsleep_withParalyzed_onHeads() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString()))
                .thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext("body slam",
                new HashSet<>(Set.of(SpecialCondition.ASLEEP)));

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState().getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getConditions())
                .contains(SpecialCondition.PARALYZED)
                .doesNotContain(SpecialCondition.ASLEEP);
    }

    @Test
    void bodySlam_shouldReplaceConfused_withParalyzed_onHeads() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString()))
                .thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext("body slam",
                new HashSet<>(Set.of(SpecialCondition.CONFUSED)));

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState().getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getConditions())
                .contains(SpecialCondition.PARALYZED)
                .doesNotContain(SpecialCondition.CONFUSED);
    }

    @Test
    void bodySlam_shouldKeepCoexistingConditions_likePoisonedAndBurned() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString()))
                .thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext("body slam",
                new HashSet<>(Set.of(SpecialCondition.POISONED, SpecialCondition.BURNED)));

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState().getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getConditions())
                .contains(SpecialCondition.PARALYZED, SpecialCondition.POISONED, SpecialCondition.BURNED);
    }

    @Test
    void bodySlam_shouldAlwaysFlipCoin() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString()))
                .thenReturn(CoinResult.TAILS);
        AttackContext ctx = buildContext("body slam", new HashSet<>());

        effect.apply(ctx);

        verify(coinFlipService, times(1)).flipAndEmit(any(AttackContext.class), eq(PLAYER_1));
    }

    // ─── unknown attack ───────────────────────────────────────────────────────

    @Test
    void unknownAttack_shouldDoNothing() {
        AttackContext ctx = buildContext("unknown", new HashSet<>());

        effect.apply(ctx);

        verifyNoInteractions(coinFlipService);
        ActivePokemon defender = ctx.getBoardState().getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getConditions()).isEmpty();
    }

    // ─── helper ───────────────────────────────────────────────────────────────

    private AttackContext buildContext(String attackName, Set<SpecialCondition> defenderConditions) {
        ActivePokemon jigglypuff = ActivePokemon.builder()
                .instanceId("jigglypuff-1")
                .cardId("xy1-88")
                .types(new ArrayList<>(List.of(EnergyType.FAIRY)))
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(0)
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
                .conditions(defenderConditions)
                .activeEffects(new ArrayList<>())
                .build();

        PlayerState attackerState = playerState(PLAYER_1, List.of(), cardIds(5));
        attackerState.setActivePokemon(jigglypuff);

        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
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
}