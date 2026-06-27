package com.pokemon.tcg.domain.strategy.trainer.item;

import com.pokemon.tcg.domain.model.card.Card;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.engine.CardLookupPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfessorsLetterEffectTest {

    @Mock
    private CardLookupPort cardLookupPort;

    private ProfessorsLetterEffect effect;

    // Basic Energy card IDs used across tests
    private static final String GRASS_ENERGY    = "xy1-132";
    private static final String FIRE_ENERGY     = "xy1-133";
    private static final String NON_ENERGY_CARD = "xy1-3";

    @BeforeEach
    void setUp() {
        effect = new ProfessorsLetterEffect(cardLookupPort);
    }

    // ── canApply ───────────────────────────────────────────────────────────────

    @Test
    void canApply_shouldFail_whenNoEnergyIdsProvided() {
        PlayerState ps = playerState(PLAYER_1, List.of(), List.of(GRASS_ENERGY));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = actionWithEnergyIds(List.of());

        ValidationResult result = effect.canApply(state, act);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage())
                .isEqualTo("No Basic Energy cards specified for Professor's Letter.");
    }

    @Test
    void canApply_shouldFail_whenMoreThan2IdsProvided() {
        PlayerState ps = playerState(PLAYER_1, List.of(),
                List.of(GRASS_ENERGY, FIRE_ENERGY, "xy1-134"));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = actionWithEnergyIds(List.of(GRASS_ENERGY, FIRE_ENERGY, "xy1-134"));

        ValidationResult result = effect.canApply(state, act);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage())
                .isEqualTo("You may search for at most 2 Basic Energy cards.");
    }

    @Test
    void canApply_shouldFail_whenCardNotInDeck() {
        PlayerState ps = playerState(PLAYER_1, List.of(), List.of(FIRE_ENERGY));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = actionWithEnergyIds(List.of(GRASS_ENERGY)); // not in deck

        ValidationResult result = effect.canApply(state, act);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("is not in your deck");
    }

    @Test
    void canApply_shouldFail_whenCardIsNotBasicEnergy() {
        PlayerState ps = playerState(PLAYER_1, List.of(), List.of(NON_ENERGY_CARD));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = actionWithEnergyIds(List.of(NON_ENERGY_CARD));

        when(cardLookupPort.findCardById(NON_ENERGY_CARD))
                .thenReturn(Optional.of(cardWithIsBasicEnergy(NON_ENERGY_CARD, false)));

        ValidationResult result = effect.canApply(state, act);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("is not a Basic Energy");
    }

    @Test
    void canApply_shouldSucceed_whenOneValidBasicEnergyChosen() {
        PlayerState ps = playerState(PLAYER_1, List.of(), List.of(GRASS_ENERGY));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = actionWithEnergyIds(List.of(GRASS_ENERGY));

        when(cardLookupPort.findCardById(GRASS_ENERGY))
                .thenReturn(Optional.of(cardWithIsBasicEnergy(GRASS_ENERGY, true)));

        assertThat(effect.canApply(state, act).isValid()).isTrue();
    }

    @Test
    void canApply_shouldSucceed_whenTwoValidBasicEnergiesChosen() {
        PlayerState ps = playerState(PLAYER_1, List.of(), List.of(GRASS_ENERGY, FIRE_ENERGY));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = actionWithEnergyIds(List.of(GRASS_ENERGY, FIRE_ENERGY));

        when(cardLookupPort.findCardById(GRASS_ENERGY))
                .thenReturn(Optional.of(cardWithIsBasicEnergy(GRASS_ENERGY, true)));
        when(cardLookupPort.findCardById(FIRE_ENERGY))
                .thenReturn(Optional.of(cardWithIsBasicEnergy(FIRE_ENERGY, true)));

        assertThat(effect.canApply(state, act).isValid()).isTrue();
    }

    // ── apply ──────────────────────────────────────────────────────────────────

    @Test
    void apply_shouldMoveChosenEnergiesFromDeckToHand() {
        PlayerState ps = playerState(PLAYER_1, List.of(),
                new ArrayList<>(List.of(GRASS_ENERGY, FIRE_ENERGY, "xy1-3")));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = actionWithEnergyIds(List.of(GRASS_ENERGY, FIRE_ENERGY));

        effect.apply(state, act);

        assertThat(ps.getHand()).containsExactlyInAnyOrder(GRASS_ENERGY, FIRE_ENERGY);
        assertThat(ps.getDeck()).containsExactly("xy1-3");
    }

    @Test
    void apply_shouldMoveOnlyOneEnergy_whenOnlyOneChosen() {
        PlayerState ps = playerState(PLAYER_1, List.of(),
                new ArrayList<>(List.of(GRASS_ENERGY, FIRE_ENERGY)));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = actionWithEnergyIds(List.of(GRASS_ENERGY));

        effect.apply(state, act);

        assertThat(ps.getHand()).containsExactly(GRASS_ENERGY);
        assertThat(ps.getDeck()).containsExactly(FIRE_ENERGY);
    }

    @Test
    void apply_shouldEmitTrainerPlayedEvent_withRevealedCardIds() {
        PlayerState ps = playerState(PLAYER_1, List.of(),
                new ArrayList<>(List.of(GRASS_ENERGY, FIRE_ENERGY)));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = actionWithEnergyIds(List.of(GRASS_ENERGY, FIRE_ENERGY));

        EngineResult result = effect.apply(state, act);

        assertThat(result.events()).hasSize(1);
        GameEvent event = result.events().get(0);
        assertThat(event.getType()).isEqualTo(GameEventType.TRAINER_PLAYED);
        assertThat(event.getData()).containsEntry("cardId", "xy1-123");
        assertThat(event.getData()).containsKey("revealedCards");

        @SuppressWarnings("unchecked")
        List<String> revealed = (List<String>) event.getData().get("revealedCards");
        assertThat(revealed).containsExactlyInAnyOrder(GRASS_ENERGY, FIRE_ENERGY);
    }

    // ── Test helpers ───────────────────────────────────────────────────────────

    /**
     * Builds a PLAY_TRAINER action with the given list of energy card IDs in the payload.
     */
    private GameAction actionWithEnergyIds(List<String> energyCardIds) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("cardId", "xy1-123");
        payload.put("energyCardIds", energyCardIds);
        return GameAction.builder()
                .type(GameActionType.PLAY_TRAINER)
                .playerId(PLAYER_1)
                .payload(payload)
                .build();
    }

    /**
     * Builds a minimal Card stub with the given ID and isBasicEnergy flag.
     */
    private Card cardWithIsBasicEnergy(String id, boolean isBasicEnergy) {
        return Card.builder()
                .id(id)
                .basicEnergy(isBasicEnergy)
                .build();
    }
}
