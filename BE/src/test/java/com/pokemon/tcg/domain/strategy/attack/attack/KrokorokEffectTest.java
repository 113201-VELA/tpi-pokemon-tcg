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

class KrokorokEffectTest {

    private CoinFlipService coinFlipService;
    private KrokorokEffect effect;

    private static final String DARKNESS_ENERGY = "xy1-97";
    private static final String WATER_ENERGY    = "xy1-131";

    @BeforeEach
    void setUp() {
        coinFlipService = mock(CoinFlipService.class);
        effect = new KrokorokEffect(coinFlipService);
    }

    @Test
    void shouldSupportBothAttacks() {
        assertThat(effect.getSupportedAttacks())
                .contains("krokorok|crunch", "krokorok|darkness fang");
    }

    // ─── Crunch — heads ───────────────────────────────────────────────────────

    @Test
    void crunch_shouldDiscardEnergyFromDefender_onHeads() {
        when(coinFlipService.flip()).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext("crunch", DARKNESS_ENERGY);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getOpponentState(PLAYER_1)
                .getActivePokemon().getAttachedEnergyIds())
                .doesNotContain(DARKNESS_ENERGY);
    }

    @Test
    void crunch_shouldMoveEnergyToOpponentDiscard_onHeads() {
        when(coinFlipService.flip()).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext("crunch", DARKNESS_ENERGY);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getOpponentState(PLAYER_1).getDiscard())
                .contains(DARKNESS_ENERGY);
    }

    @Test
    void crunch_shouldDoNothing_whenEnergyNotAttached_onHeads() {
        when(coinFlipService.flip()).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext("crunch", WATER_ENERGY);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getOpponentState(PLAYER_1)
                .getActivePokemon().getAttachedEnergyIds())
                .containsExactly(DARKNESS_ENERGY);
    }

    // ─── Crunch — tails ───────────────────────────────────────────────────────

    @Test
    void crunch_shouldNotDiscardEnergy_onTails() {
        when(coinFlipService.flip()).thenReturn(CoinResult.TAILS);
        AttackContext ctx = buildContext("crunch", DARKNESS_ENERGY);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getOpponentState(PLAYER_1)
                .getActivePokemon().getAttachedEnergyIds())
                .contains(DARKNESS_ENERGY);
    }

    @Test
    void crunch_shouldNotAddModifiers() {
        when(coinFlipService.flip()).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext("crunch", DARKNESS_ENERGY);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    // ─── Darkness Fang ────────────────────────────────────────────────────────

    @Test
    void darknessFang_shouldNotAddModifiers() {
        AttackContext ctx = buildContext("darkness fang", null);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
        verifyNoInteractions(coinFlipService);
    }

    // ─── unknown attack ───────────────────────────────────────────────────────

    @Test
    void unknownAttack_shouldDoNothing() {
        AttackContext ctx = buildContext("unknown", null);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
        verifyNoInteractions(coinFlipService);
    }

    // ─── helper ───────────────────────────────────────────────────────────────

    private AttackContext buildContext(String attackName, String energyToDiscardId) {
        ActivePokemon krokorok = ActivePokemon.builder()
                .instanceId("krokorok-1")
                .cardId("xy1-70")
                .types(new ArrayList<>(List.of(EnergyType.DARKNESS)))
                .attachedEnergyIds(new ArrayList<>(
                        List.of(DARKNESS_ENERGY, DARKNESS_ENERGY, "xy1-95")))
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
                .attachedEnergyIds(new ArrayList<>(List.of(DARKNESS_ENERGY)))
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();

        PlayerState attackerState = playerState(PLAYER_1, List.of(), cardIds(5));
        attackerState.setActivePokemon(krokorok);

        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(defender);
        defenderState.setDiscard(new ArrayList<>());

        BoardState state = boardState(attackerState, defenderState);

        Map<String, Object> payload = new HashMap<>();
        payload.put("attackName", attackName);
        if (energyToDiscardId != null) {
            payload.put("energyToDiscardId", energyToDiscardId);
        }

        GameAction act = GameAction.builder()
                .type(GameActionType.DECLARE_ATTACK)
                .playerId(PLAYER_1)
                .payload(payload)
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