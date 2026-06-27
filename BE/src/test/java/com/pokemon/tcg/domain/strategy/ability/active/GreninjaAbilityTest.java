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

class GrenijaWaterShurikenAbilityTest {

    private CardLookupPort cardLookupPort;
    private GreninjaAbility ability;

    private static final String WATER_ENERGY = "xy1-131";
    private static final String FIRE_ENERGY  = "xy1-133";

    @BeforeEach
    void setUp() {
        cardLookupPort = mock(CardLookupPort.class);
        ability = new GreninjaAbility(cardLookupPort);

        Card waterCard = mock(Card.class);
        when(waterCard.getTypes()).thenReturn(List.of(EnergyType.WATER.name()));
        when(cardLookupPort.findCardById(WATER_ENERGY)).thenReturn(Optional.of(waterCard));

        Card fireCard = mock(Card.class);
        when(fireCard.getTypes()).thenReturn(List.of(EnergyType.FIRE.name()));
        when(cardLookupPort.findCardById(FIRE_ENERGY)).thenReturn(Optional.of(fireCard));
    }

    @Test
    void shouldHaveCorrectIdentifier() {
        assertThat(ability.getIdentifier()).isEqualTo("greninja|water shuriken");
    }

    // ─── canApply ─────────────────────────────────────────────────────────────

    @Test
    void canApply_shouldSucceed_withValidPayload() {
        BoardState state = buildState(List.of(WATER_ENERGY));
        GameAction action = buildAction(WATER_ENERGY, "def-1");

        assertThat(ability.canApply(state, action).isValid()).isTrue();
    }

    @Test
    void canApply_shouldFail_whenNoEnergyCardId() {
        BoardState state = buildState(List.of(WATER_ENERGY));
        GameAction action = buildAction(null, "def-1");

        assertThat(ability.canApply(state, action).isValid()).isFalse();
    }

    @Test
    void canApply_shouldFail_whenEnergyNotInHand() {
        BoardState state = buildState(List.of()); // empty hand
        GameAction action = buildAction(WATER_ENERGY, "def-1");

        assertThat(ability.canApply(state, action).isValid()).isFalse();
    }

    @Test
    void canApply_shouldFail_whenEnergyIsNotWater() {
        BoardState state = buildState(List.of(FIRE_ENERGY));
        GameAction action = buildAction(FIRE_ENERGY, "def-1");

        assertThat(ability.canApply(state, action).isValid()).isFalse();
    }

    @Test
    void canApply_shouldFail_whenNoTargetInstanceId() {
        BoardState state = buildState(List.of(WATER_ENERGY));
        GameAction action = buildAction(WATER_ENERGY, null);

        assertThat(ability.canApply(state, action).isValid()).isFalse();
    }

    @Test
    void canApply_shouldFail_whenTargetNotInPlay() {
        BoardState state = buildState(List.of(WATER_ENERGY));
        GameAction action = buildAction(WATER_ENERGY, "nonexistent");

        assertThat(ability.canApply(state, action).isValid()).isFalse();
    }

    // ─── apply ────────────────────────────────────────────────────────────────

    @Test
    void apply_shouldDiscardWaterEnergy_fromHand() {
        BoardState state = buildState(List.of(WATER_ENERGY));
        GameAction action = buildAction(WATER_ENERGY, "def-1");

        ability.apply(state, action);

        assertThat(state.getStateFor(PLAYER_1).getHand()).doesNotContain(WATER_ENERGY);
        assertThat(state.getStateFor(PLAYER_1).getDiscard()).contains(WATER_ENERGY);
    }

    @Test
    void apply_shouldAdd3DamageCounters_toOpponentActive() {
        BoardState state = buildState(List.of(WATER_ENERGY));
        GameAction action = buildAction(WATER_ENERGY, "def-1");

        ability.apply(state, action);

        assertThat(state.getStateFor(PLAYER_2).getActivePokemon().getDamageCounters())
                .isEqualTo(3);
    }

    @Test
    void apply_shouldAdd3DamageCounters_toBenchedPokemon() {
        BoardState state = buildState(List.of(WATER_ENERGY));

        // Add a bench Pokémon to opponent
        BenchPokemon bench = BenchPokemon.builder()
                .instanceId("bench-opp-1")
                .cardId("xy1-2")
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .damageCounters(0)
                .build();
        state.getStateFor(PLAYER_2).setBench(new ArrayList<>(List.of(bench)));

        GameAction action = buildAction(WATER_ENERGY, "bench-opp-1");

        ability.apply(state, action);

        assertThat(state.getStateFor(PLAYER_2).getBench().get(0).getDamageCounters())
                .isEqualTo(3);
        // Active should be untouched
        assertThat(state.getStateFor(PLAYER_2).getActivePokemon().getDamageCounters())
                .isEqualTo(0);
    }

    @Test
    void apply_shouldMarkAbilityAsUsed() {
        BoardState state = buildState(List.of(WATER_ENERGY));
        GameAction action = buildAction(WATER_ENERGY, "def-1");

        ability.apply(state, action);

        assertThat(state.getTurnFlags().isAbilityUsed("greninja-1", "Water Shuriken"))
                .isTrue();
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private BoardState buildState(List<String> attackerHand) {
        ActivePokemon greninja = ActivePokemon.builder()
                .instanceId("greninja-1")
                .cardId("xy1-41")
                .types(new ArrayList<>(List.of(EnergyType.WATER)))
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
                .activeEffects(new ArrayList<>())
                .build();

        PlayerState attackerState = playerState(PLAYER_1, List.of(), cardIds(5));
        attackerState.setActivePokemon(greninja);
        attackerState.setHand(new ArrayList<>(attackerHand));
        attackerState.setDiscard(new ArrayList<>());

        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(defender);
        defenderState.setBench(new ArrayList<>());

        return boardState(attackerState, defenderState);
    }

    private GameAction buildAction(String energyCardId, String targetInstanceId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("instanceId", "greninja-1");
        payload.put("abilityName", "Water Shuriken");
        if (energyCardId != null) payload.put("energyCardId", energyCardId);
        if (targetInstanceId != null) payload.put("targetInstanceId", targetInstanceId);

        return GameAction.builder()
                .type(GameActionType.USE_ABILITY)
                .playerId(PLAYER_1)
                .payload(payload)
                .build();
    }
}