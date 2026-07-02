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

class YveltalEffectTest {

    private CoinFlipService coinFlipService;
    private CardLookupPort  cardLookupPort;
    private YveltalEffect   effect;

    private static final String DARKNESS_ENERGY_ID = "xy1-127";
    private static final String OTHER_ENERGY_ID     = "xy1-95";
    private static final String BENCH_INSTANCE_ID   = "bench-1";

    @BeforeEach
    void setUp() {
        coinFlipService = mock(CoinFlipService.class);
        cardLookupPort  = mock(CardLookupPort.class);

        Card darknessEnergy = mock(Card.class);
        when(darknessEnergy.isBasicEnergy()).thenReturn(true);
        when(darknessEnergy.getTypes()).thenReturn(List.of(EnergyType.DARKNESS.name()));
        when(cardLookupPort.findCardById(DARKNESS_ENERGY_ID))
                .thenReturn(Optional.of(darknessEnergy));

        Card otherEnergy = mock(Card.class);
        when(otherEnergy.isBasicEnergy()).thenReturn(true);
        when(otherEnergy.getTypes()).thenReturn(List.of(EnergyType.COLORLESS.name()));
        when(cardLookupPort.findCardById(OTHER_ENERGY_ID))
                .thenReturn(Optional.of(otherEnergy));

        effect = new YveltalEffect(coinFlipService, cardLookupPort);
    }

    @Test
    void shouldSupportBothAttacks() {
        assertThat(effect.getSupportedAttacks())
                .containsExactlyInAnyOrder("yveltal|oblivion wing", "yveltal|darkness blade");
    }

    @Test
    void applyShouldDoNothingForUnsupportedAttack() {
        AttackContext ctx = buildContext("unknown attack", List.of(), List.of());

        effect.apply(ctx);

        verifyNoInteractions(coinFlipService);
    }

    // ---------- Oblivion Wing ----------

    @Test
    void oblivionWingShouldAttachDarknessEnergyFromDiscardToBench() {
        AttackContext ctx = buildContextWithPayload("oblivion wing",
                List.of(DARKNESS_ENERGY_ID), List.of(),
                "energyCardId", DARKNESS_ENERGY_ID,
                "targetInstanceId", BENCH_INSTANCE_ID);

        effect.apply(ctx);

        PlayerState attacker = ctx.getBoardState().getStateFor(PLAYER_1);
        assertThat(attacker.getDiscard()).doesNotContain(DARKNESS_ENERGY_ID);
        BenchPokemon target = attacker.getBench().get(0);
        assertThat(target.getAttachedEnergyIds()).contains(DARKNESS_ENERGY_ID);
    }

    @Test
    void oblivionWingShouldDoNothingWhenEnergyNotInDiscard() {
        AttackContext ctx = buildContextWithPayload("oblivion wing", List.of(), List.of(),
                "energyCardId", DARKNESS_ENERGY_ID,
                "targetInstanceId", BENCH_INSTANCE_ID);

        effect.apply(ctx);

        BenchPokemon target = ctx.getBoardState().getStateFor(PLAYER_1).getBench().get(0);
        assertThat(target.getAttachedEnergyIds()).isEmpty();
    }

    @Test
    void oblivionWingShouldDoNothingWhenCardIsNotDarknessEnergy() {
        AttackContext ctx = buildContextWithPayload("oblivion wing",
                List.of(OTHER_ENERGY_ID), List.of(),
                "energyCardId", OTHER_ENERGY_ID,
                "targetInstanceId", BENCH_INSTANCE_ID);

        effect.apply(ctx);

        PlayerState attacker = ctx.getBoardState().getStateFor(PLAYER_1);
        assertThat(attacker.getDiscard()).contains(OTHER_ENERGY_ID);
        BenchPokemon target = attacker.getBench().get(0);
        assertThat(target.getAttachedEnergyIds()).isEmpty();
    }

