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

class PawniardEffectTest {

    private CoinFlipService coinFlipService;
    private PawniardEffect  effect;

    private static final String ENERGY_1 = "xy1-136";
    private static final String ENERGY_2 = "xy1-131";

    @BeforeEach
    void setUp() {
        coinFlipService = mock(CoinFlipService.class);
        effect = new PawniardEffect(coinFlipService);
    }

    @Test
    void shouldSupportOnlyCutDown() {
        assertThat(effect.getSupportedAttacks()).containsExactly("pawniard|cut down");
    }

    // ─── Cut Down ─────────────────────────────────────────────────────────────

    @Test
    void cutDown_shouldDiscardRequestedEnergy_onHeads() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString()))
                .thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext("cut down",
                List.of(ENERGY_1, ENERGY_2), ENERGY_2);

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState().getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getAttachedEnergyIds()).containsExactly(ENERGY_1);
        assertThat(ctx.getBoardState().getStateFor(PLAYER_2).getDiscard()).contains(ENERGY_2);
    }

    @Test
    void cutDown_shouldDiscardFirstEnergy_whenNoneSpecified() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString()))
                .thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext("cut down",
                List.of(ENERGY_1, ENERGY_2), null);

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState().getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getAttachedEnergyIds()).containsExactly(ENERGY_2);
        assertThat(ctx.getBoardState().getStateFor(PLAYER_2).getDiscard()).contains(ENERGY_1);
    }

    @Test
    void cutDown_shouldDiscardFirstEnergy_whenRequestedNotAttached() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString()))
                .thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext("cut down",
                List.of(ENERGY_1), "xy1-999"); // not attached

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState().getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getAttachedEnergyIds()).isEmpty();
        assertThat(ctx.getBoardState().getStateFor(PLAYER_2).getDiscard()).contains(ENERGY_1);
    }

    @Test
    void cutDown_shouldNotDiscard_onTails() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString()))
                .thenReturn(CoinResult.TAILS);
        AttackContext ctx = buildContext("cut down",
                List.of(ENERGY_1, ENERGY_2), null);

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState().getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getAttachedEnergyIds()).containsExactly(ENERGY_1, ENERGY_2);
        assertThat(ctx.getBoardState().getStateFor(PLAYER_2).getDiscard()).isEmpty();
    }

    @Test
    void cutDown_shouldDoNothing_whenDefenderHasNoEnergy() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString()))
                .thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext("cut down", List.of(), null);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_2).getDiscard()).isEmpty();
    }

    @Test
    void cutDown_shouldAlwaysFlipCoin() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString()))
                .thenReturn(CoinResult.TAILS);
        AttackContext ctx = buildContext("cut down", List.of(ENERGY_1), null);

        effect.apply(ctx);

        verify(coinFlipService, times(1)).flipAndEmit(any(AttackContext.class), eq(PLAYER_1));
    }

    // ─── Metal Claw / unknown attack ────────────────────────────────────────

    @Test
    void metalClaw_shouldDoNothing() {
        AttackContext ctx = buildContext("metal claw", List.of(ENERGY_1), null);

        effect.apply(ctx);

        verifyNoInteractions(coinFlipService);
        assertThat(ctx.getBoardState().getStateFor(PLAYER_2).getDiscard()).isEmpty();
    }

    // ─── helper ───────────────────────────────────────────────────────────────

    private AttackContext buildContext(String attackName,
                                       List<String> defenderEnergies,
                                       String requestedEnergyId) {
        ActivePokemon pawniard = ActivePokemon.builder()
                .instanceId("pawniard-1")
                .cardId("xy1-81")
                .types(new ArrayList<>(List.of(EnergyType.METAL)))
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
                .attachedEnergyIds(new ArrayList<>(defenderEnergies))
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();

        PlayerState attackerState = playerState(PLAYER_1, List.of(), cardIds(5));
        attackerState.setActivePokemon(pawniard);

        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(defender);
        defenderState.setDiscard(new ArrayList<>());

        BoardState state = boardState(attackerState, defenderState);

        Map<String, Object> payload = new HashMap<>();
        payload.put("attackName", attackName);
        if (requestedEnergyId != null) payload.put("energyCardId", requestedEnergyId);

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