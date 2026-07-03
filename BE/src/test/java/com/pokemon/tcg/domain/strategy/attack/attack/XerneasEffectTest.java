package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.Card;
import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.engine.CardLookupPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class XerneasEffectTest {

    private CardLookupPort cardLookupPort;
    private XerneasEffect  effect;

    private static final String FAIRY_ENERGY_1 = "xy1-134";
    private static final String FAIRY_ENERGY_2 = "xy1-134b";
    private static final String OTHER_ENERGY   = "xy1-131";

    @BeforeEach
    void setUp() {
        cardLookupPort = mock(CardLookupPort.class);
        effect = new XerneasEffect(cardLookupPort);

        Card fairyCard1 = mock(Card.class);
        when(fairyCard1.getTypes()).thenReturn(List.of(EnergyType.FAIRY.name()));
        when(cardLookupPort.findCardById(FAIRY_ENERGY_1)).thenReturn(Optional.of(fairyCard1));

        Card fairyCard2 = mock(Card.class);
        when(fairyCard2.getTypes()).thenReturn(List.of(EnergyType.FAIRY.name()));
        when(cardLookupPort.findCardById(FAIRY_ENERGY_2)).thenReturn(Optional.of(fairyCard2));

        Card otherCard = mock(Card.class);
        when(otherCard.getTypes()).thenReturn(List.of(EnergyType.WATER.name()));
        when(cardLookupPort.findCardById(OTHER_ENERGY)).thenReturn(Optional.of(otherCard));
    }

    @Test
    void shouldSupportBothAttacks() {
        assertThat(effect.getSupportedAttacks())
                .containsExactly("xerneas|geomancy", "xerneas|rainbow spear");
    }

    // ─── Geomancy ─────────────────────────────────────────────────────────────

    @Test
    void geomancy_shouldAttachFairyEnergyToEachChosenBenchPokemon() {
        AttackContext ctx = buildGeomancyContext(
                List.of(FAIRY_ENERGY_1, FAIRY_ENERGY_2, OTHER_ENERGY),
                List.of("bench-1", "bench-2"));

        effect.apply(ctx);

        PlayerState attacker = ctx.getBoardState().getStateFor(PLAYER_1);
        BenchPokemon bench1 = attacker.getBench().stream()
                .filter(b -> b.getInstanceId().equals("bench-1")).findFirst().orElseThrow();
        BenchPokemon bench2 = attacker.getBench().stream()
                .filter(b -> b.getInstanceId().equals("bench-2")).findFirst().orElseThrow();

        assertThat(bench1.getAttachedEnergyIds()).hasSize(1);
        assertThat(bench2.getAttachedEnergyIds()).hasSize(1);
        List<String> allAttached = new ArrayList<>(bench1.getAttachedEnergyIds());
        allAttached.addAll(bench2.getAttachedEnergyIds());
        assertThat(allAttached).containsExactlyInAnyOrder(FAIRY_ENERGY_1, FAIRY_ENERGY_2);
    }

    @Test
    void geomancy_shouldRemoveAttachedEnergiesFromDeck() {
        AttackContext ctx = buildGeomancyContext(
                List.of(FAIRY_ENERGY_1, FAIRY_ENERGY_2, OTHER_ENERGY),
                List.of("bench-1", "bench-2"));

        effect.apply(ctx);

        List<String> deck = ctx.getBoardState().getStateFor(PLAYER_1).getDeck();
        assertThat(deck).containsExactly(OTHER_ENERGY);
    }

    @Test
    void geomancy_shouldOnlyAttachToOneTarget_whenOnlyOneFairyEnergyInDeck() {
        AttackContext ctx = buildGeomancyContext(
                List.of(FAIRY_ENERGY_1, OTHER_ENERGY),
                List.of("bench-1", "bench-2"));

        effect.apply(ctx);

        PlayerState attacker = ctx.getBoardState().getStateFor(PLAYER_1);
        BenchPokemon bench1 = attacker.getBench().stream()
                .filter(b -> b.getInstanceId().equals("bench-1")).findFirst().orElseThrow();
        BenchPokemon bench2 = attacker.getBench().stream()
                .filter(b -> b.getInstanceId().equals("bench-2")).findFirst().orElseThrow();

        int totalAttached = bench1.getAttachedEnergyIds().size() + bench2.getAttachedEnergyIds().size();
        assertThat(totalAttached).isEqualTo(1);
    }

    @Test
    void geomancy_shouldDoNothing_whenNoFairyEnergyInDeck() {
        AttackContext ctx = buildGeomancyContext(
                List.of(OTHER_ENERGY),
                List.of("bench-1", "bench-2"));

        effect.apply(ctx);

        PlayerState attacker = ctx.getBoardState().getStateFor(PLAYER_1);
        assertThat(attacker.getDeck()).containsExactly(OTHER_ENERGY);
        BenchPokemon bench1 = attacker.getBench().stream()
                .filter(b -> b.getInstanceId().equals("bench-1")).findFirst().orElseThrow();
        assertThat(bench1.getAttachedEnergyIds()).isEmpty();
    }

    @Test
    void geomancy_shouldDoNothing_whenNoTargetsSpecified() {
        AttackContext ctx = buildGeomancyContext(
                List.of(FAIRY_ENERGY_1, FAIRY_ENERGY_2), List.of());

        effect.apply(ctx);

        List<String> deck = ctx.getBoardState().getStateFor(PLAYER_1).getDeck();
        assertThat(deck).containsExactlyInAnyOrder(FAIRY_ENERGY_1, FAIRY_ENERGY_2);
    }

    @Test
    void geomancy_shouldIgnoreTargetsBeyondTwo() {
        AttackContext ctx = buildGeomancyContextWithThreeBench(
                List.of(FAIRY_ENERGY_1, FAIRY_ENERGY_2, "xy1-134c"),
                List.of("bench-1", "bench-2", "bench-3"));

        effect.apply(ctx);

        PlayerState attacker = ctx.getBoardState().getStateFor(PLAYER_1);
        BenchPokemon bench3 = attacker.getBench().stream()
                .filter(b -> b.getInstanceId().equals("bench-3")).findFirst().orElseThrow();
        assertThat(bench3.getAttachedEnergyIds()).isEmpty();
    }

    @Test
    void geomancy_shouldSkipNonExistentTarget() {
        AttackContext ctx = buildGeomancyContext(
                List.of(FAIRY_ENERGY_1, FAIRY_ENERGY_2),
                List.of("nonexistent-bench", "bench-1"));

        effect.apply(ctx);

        PlayerState attacker = ctx.getBoardState().getStateFor(PLAYER_1);
        BenchPokemon bench1 = attacker.getBench().stream()
                .filter(b -> b.getInstanceId().equals("bench-1")).findFirst().orElseThrow();
        assertThat(bench1.getAttachedEnergyIds()).hasSize(1);
    }

    // ─── Rainbow Spear ────────────────────────────────────────────────────────

    @Test
    void rainbowSpear_shouldDiscardRequestedEnergy_whenSpecified() {
        AttackContext ctx = buildRainbowSpearContext(
                List.of(FAIRY_ENERGY_1, OTHER_ENERGY), OTHER_ENERGY);

        effect.apply(ctx);

        ActivePokemon xerneas = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        assertThat(xerneas.getAttachedEnergyIds()).containsExactly(FAIRY_ENERGY_1);
        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDiscard()).contains(OTHER_ENERGY);
    }

    @Test
    void rainbowSpear_shouldDiscardFirstEnergy_whenNoneSpecified() {
        AttackContext ctx = buildRainbowSpearContext(
                List.of(FAIRY_ENERGY_1, OTHER_ENERGY), null);

        effect.apply(ctx);

        ActivePokemon xerneas = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        assertThat(xerneas.getAttachedEnergyIds()).containsExactly(OTHER_ENERGY);
        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDiscard()).contains(FAIRY_ENERGY_1);
    }

    @Test
    void rainbowSpear_shouldDiscardFirstEnergy_whenRequestedNotAttached() {
        AttackContext ctx = buildRainbowSpearContext(
                List.of(FAIRY_ENERGY_1), "xy1-999");

        effect.apply(ctx);

        ActivePokemon xerneas = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        assertThat(xerneas.getAttachedEnergyIds()).isEmpty();
        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDiscard()).contains(FAIRY_ENERGY_1);
    }

    @Test
    void rainbowSpear_shouldDoNothing_whenNoEnergyAttached() {
        AttackContext ctx = buildRainbowSpearContext(List.of(), null);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDiscard()).isEmpty();
    }

    @Test
    void rainbowSpear_shouldDiscardAnyEnergyType_notOnlyFairy() {
        AttackContext ctx = buildRainbowSpearContext(List.of(OTHER_ENERGY), null);

        effect.apply(ctx);

        ActivePokemon xerneas = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        assertThat(xerneas.getAttachedEnergyIds()).isEmpty();
        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDiscard()).contains(OTHER_ENERGY);
    }

    // ─── unknown attack ───────────────────────────────────────────────────────

    @Test
    void unknownAttack_shouldDoNothing() {
        AttackContext ctx = buildRainbowSpearContext(List.of(FAIRY_ENERGY_1), null);
        ctx.setAttackName("unknown");

        effect.apply(ctx);

        ActivePokemon xerneas = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        assertThat(xerneas.getAttachedEnergyIds()).containsExactly(FAIRY_ENERGY_1);
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private AttackContext buildRainbowSpearContext(List<String> xerneasEnergies,
                                                   String requestedEnergyId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("attackName", "rainbow spear");
        if (requestedEnergyId != null) payload.put("energyToDiscardId", requestedEnergyId);
        return buildContext("rainbow spear", xerneasEnergies, List.of(), payload);
    }

    private AttackContext buildGeomancyContext(List<String> deck, List<String> targetInstanceIds) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("attackName", "geomancy");
        payload.put("targetInstanceIds", targetInstanceIds);
        return buildContext("geomancy", List.of(), deck, payload);
    }

    private AttackContext buildGeomancyContextWithThreeBench(List<String> deck,
                                                             List<String> targetInstanceIds) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("attackName", "geomancy");
        payload.put("targetInstanceIds", targetInstanceIds);
        return buildContextWithBenchCount("geomancy", List.of(), deck, payload, 3);
    }

    private AttackContext buildContext(String attackName,
                                       List<String> xerneasEnergies,
                                       List<String> deck,
                                       Map<String, Object> payload) {
        return buildContextWithBenchCount(attackName, xerneasEnergies, deck, payload, 2);
    }

    private AttackContext buildContextWithBenchCount(String attackName,
                                                     List<String> xerneasEnergies,
                                                     List<String> deck,
                                                     Map<String, Object> payload,
                                                     int benchCount) {
        ActivePokemon xerneas = ActivePokemon.builder()
                .instanceId("xerneas-1")
                .cardId("xy1-96")
                .types(new ArrayList<>(List.of(EnergyType.FAIRY)))
                .attachedEnergyIds(new ArrayList<>(xerneasEnergies))
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
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();

        PlayerState attackerState = playerState(PLAYER_1, List.of(), new ArrayList<>(deck));
        attackerState.setActivePokemon(xerneas);
        attackerState.setDiscard(new ArrayList<>());

        List<BenchPokemon> bench = new ArrayList<>();
        for (int i = 1; i <= benchCount; i++) {
            bench.add(BenchPokemon.builder()
                    .instanceId("bench-" + i)
                    .cardId("xy1-2")
                    .attachedEnergyIds(new ArrayList<>())
                    .evolutionStack(new ArrayList<>())
                    .damageCounters(0)
                    .build());
        }
        attackerState.setBench(bench);

        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(defender);

        BoardState state = boardState(attackerState, defenderState);

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