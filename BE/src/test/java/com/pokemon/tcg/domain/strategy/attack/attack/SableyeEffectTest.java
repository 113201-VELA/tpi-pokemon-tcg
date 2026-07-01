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

class SableyeEffectTest {

    private CoinFlipService coinFlipService;
    private SableyeEffect effect;

    private static final String DARKNESS_ENERGY = "xy1-97";
    private static final String WATER_ENERGY    = "xy1-131";

    @BeforeEach
    void setUp() {
        coinFlipService = mock(CoinFlipService.class);
        effect = new SableyeEffect(coinFlipService);
    }

    @Test
    void shouldSupportBothAttacks() {
        assertThat(effect.getSupportedAttacks())
                .contains("sableye|filch", "sableye|rip claw");
    }

    // ─── Filch ────────────────────────────────────────────────────────────────

    @Test
    void filch_shouldDrawOneCardIntoHand() {
        AttackContext ctx = buildContext("filch", 3, null, CoinResult.TAILS);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getHand()).hasSize(1);
        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDeck()).hasSize(2);
    }

    @Test
    void filch_shouldDoNothing_whenDeckIsEmpty() {
        AttackContext ctx = buildContext("filch", 0, null, CoinResult.TAILS);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getHand()).isEmpty();
    }

    @Test
    void filch_shouldNotFlipCoin() {
        AttackContext ctx = buildContext("filch", 3, null, CoinResult.TAILS);

        effect.apply(ctx);

        verifyNoInteractions(coinFlipService);
    }

    @Test
    void filch_shouldNotAddModifiers() {
        AttackContext ctx = buildContext("filch", 3, null, CoinResult.TAILS);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    // ─── Rip Claw — heads ─────────────────────────────────────────────────────

    @Test
    void ripClaw_shouldDiscardEnergyFromDefender_onHeads() {
        AttackContext ctx = buildContext("rip claw", 5, DARKNESS_ENERGY, CoinResult.HEADS);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getOpponentState(PLAYER_1)
                .getActivePokemon().getAttachedEnergyIds())
                .doesNotContain(DARKNESS_ENERGY);
    }

    @Test
    void ripClaw_shouldMoveEnergyToOpponentDiscard_onHeads() {
        AttackContext ctx = buildContext("rip claw", 5, DARKNESS_ENERGY, CoinResult.HEADS);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getOpponentState(PLAYER_1).getDiscard())
                .contains(DARKNESS_ENERGY);
    }

    @Test
    void ripClaw_shouldDoNothing_whenEnergyNotAttached_onHeads() {
        AttackContext ctx = buildContext("rip claw", 5, WATER_ENERGY, CoinResult.HEADS);

        effect.apply(ctx);

        // Defender only has DARKNESS_ENERGY attached, not WATER_ENERGY
        assertThat(ctx.getBoardState().getOpponentState(PLAYER_1)
                .getActivePokemon().getAttachedEnergyIds())
                .containsExactly(DARKNESS_ENERGY);
    }

    // ─── Rip Claw — tails ─────────────────────────────────────────────────────

    @Test
    void ripClaw_shouldNotDiscardEnergy_onTails() {
        AttackContext ctx = buildContext("rip claw", 5, DARKNESS_ENERGY, CoinResult.TAILS);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getOpponentState(PLAYER_1)
                .getActivePokemon().getAttachedEnergyIds())
                .contains(DARKNESS_ENERGY);
    }

    @Test
    void ripClaw_shouldNotAddModifiers() {
        AttackContext ctx = buildContext("rip claw", 5, DARKNESS_ENERGY, CoinResult.HEADS);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    // ─── unknown attack ───────────────────────────────────────────────────────

    @Test
    void unknownAttack_shouldDoNothing() {
        AttackContext ctx = buildContext("unknown", 3, null, CoinResult.HEADS);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
        verifyNoInteractions(coinFlipService);
    }

    // ─── helper ───────────────────────────────────────────────────────────────

    private AttackContext buildContext(String attackName,
                                       int attackerDeckSize,
                                       String energyToDiscardId,
                                       CoinResult coinResult) {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(coinResult);

        ActivePokemon sableye = ActivePokemon.builder()
                .instanceId("sableye-1")
                .cardId("xy1-68")
                .types(new ArrayList<>(List.of(EnergyType.DARKNESS)))
                .attachedEnergyIds(new ArrayList<>(List.of(DARKNESS_ENERGY, "xy1-95")))
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

        List<String> deck = attackerDeckSize > 0
                ? cardIds(attackerDeckSize) : new ArrayList<>();
        PlayerState attackerState = playerState(PLAYER_1, new ArrayList<>(), deck);
        attackerState.setActivePokemon(sableye);

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