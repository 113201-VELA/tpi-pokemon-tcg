package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.Card;
import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.engine.CardLookupPort;
import com.pokemon.tcg.engine.CoinFlipService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class LaprasEffectTest {

    private CoinFlipService coinFlipService;
    private CardLookupPort cardLookupPort;
    private LaprasEffect effect;

    private static final String WATER_ENERGY    = "xy1-131";
    private static final String NON_WATER_ENERGY = "xy1-133"; // fire energy

    @BeforeEach
    void setUp() {
        coinFlipService = mock(CoinFlipService.class);
        cardLookupPort  = mock(CardLookupPort.class);
        effect = new LaprasEffect(coinFlipService, cardLookupPort);

        Card waterCard = mock(Card.class);
        when(waterCard.getTypes()).thenReturn(List.of(EnergyType.WATER.name()));
        when(cardLookupPort.findCardById(WATER_ENERGY)).thenReturn(Optional.of(waterCard));

        Card fireCard = mock(Card.class);
        when(fireCard.getTypes()).thenReturn(List.of(EnergyType.FIRE.name()));
        when(cardLookupPort.findCardById(NON_WATER_ENERGY)).thenReturn(Optional.of(fireCard));
    }

    // ─── getSupportedAttacks ──────────────────────────────────────────────────

    @Test
    void shouldSupportBothAttacks() {
        assertThat(effect.getSupportedAttacks())
                .contains("lapras|seafaring", "lapras|hydro pump");
    }

    // ─── Seafaring ────────────────────────────────────────────────────────────

    @Test
    void seafaring_shouldAttachWaterEnergy_toBench_onHeads() {
        when(coinFlipService.flip()).thenReturn(CoinResult.HEADS, CoinResult.TAILS, CoinResult.TAILS);
        AttackContext ctx = buildSeafaringContext(
                List.of(WATER_ENERGY), // discard
                1,                      // bench size
                null);                  // no explicit targets

        effect.apply(ctx);

        PlayerState attacker = ctx.getBoardState().getStateFor(PLAYER_1);
        assertThat(attacker.getBench().get(0).getAttachedEnergyIds()).contains(WATER_ENERGY);
        assertThat(attacker.getDiscard()).doesNotContain(WATER_ENERGY);
    }

    @Test
    void seafaring_shouldAttachUpToHeadsCount_fromDiscard() {
        when(coinFlipService.flip()).thenReturn(CoinResult.HEADS, CoinResult.HEADS, CoinResult.HEADS);
        AttackContext ctx = buildSeafaringContext(
                List.of(WATER_ENERGY, WATER_ENERGY), // only 2 in discard but 3 heads
                2,
                null);

        effect.apply(ctx);

        PlayerState attacker = ctx.getBoardState().getStateFor(PLAYER_1);
        // Only 2 energies available even though 3 heads
        int totalAttached = attacker.getBench().stream()
                .mapToInt(b -> b.getAttachedEnergyIds().size())
                .sum();
        assertThat(totalAttached).isEqualTo(2);
        assertThat(attacker.getDiscard()).isEmpty();
    }

    @Test
    void seafaring_shouldDoNothing_whenAllTails() {
        when(coinFlipService.flip()).thenReturn(CoinResult.TAILS, CoinResult.TAILS, CoinResult.TAILS);
        AttackContext ctx = buildSeafaringContext(
                List.of(WATER_ENERGY),
                1,
                null);

        effect.apply(ctx);

        PlayerState attacker = ctx.getBoardState().getStateFor(PLAYER_1);
        assertThat(attacker.getBench().get(0).getAttachedEnergyIds()).isEmpty();
        assertThat(attacker.getDiscard()).contains(WATER_ENERGY);
    }

    @Test
    void seafaring_shouldDoNothing_whenNoWaterEnergyInDiscard() {
        when(coinFlipService.flip()).thenReturn(CoinResult.HEADS, CoinResult.HEADS, CoinResult.HEADS);
        AttackContext ctx = buildSeafaringContext(
                List.of(NON_WATER_ENERGY), // fire energy, not water
                1,
                null);

        effect.apply(ctx);

        PlayerState attacker = ctx.getBoardState().getStateFor(PLAYER_1);
        assertThat(attacker.getBench().get(0).getAttachedEnergyIds()).isEmpty();
    }

    @Test
    void seafaring_shouldDoNothing_whenNoBench() {
        when(coinFlipService.flip()).thenReturn(CoinResult.HEADS, CoinResult.HEADS, CoinResult.HEADS);
        AttackContext ctx = buildSeafaringContext(
                List.of(WATER_ENERGY),
                0, // no bench
                null);

        effect.apply(ctx);

        // No exception thrown
        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDiscard())
                .contains(WATER_ENERGY);
    }

    @Test
    void seafaring_shouldRespectPlayerTargetChoice() {
        when(coinFlipService.flip()).thenReturn(CoinResult.HEADS, CoinResult.TAILS, CoinResult.TAILS);

        List<Map<String, String>> targets = List.of(
                Map.of("instanceId", "bench-1", "energyCardId", WATER_ENERGY));

        AttackContext ctx = buildSeafaringContext(
                List.of(WATER_ENERGY),
                2, // two bench Pokémon
                targets);

        effect.apply(ctx);

        PlayerState attacker = ctx.getBoardState().getStateFor(PLAYER_1);
        BenchPokemon targetBench = attacker.getBench().stream()
                .filter(b -> b.getInstanceId().equals("bench-1"))
                .findFirst().orElseThrow();
        assertThat(targetBench.getAttachedEnergyIds()).contains(WATER_ENERGY);
    }

    @Test
    void seafaring_shouldFlipExactly3Coins() {
        when(coinFlipService.flip()).thenReturn(CoinResult.TAILS);
        AttackContext ctx = buildSeafaringContext(List.of(), 1, null);

        effect.apply(ctx);

        verify(coinFlipService, times(3)).flip();
    }

    // ─── Hydro Pump ───────────────────────────────────────────────────────────

    @Test
    void hydroPump_shouldAdd20Damage_perWaterEnergy() {
        AttackContext ctx = buildHydroPumpContext(
                List.of(WATER_ENERGY, WATER_ENERGY, WATER_ENERGY));

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).hasSize(1);
        assertThat(ctx.getModifiers().get(0).amount()).isEqualTo(60); // 3 × 20
    }

    @Test
    void hydroPump_shouldNotAddBonus_whenNoWaterEnergyAttached() {
        AttackContext ctx = buildHydroPumpContext(List.of(NON_WATER_ENERGY));

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    @Test
    void hydroPump_shouldOnlyCountWaterEnergies_whenMixedAttached() {
        AttackContext ctx = buildHydroPumpContext(
                List.of(WATER_ENERGY, NON_WATER_ENERGY, WATER_ENERGY));

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).hasSize(1);
        assertThat(ctx.getModifiers().get(0).amount()).isEqualTo(40); // 2 × 20
    }

    @Test
    void hydroPump_shouldNotAddBonus_whenNoEnergyAttached() {
        AttackContext ctx = buildHydroPumpContext(List.of());

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    @Test
    void hydroPump_shouldNotFlipCoins() {
        AttackContext ctx = buildHydroPumpContext(List.of(WATER_ENERGY));

        effect.apply(ctx);

        verifyNoInteractions(coinFlipService);
    }

    // ─── unknown attack ───────────────────────────────────────────────────────

    @Test
    void unknownAttack_shouldDoNothing() {
        AttackContext ctx = buildHydroPumpContext(List.of(WATER_ENERGY));
        ctx = AttackContext.builder()
                .boardState(ctx.getBoardState())
                .action(ctx.getAction())
                .attackName("unknown")
                .modifiers(new ArrayList<>())
                .events(new ArrayList<>())
                .build();

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
        verifyNoInteractions(coinFlipService);
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private AttackContext buildSeafaringContext(List<String> discardEnergies,
                                                int benchSize,
                                                List<Map<String, String>> benchTargets) {
        ActivePokemon lapras = ActivePokemon.builder()
                .instanceId("lapras-1")
                .cardId("xy1-35")
                .types(new ArrayList<>(List.of(EnergyType.WATER)))
                .attachedEnergyIds(new ArrayList<>(List.of(WATER_ENERGY)))
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();

        PlayerState attackerState = playerState(PLAYER_1, List.of(), cardIds(5));
        attackerState.setActivePokemon(lapras);
        attackerState.setDiscard(new ArrayList<>(discardEnergies));

        List<BenchPokemon> bench = new ArrayList<>();
        for (int i = 0; i < benchSize; i++) {
            bench.add(BenchPokemon.builder()
                    .instanceId("bench-" + i)
                    .cardId("xy1-2")
                    .attachedEnergyIds(new ArrayList<>())
                    .evolutionStack(new ArrayList<>())
                    .damageCounters(0)
                    .build());
        }
        attackerState.setBench(bench);

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
        payload.put("attackName", "Seafaring");
        if (benchTargets != null) {
            payload.put("benchTargets", benchTargets);
        }

        GameAction act = GameAction.builder()
                .type(GameActionType.DECLARE_ATTACK)
                .playerId(PLAYER_1)
                .payload(payload)
                .build();

        return AttackContext.builder()
                .boardState(state)
                .action(act)
                .attackName("seafaring")
                .modifiers(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
    }

    private AttackContext buildHydroPumpContext(List<String> attachedEnergies) {
        ActivePokemon lapras = ActivePokemon.builder()
                .instanceId("lapras-1")
                .cardId("xy1-35")
                .types(new ArrayList<>(List.of(EnergyType.WATER)))
                .attachedEnergyIds(new ArrayList<>(attachedEnergies))
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();

        PlayerState attackerState = playerState(PLAYER_1, List.of(), cardIds(5));
        attackerState.setActivePokemon(lapras);

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

        GameAction act = GameAction.builder()
                .type(GameActionType.DECLARE_ATTACK)
                .playerId(PLAYER_1)
                .payload(Map.of("attackName", "Hydro Pump"))
                .build();

        return AttackContext.builder()
                .boardState(state)
                .action(act)
                .attackName("hydro pump")
                .modifiers(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
    }
}