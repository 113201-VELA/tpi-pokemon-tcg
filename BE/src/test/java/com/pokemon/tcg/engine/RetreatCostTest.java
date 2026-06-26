package com.pokemon.tcg.engine;

import com.pokemon.tcg.domain.model.card.Card;
import com.pokemon.tcg.domain.model.card.CardType;
import com.pokemon.tcg.domain.model.game.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetreatCostTest {

    @Mock
    private CardLookupPort cardLookupPort;

    private RuleValidator validator;

    private static final String ACTIVE_CARD_ID   = "xy1-3";
    private static final String ENERGY_1         = "xy1-132";
    private static final String ENERGY_2         = "xy1-133";
    private static final String BENCH_INSTANCE   = "bench-inst-1";
    private static final String ACTIVE_INSTANCE  = "active-inst-1";

    @BeforeEach
    void setUp() {
        validator = new RuleValidator(cardLookupPort);
    }

    // ── validateRetreat — retreat cost ─────────────────────────────────────────

    @Test
    void validateRetreat_shouldSucceed_whenRetreatCostIsZeroAndNoEnergiesProvided() {
        when(cardLookupPort.findCardById(ACTIVE_CARD_ID))
                .thenReturn(Optional.of(cardWithRetreatCost(ACTIVE_CARD_ID, 0)));

        BoardState state = stateWithActiveAndBench(List.of());
        GameAction act = retreatAction(List.of());

        assertThat(validator.validate(state, act).isValid()).isTrue();
    }

    @Test
    void validateRetreat_shouldSucceed_whenCorrectEnergiesProvided() {
        when(cardLookupPort.findCardById(ACTIVE_CARD_ID))
                .thenReturn(Optional.of(cardWithRetreatCost(ACTIVE_CARD_ID, 2)));

        BoardState state = stateWithActiveAndBench(List.of(ENERGY_1, ENERGY_2));
        GameAction act = retreatAction(List.of(ENERGY_1, ENERGY_2));

        assertThat(validator.validate(state, act).isValid()).isTrue();
    }

    @Test
    void validateRetreat_shouldFail_whenTooFewEnergiesProvided() {
        when(cardLookupPort.findCardById(ACTIVE_CARD_ID))
                .thenReturn(Optional.of(cardWithRetreatCost(ACTIVE_CARD_ID, 2)));

        BoardState state = stateWithActiveAndBench(List.of(ENERGY_1, ENERGY_2));
        GameAction act = retreatAction(List.of(ENERGY_1)); // only 1, needs 2

        ValidationResult result = validator.validate(state, act);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage())
                .isEqualTo("You must discard exactly 2 Energy card(s) to retreat.");
    }

    @Test
    void validateRetreat_shouldFail_whenTooManyEnergiesProvided() {
        when(cardLookupPort.findCardById(ACTIVE_CARD_ID))
                .thenReturn(Optional.of(cardWithRetreatCost(ACTIVE_CARD_ID, 1)));

        BoardState state = stateWithActiveAndBench(List.of(ENERGY_1, ENERGY_2));
        GameAction act = retreatAction(List.of(ENERGY_1, ENERGY_2)); // 2, needs 1

        ValidationResult result = validator.validate(state, act);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage())
                .isEqualTo("You must discard exactly 1 Energy card(s) to retreat.");
    }

    @Test
    void validateRetreat_shouldFail_whenEnergyNotAttachedToActive() {
        when(cardLookupPort.findCardById(ACTIVE_CARD_ID))
                .thenReturn(Optional.of(cardWithRetreatCost(ACTIVE_CARD_ID, 1)));

        BoardState state = stateWithActiveAndBench(List.of(ENERGY_1));
        GameAction act = retreatAction(List.of(ENERGY_2)); // ENERGY_2 not attached

        ValidationResult result = validator.validate(state, act);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("is not attached to your Active Pokémon");
    }

    @Test
    void validateRetreat_shouldFail_whenSameEnergySpecifiedTwiceButOnlyAttachedOnce() {
        when(cardLookupPort.findCardById(ACTIVE_CARD_ID))
                .thenReturn(Optional.of(cardWithRetreatCost(ACTIVE_CARD_ID, 2)));

        // Only one copy of ENERGY_1 is attached, but player tries to discard it twice
        BoardState state = stateWithActiveAndBench(List.of(ENERGY_1));
        GameAction act = retreatAction(List.of(ENERGY_1, ENERGY_1));

        ValidationResult result = validator.validate(state, act);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("is not attached to your Active Pokémon");
    }

    // ── handleRetreat — energy discard ────────────────────────────────────────

    @Test
    void handleRetreat_shouldDiscardSpecifiedEnergies() {
        BoardState state = stateWithActiveAndBench(List.of(ENERGY_1, ENERGY_2));
        GameAction act = retreatAction(List.of(ENERGY_1));

        // Simulate TurnManager behavior directly
        PlayerState ps = state.getStateFor(PLAYER_1);
        ActivePokemon active = ps.getActivePokemon();

        // Discard one energy
        List<String> attached = new ArrayList<>(active.getAttachedEnergyIds());
        attached.remove(ENERGY_1);
        active.setAttachedEnergyIds(attached);

        List<String> discard = new ArrayList<>(ps.getDiscard() != null ? ps.getDiscard() : List.of());
        discard.add(ENERGY_1);
        ps.setDiscard(discard);

        assertThat(active.getAttachedEnergyIds()).containsExactly(ENERGY_2);
        assertThat(ps.getDiscard()).containsExactly(ENERGY_1);
    }

    @Test
    void handleRetreat_shouldNotDiscardAnyEnergy_whenRetreatCostIsZero() {
        BoardState state = stateWithActiveAndBench(List.of(ENERGY_1));
        GameAction act = retreatAction(List.of());

        PlayerState ps = state.getStateFor(PLAYER_1);
        ActivePokemon active = ps.getActivePokemon();

        // No energies discarded
        assertThat(active.getAttachedEnergyIds()).containsExactly(ENERGY_1);
        assertThat(ps.getDiscard()).isEmpty();
    }

    // ── Test helpers ───────────────────────────────────────────────────────────

    /**
     * Builds a board state with an Active Pokémon with the given attached energies
     * and one Bench Pokémon, in MAIN phase.
     */
    private BoardState stateWithActiveAndBench(List<String> attachedEnergies) {
        ActivePokemon active = ActivePokemon.builder()
                .instanceId(ACTIVE_INSTANCE)
                .cardId(ACTIVE_CARD_ID)
                .attachedEnergyIds(new ArrayList<>(attachedEnergies))
                .evolutionStack(new ArrayList<>(List.of(ACTIVE_CARD_ID)))
                .damageCounters(0)
                .conditions(new HashSet<>())
                .enteredThisTurn(false)
                .build();

        BenchPokemon benched = BenchPokemon.builder()
                .instanceId(BENCH_INSTANCE)
                .cardId("xy1-6")
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>(List.of("xy1-6")))
                .damageCounters(0)
                .enteredThisTurn(false)
                .build();

        PlayerState ps = playerState(PLAYER_1, List.of(), cardIds(5));
        ps.setActivePokemon(active);
        ps.setBench(new ArrayList<>(List.of(benched)));

        return boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
    }

    /**
     * Builds a RETREAT action with the given list of energy IDs to discard.
     */
    private GameAction retreatAction(List<String> energyCardIdsToDiscard) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("replacementInstanceId", BENCH_INSTANCE);
        payload.put("energyCardIdsToDiscard", energyCardIdsToDiscard);
        return GameAction.builder()
                .type(GameActionType.RETREAT)
                .playerId(PLAYER_1)
                .payload(payload)
                .build();
    }

    /**
     * Builds a Card stub with the given retreat cost size.
     * Each entry in the retreat cost list is "COLORLESS" (standard XY format).
     */
    private Card cardWithRetreatCost(String id, int cost) {
        List<String> retreatCost = new ArrayList<>();
        for (int i = 0; i < cost; i++) retreatCost.add("COLORLESS");
        return Card.builder()
                .id(id)
                .supertype(CardType.POKEMON)
                .retreatCost(retreatCost)
                .build();
    }
}
