package com.pokemon.tcg.domain.strategy.ability.active;

import com.pokemon.tcg.domain.model.card.Card;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.engine.CardLookupPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InkayAbilityTest {

    @Mock private CardLookupPort cardLookupPort;

    private InkayAbility ability;

    private static final String PLAYER_1 = "player-1";
    private static final String INKAY_INSTANCE = "inkay-1";
    private static final String MALAMAR_CARD_ID = "xy1-75";

    @BeforeEach
    void setUp() {
        ability = new InkayAbility(cardLookupPort);
    }

    @Test
    void canApply_shouldSucceed_whenConfusedAndValidEvolution() {
        when(cardLookupPort.findCardById(MALAMAR_CARD_ID))
                .thenReturn(Optional.of(Card.builder()
                        .id(MALAMAR_CARD_ID)
                        .name("Malamar")
                        .evolvesFrom("Inkay")
                        .build()));

        BoardState state = buildState(true, List.of(MALAMAR_CARD_ID));
        GameAction action = buildAction(MALAMAR_CARD_ID);

        ValidationResult result = ability.canApply(state, action);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void canApply_shouldFail_whenNotConfused() {
        BoardState state = buildState(false, List.of(MALAMAR_CARD_ID));
        GameAction action = buildAction(MALAMAR_CARD_ID);

        ValidationResult result = ability.canApply(state, action);

        assertThat(result.isValid()).isFalse();
    }

    @Test
    void canApply_shouldFail_whenCardNotInDeck() {
        BoardState state = buildState(true, List.of());
        GameAction action = buildAction(MALAMAR_CARD_ID);

        ValidationResult result = ability.canApply(state, action);

        assertThat(result.isValid()).isFalse();
    }

    @Test
    void canApply_shouldFail_whenCardDoesNotEvolveFromInkay() {
        when(cardLookupPort.findCardById(MALAMAR_CARD_ID))
                .thenReturn(Optional.of(Card.builder()
                        .id(MALAMAR_CARD_ID)
                        .name("SomethingElse")
                        .evolvesFrom("NotInkay")
                        .build()));

        BoardState state = buildState(true, List.of(MALAMAR_CARD_ID));
        GameAction action = buildAction(MALAMAR_CARD_ID);

        ValidationResult result = ability.canApply(state, action);

        assertThat(result.isValid()).isFalse();
    }

    @Test
    void apply_shouldEvolveInkay_andRemoveCardFromDeck() {
        BoardState state = buildState(true, new ArrayList<>(List.of(MALAMAR_CARD_ID, "xy1-132")));
        GameAction action = buildAction(MALAMAR_CARD_ID);

        BoardState result = ability.apply(state, action);

        ActivePokemon inkay = result.getStateFor(PLAYER_1).getActivePokemon();
        assertThat(inkay.getCardId()).isEqualTo(MALAMAR_CARD_ID);
        assertThat(inkay.getConditions()).isEmpty();
        assertThat(inkay.getEvolutionStack()).contains(MALAMAR_CARD_ID);
        assertThat(result.getStateFor(PLAYER_1).getDeck()).doesNotContain(MALAMAR_CARD_ID);
    }

    private BoardState buildState(boolean confused, List<String> deck) {
        Set<SpecialCondition> conditions = new HashSet<>();
        if (confused) conditions.add(SpecialCondition.CONFUSED);

        ActivePokemon inkay = ActivePokemon.builder()
                .instanceId(INKAY_INSTANCE)
                .cardId("xy1-74")
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>(List.of("xy1-74")))
                .damageCounters(0)
                .conditions(conditions)
                .activeEffects(new ArrayList<>())
                .build();

        PlayerState ps1 = PlayerState.builder()
                .playerId(PLAYER_1)
                .hand(new ArrayList<>())
                .deck(new ArrayList<>(deck))
                .discard(new ArrayList<>())
                .prizes(new ArrayList<>())
                .bench(new ArrayList<>())
                .activePokemon(inkay)
                .build();

        PlayerState ps2 = PlayerState.builder()
                .playerId("player-2")
                .hand(new ArrayList<>())
                .deck(new ArrayList<>())
                .discard(new ArrayList<>())
                .prizes(new ArrayList<>())
                .bench(new ArrayList<>())
                .build();

        return BoardState.builder()
                .gameId("game-1")
                .player1State(ps1)
                .player2State(ps2)
                .turnFlags(TurnFlags.fresh())
                .build();
    }

    private GameAction buildAction(String cardId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("instanceId", INKAY_INSTANCE);
        payload.put("abilityName", "Upside-Down Evolution");
        payload.put("cardId", cardId);

        return GameAction.builder()
                .type(GameActionType.USE_ABILITY)
                .playerId(PLAYER_1)
                .payload(payload)
                .build();
    }
}