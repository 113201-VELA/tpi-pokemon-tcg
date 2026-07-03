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

class Wigglytuff89EffectTest {

    private CardLookupPort      cardLookupPort;
    private Wigglytuff89Effect  effect;

    private static final String BASIC_ENERGY_1 = "xy1-131"; // water energy
    private static final String BASIC_ENERGY_2 = "xy1-135"; // lightning energy
    private static final String NON_ENERGY_CARD = "xy1-1";  // a Pokémon card

    @BeforeEach
    void setUp() {
        cardLookupPort = mock(CardLookupPort.class);
        effect = new Wigglytuff89Effect(cardLookupPort);

        Card basicEnergy1 = mock(Card.class);
        when(basicEnergy1.isBasicEnergy()).thenReturn(true);
        when(cardLookupPort.findCardById(BASIC_ENERGY_1)).thenReturn(Optional.of(basicEnergy1));

        Card basicEnergy2 = mock(Card.class);
        when(basicEnergy2.isBasicEnergy()).thenReturn(true);
        when(cardLookupPort.findCardById(BASIC_ENERGY_2)).thenReturn(Optional.of(basicEnergy2));

        Card nonEnergy = mock(Card.class);
        when(nonEnergy.isBasicEnergy()).thenReturn(false);
        when(cardLookupPort.findCardById(NON_ENERGY_CARD)).thenReturn(Optional.of(nonEnergy));
    }

    @Test
    void shouldSupportBothAttacks() {
        assertThat(effect.getSupportedAttacks())
                .containsExactly("wigglytuff|gather energy", "wigglytuff|hocus pinkus");
    }

    // ─── Gather Energy ────────────────────────────────────────────────────────

    @Test
    void gatherEnergy_shouldAttachToOwnActive_whenNoTargetSpecified() {
        AttackContext ctx = buildGatherEnergyContext(
                List.of(BASIC_ENERGY_1, NON_ENERGY_CARD), null, null, true);

        effect.apply(ctx);

        ActivePokemon wigglytuff = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        assertThat(wigglytuff.getAttachedEnergyIds()).contains(BASIC_ENERGY_1);
    }

    @Test
    void gatherEnergy_shouldAttachToSpecifiedBenchTarget() {
        AttackContext ctx = buildGatherEnergyContext(
                List.of(BASIC_ENERGY_1), null, "bench-1", true);

        effect.apply(ctx);

        BenchPokemon bench = ctx.getBoardState().getStateFor(PLAYER_1).getBench().get(0);
        assertThat(bench.getAttachedEnergyIds()).contains(BASIC_ENERGY_1);

        ActivePokemon wigglytuff = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        assertThat(wigglytuff.getAttachedEnergyIds()).isEmpty();
    }

    @Test
    void gatherEnergy_shouldPreferRequestedEnergy_whenSpecified() {
        AttackContext ctx = buildGatherEnergyContext(
                List.of(BASIC_ENERGY_1, BASIC_ENERGY_2), BASIC_ENERGY_2, null, true);

        effect.apply(ctx);

        ActivePokemon wigglytuff = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        assertThat(wigglytuff.getAttachedEnergyIds()).containsExactly(BASIC_ENERGY_2);

        List<String> deck = ctx.getBoardState().getStateFor(PLAYER_1).getDeck();
        assertThat(deck).containsExactly(BASIC_ENERGY_1); // remaining card, shuffled deck of 1
    }

    @Test
    void gatherEnergy_shouldFallBackToFirstBasicEnergy_whenRequestedNotBasicEnergy() {
        AttackContext ctx = buildGatherEnergyContext(
                List.of(BASIC_ENERGY_1, NON_ENERGY_CARD), NON_ENERGY_CARD, null, true);

        effect.apply(ctx);

        ActivePokemon wigglytuff = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        assertThat(wigglytuff.getAttachedEnergyIds()).containsExactly(BASIC_ENERGY_1);
    }

    @Test
    void gatherEnergy_shouldRemoveChosenEnergyFromDeck() {
        AttackContext ctx = buildGatherEnergyContext(
                List.of(BASIC_ENERGY_1, NON_ENERGY_CARD), null, null, true);

        effect.apply(ctx);

        List<String> deck = ctx.getBoardState().getStateFor(PLAYER_1).getDeck();
        assertThat(deck).doesNotContain(BASIC_ENERGY_1);
        assertThat(deck).hasSize(1);
    }

