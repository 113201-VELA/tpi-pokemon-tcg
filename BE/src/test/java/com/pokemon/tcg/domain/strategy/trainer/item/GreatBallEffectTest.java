package com.pokemon.tcg.domain.strategy.trainer.item;

import com.pokemon.tcg.domain.model.card.Card;
import com.pokemon.tcg.domain.model.card.CardType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.engine.CardLookupPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GreatBallEffectTest {

    @Mock
    private CardLookupPort cardLookupPort;

    private GreatBallEffect effect;

    private static final String POKEMON_1     = "xy1-1";
    private static final String POKEMON_2     = "xy1-4";
    private static final String NON_POKEMON_1 = "xy1-132";
    private static final String NON_POKEMON_2 = "xy1-133";

    @BeforeEach
    void setUp() {
        effect = new GreatBallEffect(cardLookupPort);
    }

    // ── canApply ───────────────────────────────────────────────────────────────

    @Test
    void canApply_shouldSucceed_whenPokemonInTop7() {
        List<String> deck = new ArrayList<>(List.of(
                POKEMON_1, NON_POKEMON_1, NON_POKEMON_2, "xy1-5", "xy1-6", "xy1-7", "xy1-8", "xy1-9"));
        PlayerState ps = playerState(PLAYER_1, List.of(), deck);
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-118");

        when(cardLookupPort.findCardById(POKEMON_1))
                .thenReturn(Optional.of(cardWithSupertype(POKEMON_1, CardType.POKEMON)));

        ValidationResult result = effect.canApply(state, act);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void canApply_shouldFail_whenDeckEmpty() {
        PlayerState ps = playerState(PLAYER_1, List.of(), List.of());
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-118");

        ValidationResult result = effect.canApply(state, act);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("empty");
    }

    @Test
    void canApply_shouldFail_whenNoPokemonInTop7() {
        List<String> deck = new ArrayList<>(List.of(
                NON_POKEMON_1, NON_POKEMON_2, "xy1-5", "xy1-6", "xy1-7", "xy1-8", "xy1-9"));
        PlayerState ps = playerState(PLAYER_1, List.of(), deck);
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-118");

        for (String id : List.of(NON_POKEMON_1, NON_POKEMON_2, "xy1-5", "xy1-6", "xy1-7", "xy1-8", "xy1-9")) {
            when(cardLookupPort.findCardById(id))
                    .thenReturn(Optional.of(cardWithSupertype(id, CardType.TRAINER)));
        }

        ValidationResult result = effect.canApply(state, act);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("No Pokémon found");
    }

    @Test
    void canApply_shouldSucceed_whenDeckSmallerThan7HasPokemon() {
        List<String> deck = new ArrayList<>(List.of(POKEMON_1, NON_POKEMON_1));
        PlayerState ps = playerState(PLAYER_1, List.of(), deck);
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-118");

        when(cardLookupPort.findCardById(POKEMON_1))
                .thenReturn(Optional.of(cardWithSupertype(POKEMON_1, CardType.POKEMON)));

        ValidationResult result = effect.canApply(state, act);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void canApply_shouldFail_whenDeckSmallerThan7NoPokemon() {
        List<String> deck = new ArrayList<>(List.of(NON_POKEMON_1, NON_POKEMON_2));
        PlayerState ps = playerState(PLAYER_1, List.of(), deck);
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-118");

        when(cardLookupPort.findCardById(NON_POKEMON_1))
                .thenReturn(Optional.of(cardWithSupertype(NON_POKEMON_1, CardType.TRAINER)));
        when(cardLookupPort.findCardById(NON_POKEMON_2))
                .thenReturn(Optional.of(cardWithSupertype(NON_POKEMON_2, CardType.ENERGY)));

        ValidationResult result = effect.canApply(state, act);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("No Pokémon found");
    }

    // ── apply ──────────────────────────────────────────────────────────────────

    @Test
    void apply_shouldSetPendingDeckSelectionState() {
        List<String> topPokemon = List.of(POKEMON_1, POKEMON_2);
        List<String> topNonPokemon = List.of(NON_POKEMON_1, NON_POKEMON_2);
        List<String> rest = List.of("xy1-5", "xy1-6", "xy1-7");
        List<String> deck = new ArrayList<>();
        deck.addAll(topPokemon);
        deck.addAll(topNonPokemon);
        deck.addAll(rest);

        PlayerState ps = playerState(PLAYER_1, List.of(), new ArrayList<>(deck));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-118");

        effect.apply(state, act);

        assertThat(state.isPendingDeckSelection()).isTrue();
        assertThat(state.getPendingDeckSelectionPlayerId()).isEqualTo(PLAYER_1);
        assertThat(state.getPendingDeckSelectionCardIds())
                .containsExactly(POKEMON_1, POKEMON_2, NON_POKEMON_1, NON_POKEMON_2, "xy1-5", "xy1-6", "xy1-7");
    }

    @Test
    void apply_shouldRemoveTopCardsFromDeck() {
        List<String> deck = new ArrayList<>(List.of(
                POKEMON_1, NON_POKEMON_1, NON_POKEMON_2, "xy1-5", "xy1-6", "xy1-7", "xy1-8", "xy1-9"));
        PlayerState ps = playerState(PLAYER_1, List.of(), new ArrayList<>(deck));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-118");

        effect.apply(state, act);

        assertThat(ps.getDeck()).containsExactly("xy1-9");
    }

    @Test
    void apply_shouldEmitTrainerPlayedEvent_withTopCards() {
        List<String> deck = new ArrayList<>(List.of(POKEMON_1, NON_POKEMON_1, NON_POKEMON_2));
        PlayerState ps = playerState(PLAYER_1, List.of(), new ArrayList<>(deck));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-118");

        EngineResult result = effect.apply(state, act);

        assertThat(result.events()).hasSize(1);
        GameEvent event = result.events().get(0);
        assertThat(event.getType()).isEqualTo(GameEventType.TRAINER_PLAYED);
        assertThat(event.getData()).containsEntry("cardId", "xy1-118");
        assertThat(event.getData()).containsKey("topCards");

        @SuppressWarnings("unchecked")
        List<String> topCards = (List<String>) event.getData().get("topCards");
        assertThat(topCards).containsExactly(POKEMON_1, NON_POKEMON_1, NON_POKEMON_2);
    }

    // ── Test helpers ───────────────────────────────────────────────────────────

    private Card cardWithSupertype(String id, CardType supertype) {
        return Card.builder()
                .id(id)
                .supertype(supertype)
                .build();
    }
}
