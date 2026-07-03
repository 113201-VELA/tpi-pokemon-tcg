package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.DamageModifier;
import com.pokemon.tcg.engine.CoinFlipService;
import com.pokemon.tcg.engine.StatusEffectManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CloysterEffectTest {

    private CoinFlipService coinFlipService;
    private StatusEffectManager statusEffectManager;
    private CloysterEffect effect;

    private static final String WATER_ENERGY = "xy1-131";

    @BeforeEach
    void setUp() {
        coinFlipService = mock(CoinFlipService.class);
        statusEffectManager = mock(StatusEffectManager.class);
        effect = new CloysterEffect(coinFlipService, statusEffectManager);
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

    // ─── getSupportedAttacks ──────────────────────────────────────────────────

    @Test
    void shouldSupportBothAttacks() {
        assertThat(effect.getSupportedAttacks())
                .contains("cloyster|clamp crush", "cloyster|spike cannon");
    }

    // ─── Clamp Crush — heads ──────────────────────────────────────────────────

    @Test
    void clampCrush_onHeads_shouldParalyzeDefender() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext("clamp crush", List.of(WATER_ENERGY));

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState().getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getConditions()).contains(SpecialCondition.PARALYZED);
    }

    @Test
    void clampCrush_onHeads_shouldReplaceAsleepWithParalyzed() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext("clamp crush", List.of(WATER_ENERGY));
        ctx.getBoardState().getStateFor(PLAYER_2).getActivePokemon()
                .getConditions().add(SpecialCondition.ASLEEP);

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState().getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getConditions()).contains(SpecialCondition.PARALYZED);
        assertThat(defender.getConditions()).doesNotContain(SpecialCondition.ASLEEP);
    }

    @Test
    void clampCrush_onHeads_shouldDiscardOneEnergyFromDefender() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext("clamp crush", List.of(WATER_ENERGY));

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState().getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getAttachedEnergyIds()).isEmpty();
        assertThat(ctx.getBoardState().getStateFor(PLAYER_2).getDiscard())
                .contains(WATER_ENERGY);
    }

    @Test
    void clampCrush_onHeads_shouldKeepRemainingEnergies_whenMultipleAttached() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.HEADS);
        String secondEnergy = "xy1-132";
        AttackContext ctx = buildContext("clamp crush", List.of(WATER_ENERGY, secondEnergy));

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState().getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getAttachedEnergyIds()).containsExactly(secondEnergy);
    }

    @Test
    void clampCrush_onHeads_shouldNotDiscardEnergy_whenNoneAttached() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext("clamp crush", List.of());

        effect.apply(ctx);

        // Paralyzed still applies, just no energy to discard
        ActivePokemon defender = ctx.getBoardState().getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getConditions()).contains(SpecialCondition.PARALYZED);
        assertThat(ctx.getBoardState().getStateFor(PLAYER_2).getDiscard()).isEmpty();
    }

    // ─── Clamp Crush — tails ─────────────────────────────────────────────────

    @Test
    void clampCrush_onTails_shouldNotParalyzeDefender() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.TAILS);
        AttackContext ctx = buildContext("clamp crush", List.of(WATER_ENERGY));

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState().getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getConditions()).doesNotContain(SpecialCondition.PARALYZED);
    }

    @Test
    void clampCrush_onTails_shouldNotDiscardEnergy() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.TAILS);
        AttackContext ctx = buildContext("clamp crush", List.of(WATER_ENERGY));

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState().getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getAttachedEnergyIds()).containsExactly(WATER_ENERGY);
        assertThat(ctx.getBoardState().getStateFor(PLAYER_2).getDiscard()).isEmpty();
    }

    // ─── Spike Cannon ─────────────────────────────────────────────────────────

    @Test
    void spikeCannon_shouldAdd150Damage_whenAll5Heads() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext("spike cannon", List.of());

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).hasSize(1);
        DamageModifier mod = ctx.getModifiers().get(0);
        assertThat(mod.amount()).isEqualTo(150);
    }

    @Test
    void spikeCannon_shouldAdd0Damage_whenAll5Tails() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.TAILS);
        AttackContext ctx = buildContext("spike cannon", List.of());

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).hasSize(1);
        DamageModifier mod = ctx.getModifiers().get(0);
        assertThat(mod.amount()).isEqualTo(0);
    }

    @Test
    void spikeCannon_shouldAdd90Damage_when3Heads() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString()))
                .thenReturn(CoinResult.HEADS)
                .thenReturn(CoinResult.HEADS)
                .thenReturn(CoinResult.HEADS)
                .thenReturn(CoinResult.TAILS)
                .thenReturn(CoinResult.TAILS);
        AttackContext ctx = buildContext("spike cannon", List.of());

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).hasSize(1);
        assertThat(ctx.getModifiers().get(0).amount()).isEqualTo(90);
    }

    @Test
    void spikeCannon_shouldFlipExactly5Coins() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.TAILS);
        AttackContext ctx = buildContext("spike cannon", List.of());

        effect.apply(ctx);

        verify(coinFlipService, times(5)).flipAndEmit(any(AttackContext.class), anyString());
    }

    // ─── unknown attack ───────────────────────────────────────────────────────

    @Test
    void unknownAttack_shouldDoNothing() {
        AttackContext ctx = buildContext("unknown", List.of(WATER_ENERGY));

        effect.apply(ctx);

        verifyNoInteractions(coinFlipService);
        assertThat(ctx.getModifiers()).isEmpty();
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private AttackContext buildContext(String attackName, List<String> defenderEnergies) {
        ActivePokemon cloyster = ActivePokemon.builder()
                .instanceId("cloyster-1")
                .cardId("xy1-32")
                .types(new ArrayList<>(List.of(EnergyType.WATER)))
                .attachedEnergyIds(new ArrayList<>(
                        List.of(WATER_ENERGY, WATER_ENERGY)))
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
                .attachedEnergyIds(new ArrayList<>(defenderEnergies))
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();

        PlayerState attackerState = playerState(PLAYER_1, List.of(), cardIds(5));
        attackerState.setActivePokemon(cloyster);

        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
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