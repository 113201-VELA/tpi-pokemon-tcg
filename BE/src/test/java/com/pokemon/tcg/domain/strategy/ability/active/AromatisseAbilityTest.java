package com.pokemon.tcg.domain.strategy.ability.active;

import com.pokemon.tcg.domain.model.card.Card;
import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.engine.CardLookupPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AromatisseAbilityTest {

    private CardLookupPort    cardLookupPort;
    private AromatisseAbility ability;

    private static final String FAIRY_ENERGY = "xy1-134";
    private static final String WATER_ENERGY = "xy1-131";

    @BeforeEach
    void setUp() {
        cardLookupPort = mock(CardLookupPort.class);
        ability = new AromatisseAbility(cardLookupPort);

        Card fairyCard = mock(Card.class);
        when(fairyCard.getTypes()).thenReturn(List.of(EnergyType.FAIRY.name()));
        when(cardLookupPort.findCardById(FAIRY_ENERGY)).thenReturn(Optional.of(fairyCard));

        Card waterCard = mock(Card.class);
        when(waterCard.getTypes()).thenReturn(List.of(EnergyType.WATER.name()));
        when(cardLookupPort.findCardById(WATER_ENERGY)).thenReturn(Optional.of(waterCard));
    }

    @Test
    void shouldHaveCorrectIdentifier() {
        assertThat(ability.getIdentifier()).isEqualTo("aromatisse|fairy transfer");
    }

    // ─── canApply ─────────────────────────────────────────────────────────────

    @Test
    void canApply_shouldFail_whenNoSourceInstanceId() {
        BoardState state = buildState(List.of(FAIRY_ENERGY), List.of());
        GameAction action = buildAction(null, "aromatisse-1", FAIRY_ENERGY);

        assertThat(ability.canApply(state, action).isValid()).isFalse();
        assertThat(ability.canApply(state, action).getErrorMessage())
                .isEqualTo("You must specify the source Pokémon.");
    }

    @Test
    void canApply_shouldFail_whenNoTargetInstanceId() {
        BoardState state = buildState(List.of(FAIRY_ENERGY), List.of());
        GameAction action = buildAction("aromatisse-1", null, FAIRY_ENERGY);

        assertThat(ability.canApply(state, action).isValid()).isFalse();
        assertThat(ability.canApply(state, action).getErrorMessage())
                .isEqualTo("You must specify the target Pokémon.");
    }

    @Test
    void canApply_shouldFail_whenNoEnergyCardId() {
        BoardState state = buildState(List.of(FAIRY_ENERGY), List.of());
        GameAction action = buildAction("aromatisse-1", "bench-1", null);

        assertThat(ability.canApply(state, action).isValid()).isFalse();
        assertThat(ability.canApply(state, action).getErrorMessage())
                .isEqualTo("You must specify a Fairy Energy card to move.");
    }

    @Test
    void canApply_shouldFail_whenSourceNotInPlay() {
        BoardState state = buildState(List.of(FAIRY_ENERGY), List.of());
        GameAction action = buildAction("nonexistent", "bench-1", FAIRY_ENERGY);

        assertThat(ability.canApply(state, action).isValid()).isFalse();
        assertThat(ability.canApply(state, action).getErrorMessage())
                .isEqualTo("The source Pokémon is not in play.");
    }

    @Test
    void canApply_shouldFail_whenTargetNotInPlay() {
        BoardState state = buildState(List.of(FAIRY_ENERGY), List.of());
        GameAction action = buildAction("aromatisse-1", "nonexistent", FAIRY_ENERGY);

        assertThat(ability.canApply(state, action).isValid()).isFalse();
        assertThat(ability.canApply(state, action).getErrorMessage())
                .isEqualTo("The target Pokémon is not in play.");
    }

    @Test
    void canApply_shouldFail_whenEnergyNotAttachedToSource() {
        BoardState state = buildState(List.of(), List.of()); // Aromatisse has no energy
        GameAction action = buildAction("aromatisse-1", "bench-1", FAIRY_ENERGY);

        assertThat(ability.canApply(state, action).isValid()).isFalse();
        assertThat(ability.canApply(state, action).getErrorMessage())
                .isEqualTo("The specified Energy is not attached to the source Pokémon.");
    }

    @Test
    void canApply_shouldFail_whenEnergyIsNotFairy() {
        BoardState state = buildState(List.of(WATER_ENERGY), List.of());
        GameAction action = buildAction("aromatisse-1", "bench-1", WATER_ENERGY);

        assertThat(ability.canApply(state, action).isValid()).isFalse();
        assertThat(ability.canApply(state, action).getErrorMessage())
                .isEqualTo("You must move a Fairy Energy card.");
    }

    @Test
    void canApply_shouldSucceed_withValidPayload() {
        BoardState state = buildState(List.of(FAIRY_ENERGY), List.of());
        GameAction action = buildAction("aromatisse-1", "bench-1", FAIRY_ENERGY);

        assertThat(ability.canApply(state, action).isValid()).isTrue();
    }

    // ─── apply ────────────────────────────────────────────────────────────────

    @Test
    void apply_shouldMoveEnergy_fromActiveToBench() {
        BoardState state = buildState(List.of(FAIRY_ENERGY), List.of());
        GameAction action = buildAction("aromatisse-1", "bench-1", FAIRY_ENERGY);

        ability.apply(state, action);

        PlayerState ps = state.getStateFor(PLAYER_1);
        assertThat(ps.getActivePokemon().getAttachedEnergyIds()).doesNotContain(FAIRY_ENERGY);
        assertThat(ps.getBench().get(0).getAttachedEnergyIds()).contains(FAIRY_ENERGY);
    }

    @Test
    void apply_shouldMoveEnergy_fromBenchToActive() {
        BoardState state = buildState(List.of(), List.of(FAIRY_ENERGY));
        GameAction action = buildAction("bench-1", "aromatisse-1", FAIRY_ENERGY);

        ability.apply(state, action);

        PlayerState ps = state.getStateFor(PLAYER_1);
        assertThat(ps.getBench().get(0).getAttachedEnergyIds()).doesNotContain(FAIRY_ENERGY);
        assertThat(ps.getActivePokemon().getAttachedEnergyIds()).contains(FAIRY_ENERGY);
    }

    @Test
    void apply_shouldNotMarkAbilityAsUsed() {
        BoardState state = buildState(List.of(FAIRY_ENERGY), List.of());
        GameAction action = buildAction("aromatisse-1", "bench-1", FAIRY_ENERGY);

        ability.apply(state, action);

        assertThat(state.getTurnFlags().isAbilityUsed("aromatisse-1", "Fairy Transfer"))
                .isFalse();
    }

    @Test
    void apply_shouldAllowMultipleConsecutiveMoves_inSameTurn() {
        BoardState state = buildState(List.of(FAIRY_ENERGY), List.of());
        GameAction firstMove = buildAction("aromatisse-1", "bench-1", FAIRY_ENERGY);

        ability.apply(state, firstMove);

        // Second move: back from bench to active, still unrestricted
        GameAction secondMove = buildAction("bench-1", "aromatisse-1", FAIRY_ENERGY);
        assertThat(ability.canApply(state, secondMove).isValid()).isTrue();

        ability.apply(state, secondMove);

        PlayerState ps = state.getStateFor(PLAYER_1);
        assertThat(ps.getActivePokemon().getAttachedEnergyIds()).contains(FAIRY_ENERGY);
        assertThat(ps.getBench().get(0).getAttachedEnergyIds()).doesNotContain(FAIRY_ENERGY);
    }

    @Test
    void apply_shouldNotAffectOpponent() {
        BoardState state = buildState(List.of(FAIRY_ENERGY), List.of());
        GameAction action = buildAction("aromatisse-1", "bench-1", FAIRY_ENERGY);

        ability.apply(state, action);

        PlayerState opponent = state.getStateFor(PLAYER_2);
        assertThat(opponent.getActivePokemon().getAttachedEnergyIds()).isEmpty();
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private BoardState buildState(List<String> activeEnergies, List<String> benchEnergies) {
        ActivePokemon aromatisse = ActivePokemon.builder()
                .instanceId("aromatisse-1")
                .cardId("xy1-93")
                .types(new ArrayList<>(List.of(EnergyType.FAIRY)))
                .attachedEnergyIds(new ArrayList<>(activeEnergies))
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();

        BenchPokemon bench = BenchPokemon.builder()
                .instanceId("bench-1")
                .cardId("xy1-2")
                .attachedEnergyIds(new ArrayList<>(benchEnergies))
                .evolutionStack(new ArrayList<>())
                .damageCounters(0)
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

        PlayerState attackerState = playerState(PLAYER_1, List.of(), cardIds(5));
        attackerState.setActivePokemon(aromatisse);
        attackerState.setBench(new ArrayList<>(List.of(bench)));

        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(defender);
        defenderState.setBench(new ArrayList<>());

        return boardState(attackerState, defenderState);
    }

    private GameAction buildAction(String sourceInstanceId, String targetInstanceId,
                                   String energyCardId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("instanceId", "aromatisse-1");
        payload.put("abilityName", "Fairy Transfer");
        if (sourceInstanceId != null) payload.put("sourceInstanceId", sourceInstanceId);
        if (targetInstanceId != null) payload.put("targetInstanceId", targetInstanceId);
        if (energyCardId != null) payload.put("energyCardId", energyCardId);

        return GameAction.builder()
                .type(GameActionType.USE_ABILITY)
                .playerId(PLAYER_1)
                .payload(payload)
                .build();
    }
}