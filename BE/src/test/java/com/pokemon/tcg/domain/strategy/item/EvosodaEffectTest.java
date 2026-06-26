package com.pokemon.tcg.domain.strategy.item;

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
class EvosodaEffectTest {

    @Mock
    private CardLookupPort cardLookupPort;

    private EvosodaEffect effect;

    // IDs reales del set XY1
    private static final String FROAKIE_ID   = "xy1-39";
    private static final String FROGADIER_ID = "xy1-40";
    private static final String EVOSODA_ID   = "xy1-116";
    private static final String PLAYER_1     = "p1";

    @BeforeEach
    void setUp() {
        effect = new EvosodaEffect(cardLookupPort);
    }

    @Test
    void canApply_shouldSucceed_whenEvolutionPathIsValid() {
        PlayerState ps = playerStateWithPokemonInPlay(FROAKIE_ID, "inst-1");
        ps.setDeck(new ArrayList<>(List.of(FROGADIER_ID)));
        BoardState state = BoardState.builder().player1State(ps).build();
        GameAction act = actionEvosoda("inst-1", FROGADIER_ID);

        // API retorna datos reales
        Card froakie  = Card.builder().id(FROAKIE_ID).name("Froakie").build();
        Card frogadier = Card.builder().id(FROGADIER_ID).name("Frogadier").evolvesFrom("Froakie").build();
        when(cardLookupPort.findCardById(FROAKIE_ID)).thenReturn(Optional.of(froakie));
        when(cardLookupPort.findCardById(FROGADIER_ID)).thenReturn(Optional.of(frogadier));

        assertThat(effect.canApply(state, act).isValid()).isTrue();
    }

    @Test
    void canApply_shouldFail_whenEvolutionPathIsInvalid() {
        PlayerState ps = playerStateWithPokemonInPlay(FROAKIE_ID, "inst-1");
        ps.setDeck(new ArrayList<>(List.of("xy1-99"))); // Carta que no evoluciona de Froakie
        BoardState state = BoardState.builder().player1State(ps).build();
        GameAction act = actionEvosoda("inst-1", "xy1-99");

        when(cardLookupPort.findCardById(FROAKIE_ID))
                .thenReturn(Optional.of(Card.builder().name("Froakie").build()));
        when(cardLookupPort.findCardById("xy1-99"))
                .thenReturn(Optional.of(Card.builder().name("Random").evolvesFrom("Pikachu").build()));

        ValidationResult result = effect.canApply(state, act);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("Invalid evolution path");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private GameAction actionEvosoda(String instanceId, String evolutionId) {
        return GameAction.builder()
                .type(GameActionType.PLAY_TRAINER)
                .playerId(PLAYER_1)
                .payload(Map.of(
                        "cardId", EVOSODA_ID,
                        "targetPokemonInstanceId", instanceId,
                        "evolutionCardId", evolutionId))
                .build();
    }

    private PlayerState playerStateWithPokemonInPlay(String cardId, String instanceId) {
        return PlayerState.builder()
                .playerId(PLAYER_1)
                .activePokemon(ActivePokemon.builder()
                        .cardId(cardId)
                        .instanceId(instanceId)
                        .build())
                .build();
    }
}
