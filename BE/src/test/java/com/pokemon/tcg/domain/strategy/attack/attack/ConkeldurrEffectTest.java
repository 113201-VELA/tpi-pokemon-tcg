package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.DamageModifier;
import com.pokemon.tcg.engine.CoinFlipService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ConkeldurrEffectTest {

    private CoinFlipService coinFlipService;
    private ConkeldurrEffect effect;

    @BeforeEach
    void setUp() {
        coinFlipService = mock(CoinFlipService.class);
        effect = new ConkeldurrEffect(coinFlipService);
    }

    @Test
    void shouldSupportBothAttacks() {
        assertThat(effect.getSupportedAttacks())
                .contains("conkeldurr|wake-up slap", "conkeldurr|dynamic punch");
    }

    // ─── Wake-Up Slap ─────────────────────────────────────────────────────────

    @Test
    void wakeUpSlap_shouldAdd60Modifier_whenDefenderHasCondition() {
        AttackContext ctx = buildContext("wake-up slap",
                Set.of(SpecialCondition.POISONED), CoinResult.TAILS);

        effect.apply(ctx);

        int bonus = ctx.getModifiers().stream()
                .mapToInt(DamageModifier::amount).sum();
        assertThat(bonus).isEqualTo(60);
    }

    @Test
    void wakeUpSlap_shouldClearAllConditions_whenDefenderHasCondition() {
        AttackContext ctx = buildContext("wake-up slap",
                new HashSet<>(Set.of(SpecialCondition.POISONED, SpecialCondition.BURNED)),
                CoinResult.TAILS);

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState()
                .getOpponentState(PLAYER_1).getActivePokemon();
        assertThat(defender.getConditions()).isEmpty();
    }

    @Test
    void wakeUpSlap_shouldNotAddModifier_whenDefenderHasNoCondition() {
        AttackContext ctx = buildContext("wake-up slap", new HashSet<>(), CoinResult.TAILS);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    @Test
    void wakeUpSlap_shouldNotFlipCoin() {
        AttackContext ctx = buildContext("wake-up slap",
                Set.of(SpecialCondition.PARALYZED), CoinResult.TAILS);

        effect.apply(ctx);

        verifyNoInteractions(coinFlipService);
    }

    // ─── Dynamic Punch ────────────────────────────────────────────────────────

    @Test
    void dynamicPunch_shouldAdd40ModifierAndConfuse_onHeads() {
        AttackContext ctx = buildContext("dynamic punch", new HashSet<>(), CoinResult.HEADS);

        effect.apply(ctx);

        int bonus = ctx.getModifiers().stream()
                .mapToInt(DamageModifier::amount).sum();
        assertThat(bonus).isEqualTo(40);

        ActivePokemon defender = ctx.getBoardState()
                .getOpponentState(PLAYER_1).getActivePokemon();
        assertThat(defender.getConditions()).contains(SpecialCondition.CONFUSED);
    }

    @Test
    void dynamicPunch_shouldNotAddModifierOrConfuse_onTails() {
        AttackContext ctx = buildContext("dynamic punch", new HashSet<>(), CoinResult.TAILS);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
        ActivePokemon defender = ctx.getBoardState()
                .getOpponentState(PLAYER_1).getActivePokemon();
        assertThat(defender.getConditions()).doesNotContain(SpecialCondition.CONFUSED);
    }

    @Test
    void dynamicPunch_shouldFlipExactlyOneCoin() {
        AttackContext ctx = buildContext("dynamic punch", new HashSet<>(), CoinResult.TAILS);

        effect.apply(ctx);

        verify(coinFlipService, times(1)).flipAndEmit(any(AttackContext.class), anyString());
    }

    // ─── unknown attack ───────────────────────────────────────────────────────

    @Test
    void unknownAttack_shouldDoNothing() {
        AttackContext ctx = buildContext("unknown", new HashSet<>(), CoinResult.HEADS);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
        verifyNoInteractions(coinFlipService);
    }

    // ─── helper ───────────────────────────────────────────────────────────────

    private AttackContext buildContext(String attackName,
                                       Set<SpecialCondition> defenderConditions,
                                       CoinResult coinResult) {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(coinResult);

        ActivePokemon conkeldurr = ActivePokemon.builder()
                .instanceId("conkeldurr-1")
                .cardId("xy1-67")
                .types(new ArrayList<>(List.of(EnergyType.FIGHTING)))
                .attachedEnergyIds(new ArrayList<>(
                        List.of("xy1-96", "xy1-96", "xy1-95")))
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
                .conditions(new HashSet<>(defenderConditions))
                .activeEffects(new ArrayList<>())
                .build();

        PlayerState attackerState = playerState(PLAYER_1, List.of(), cardIds(5));
        attackerState.setActivePokemon(conkeldurr);

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