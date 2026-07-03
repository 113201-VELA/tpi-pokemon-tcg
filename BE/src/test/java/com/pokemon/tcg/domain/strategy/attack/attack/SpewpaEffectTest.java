package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.engine.CoinFlipService;
import com.pokemon.tcg.engine.StatusEffectManager;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpewpaEffectTest {

    @Mock
    private CoinFlipService coinFlipService;
    @Mock
    private StatusEffectManager statusEffectManager;

    private SpewpaEffect effect;

    @BeforeEach
    void setUp() {
        effect = new SpewpaEffect(coinFlipService, statusEffectManager);
        // NOTE: the statusEffectManager.applyCondition stub used to live here,
        // but apply_shouldNotParalyzeDefender_whenTails never triggers it
        // (tails = no condition applied). MockitoExtension runs in
        // strict-stubs mode, so an unused stub there throws
        // UnnecessaryStubbingException. Moved into stubApplyCondition(),
        // called only from the tests that actually need it.
    }

    private void stubApplyCondition() {
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

    @Test
    void apply_shouldParalyzeDefender_whenHeads() {
        stubApplyCondition();
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext();

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getConditions())
                .contains(SpecialCondition.PARALYZED);
    }

    @Test
    void apply_shouldNotParalyzeDefender_whenTails() {
        // No stubApplyCondition() here: tails never calls it.
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.TAILS);
        AttackContext ctx = buildContext();

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getConditions())
                .doesNotContain(SpecialCondition.PARALYZED);
    }

    @Test
    void apply_shouldReplaceAsleep_withParalyzed_whenHeads() {
        stubApplyCondition();
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext();
        ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getConditions().add(SpecialCondition.ASLEEP);

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState()
                .getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getConditions()).contains(SpecialCondition.PARALYZED);
        assertThat(defender.getConditions()).doesNotContain(SpecialCondition.ASLEEP);
    }

    @Test
    void apply_shouldReplaceConfused_withParalyzed_whenHeads() {
        stubApplyCondition();
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext();
        ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getConditions().add(SpecialCondition.CONFUSED);

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState()
                .getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getConditions()).contains(SpecialCondition.PARALYZED);
        assertThat(defender.getConditions()).doesNotContain(SpecialCondition.CONFUSED);
    }

    @Test
    void apply_shouldNotAffectAttacker_whenHeads() {
        stubApplyCondition();
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext();

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getConditions()).isEmpty();
    }

    @Test
    void apply_shouldNotAddModifiers() {
        stubApplyCondition();
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext();

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    private AttackContext buildContext() {
        ActivePokemon spewpa = ActivePokemon.builder()
                .instanceId("spewpa-1")
                .cardId("xy1-16")
                .types(new ArrayList<>(List.of(EnergyType.COLORLESS)))
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
                .cardId("xy1-20")
                .types(new ArrayList<>(List.of(EnergyType.FIRE)))
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();

        PlayerState attackerState = playerState(PLAYER_1, List.of(), cardIds(5));
        attackerState.setActivePokemon(spewpa);
        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(defender);

        BoardState state = boardState(attackerState, defenderState);
        GameAction act = action(GameActionType.DECLARE_ATTACK, PLAYER_1,
                "attackName", "Stun Spore");

        return AttackContext.builder()
                .boardState(state)
                .action(act)
                .attackName("Stun Spore")
                .modifiers(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
    }
}