    @Test
    void gatherEnergy_shouldDoNothing_whenNoBasicEnergyInDeck() {
        AttackContext ctx = buildGatherEnergyContext(
                List.of(NON_ENERGY_CARD), null, null, true);

        effect.apply(ctx);

        ActivePokemon wigglytuff = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        assertThat(wigglytuff.getAttachedEnergyIds()).isEmpty();
        List<String> deck = ctx.getBoardState().getStateFor(PLAYER_1).getDeck();
        assertThat(deck).containsExactly(NON_ENERGY_CARD);
    }

    @Test
    void gatherEnergy_shouldFallBackToActive_whenTargetBenchNotFound() {
        AttackContext ctx = buildGatherEnergyContext(
                List.of(BASIC_ENERGY_1), null, "nonexistent-bench", true);

        effect.apply(ctx);

        ActivePokemon wigglytuff = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        assertThat(wigglytuff.getAttachedEnergyIds()).containsExactly(BASIC_ENERGY_1);
    }

    // ─── Hocus Pinkus ─────────────────────────────────────────────────────────

    @Test
    void hocusPinkus_shouldAddCantAttackToDefender() {
        AttackContext ctx = buildHocusPinkusContext(new ArrayList<>());

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState().getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getActiveEffects()).contains(PokemonEffect.CANT_ATTACK);
    }

    @Test
    void hocusPinkus_shouldNotDuplicateCantAttack_whenAlreadyPresent() {
        AttackContext ctx = buildHocusPinkusContext(
                new ArrayList<>(List.of(PokemonEffect.CANT_ATTACK)));

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState().getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getActiveEffects())
                .filteredOn(e -> e == PokemonEffect.CANT_ATTACK)
                .hasSize(1);
    }

    @Test
    void hocusPinkus_shouldPreserveOtherActiveEffects() {
        AttackContext ctx = buildHocusPinkusContext(
                new ArrayList<>(List.of(PokemonEffect.HARDEN)));

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState().getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getActiveEffects())
                .contains(PokemonEffect.HARDEN, PokemonEffect.CANT_ATTACK);
    }

    // ─── unknown attack ───────────────────────────────────────────────────────

    @Test
    void unknownAttack_shouldDoNothing() {
        AttackContext ctx = buildGatherEnergyContext(
                List.of(BASIC_ENERGY_1), null, null, true);
        ctx.setAttackName("unknown");

        effect.apply(ctx);

        ActivePokemon wigglytuff = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        assertThat(wigglytuff.getAttachedEnergyIds()).isEmpty();
        List<String> deck = ctx.getBoardState().getStateFor(PLAYER_1).getDeck();
        assertThat(deck).containsExactly(BASIC_ENERGY_1);
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private AttackContext buildGatherEnergyContext(List<String> deck,
                                                   String requestedEnergyId,
                                                   String targetInstanceId,
                                                   boolean hasBench) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("attackName", "gather energy");
        if (requestedEnergyId != null) payload.put("energyCardId", requestedEnergyId);
        if (targetInstanceId != null) payload.put("targetInstanceId", targetInstanceId);

        return buildContext("gather energy", deck, payload, hasBench, new ArrayList<>());
    }

    private AttackContext buildHocusPinkusContext(List<PokemonEffect> defenderEffects) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("attackName", "hocus pinkus");

        return buildContext("hocus pinkus", List.of(), payload, false, defenderEffects);
    }

    private AttackContext buildContext(String attackName,
                                       List<String> deck,
                                       Map<String, Object> payload,
                                       boolean hasBench,
                                       List<PokemonEffect> defenderEffects) {
        ActivePokemon wigglytuff = ActivePokemon.builder()
                .instanceId("wigglytuff-1")
                .cardId("xy1-89")
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
                .conditions(new HashSet<>())
                .activeEffects(defenderEffects)
                .build();

        PlayerState attackerState = playerState(PLAYER_1, List.of(), new ArrayList<>(deck));
        attackerState.setActivePokemon(wigglytuff);

        if (hasBench) {
            BenchPokemon bench = BenchPokemon.builder()
                    .instanceId("bench-1")
                    .cardId("xy1-2")
                    .attachedEnergyIds(new ArrayList<>())
                    .evolutionStack(new ArrayList<>())
                    .damageCounters(0)
                    .build();
            attackerState.setBench(new ArrayList<>(List.of(bench)));
        } else {
            attackerState.setBench(new ArrayList<>());
        }

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