    @Test
    void oblivionWingShouldDoNothingWhenTargetNotOnBench() {
        AttackContext ctx = buildContextWithPayload("oblivion wing",
                List.of(DARKNESS_ENERGY_ID), List.of(),
                "energyCardId", DARKNESS_ENERGY_ID,
                "targetInstanceId", "nonexistent-instance");

        effect.apply(ctx);

        PlayerState attacker = ctx.getBoardState().getStateFor(PLAYER_1);
        assertThat(attacker.getDiscard()).contains(DARKNESS_ENERGY_ID);
    }

    @Test
    void oblivionWingShouldDoNothingWhenPayloadMissing() {
        AttackContext ctx = buildContext("oblivion wing",
                List.of(DARKNESS_ENERGY_ID), List.of());
        // no energyCardId/targetInstanceId in payload

        effect.apply(ctx);

        PlayerState attacker = ctx.getBoardState().getStateFor(PLAYER_1);
        assertThat(attacker.getDiscard()).contains(DARKNESS_ENERGY_ID);
    }

    // ---------- Darkness Blade ----------

    @Test
    void darknessBladeShouldNotBlockOnHeads() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), eq(PLAYER_1)))
                .thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext("darkness blade", List.of(), List.of());

        effect.apply(ctx);

        ActivePokemon yveltal = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        assertThat(yveltal.getBlockedAttackName()).isNull();
    }

    @Test
    void darknessBladeShouldBlockNextTurnOnTails() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), eq(PLAYER_1)))
                .thenReturn(CoinResult.TAILS);
        AttackContext ctx = buildContext("darkness blade", List.of(), List.of());

        effect.apply(ctx);

        ActivePokemon yveltal = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        assertThat(yveltal.getBlockedAttackName()).isEqualTo("darkness blade");
        // boardState() defaults turnNumber to 1 → expires once turnNumber reaches 3
        assertThat(yveltal.getBlockedAttackUntilTurn()).isEqualTo(3);
    }

    @Test
    void darknessBladeShouldFlipExactlyOnce() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), eq(PLAYER_1)))
                .thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext("darkness blade", List.of(), List.of());

        effect.apply(ctx);

        verify(coinFlipService, times(1)).flipAndEmit(any(AttackContext.class), eq(PLAYER_1));
    }

    // ---------- helpers ----------

    private AttackContext buildContext(String attackName,
                                       List<String> attackerDiscard,
                                       List<String> attackerHand) {
        return buildContextWithPayload(attackName, attackerDiscard, attackerHand);
    }

    /**
     * Builds an AttackContext whose GameAction payload contains "attackName"
     * plus any extra key-value pairs passed in extraPayload. GameAction has
     * no setter (only @Getter/@Builder), so the payload must be complete
     * before the GameAction/AttackContext are built — it can't be mutated
     * afterward.
     */
    private AttackContext buildContextWithPayload(String attackName,
                                                  List<String> attackerDiscard,
                                                  List<String> attackerHand,
                                                  String... extraPayload) {
        ActivePokemon yveltal = ActivePokemon.builder()
                .instanceId("yveltal-1")
                .cardId("xy1-78")
                .types(new ArrayList<>(List.of(EnergyType.DARKNESS)))
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();

        BenchPokemon benchTarget = BenchPokemon.builder()
                .instanceId(BENCH_INSTANCE_ID)
                .cardId("xy1-1")
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .damageCounters(0)
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

        PlayerState attackerState = playerState(PLAYER_1, attackerHand, cardIds(5));
        attackerState.setActivePokemon(yveltal);
        attackerState.setDiscard(new ArrayList<>(attackerDiscard));
        attackerState.setBench(new ArrayList<>(List.of(benchTarget)));

        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(defender);

        BoardState state = boardState(attackerState, defenderState);

        Map<String, Object> payload = new HashMap<>();
        payload.put("attackName", attackName);
        for (int i = 0; i < extraPayload.length - 1; i += 2) {
            payload.put(extraPayload[i], extraPayload[i + 1]);
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