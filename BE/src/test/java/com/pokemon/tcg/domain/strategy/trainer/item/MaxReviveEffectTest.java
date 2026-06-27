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
class MaxReviveEffectTest {

    @Mock
    private CardLookupPort cardLookupPort;

    private MaxReviveEffect effect;

    private static final String POKEMON_CARD     = "xy1-3";
    private static final String NON_POKEMON_CARD = "xy1-132";

    @BeforeEach
    void setUp() {
        effect = new MaxReviveEffect(cardLookupPort);
    }

    @Test
    void canApply_shouldFail_whenNoCardSpecified() {
        PlayerState ps = playerStateWithDiscard(PLAYER_1, List.of(POKEMON_CARD));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-120");

        ValidationResult result = effect.canApply(state, act);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("No Pokémon specified for Max Revive.");
    }

    @Test
    void canApply_shouldFail_whenDiscardIsEmpty() {
        PlayerState ps = playerStateWithDiscard(PLAYER_1, List.of());
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1,
                "cardId", "xy1-120",
                "chosenCardId", POKEMON_CARD);

        ValidationResult result = effect.canApply(state, act);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Your discard pile is empty.");
    }

    @Test
    void canApply_shouldFail_whenCardNotInDiscard() {
        PlayerState ps = playerStateWithDiscard(PLAYER_1, List.of("xy1-5"));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1,
                "cardId", "xy1-120",
                "chosenCardId", POKEMON_CARD);

        ValidationResult result = effect.canApply(state, act);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("is not in your discard pile");
    }

    @Test
    void canApply_shouldFail_whenCardIsNotPokemon() {
        PlayerState ps = playerStateWithDiscard(PLAYER_1, List.of(NON_POKEMON_CARD));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1,
                "cardId", "xy1-120",
                "chosenCardId", NON_POKEMON_CARD);

        when(cardLookupPort.findCardById(NON_POKEMON_CARD))
                .thenReturn(Optional.of(nonPokemonCard(NON_POKEMON_CARD)));

        ValidationResult result = effect.canApply(state, act);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("is not a Pokémon");
    }

    @Test
    void canApply_shouldSucceed_whenValidPokemonInDiscard() {
        PlayerState ps = playerStateWithDiscard(PLAYER_1, List.of(POKEMON_CARD));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1,
                "cardId", "xy1-120",
                "chosenCardId", POKEMON_CARD);

        when(cardLookupPort.findCardById(POKEMON_CARD))
                .thenReturn(Optional.of(pokemonCard(POKEMON_CARD)));

        assertThat(effect.canApply(state, act).isValid()).isTrue();
    }

    @Test
    void apply_shouldPlacePokemonOnTopOfDeck() {
        PlayerState ps = playerStateWithDiscard(PLAYER_1, List.of(POKEMON_CARD));
        ps.setDeck(new ArrayList<>(cardIds(5)));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1,
                "cardId", "xy1-120",
                "chosenCardId", POKEMON_CARD);

        effect.apply(state, act);

        assertThat(ps.getDeck().get(0)).isEqualTo(POKEMON_CARD);
        assertThat(ps.getDeck()).hasSize(6);
    }

    @Test
    void apply_shouldRemovePokemonFromDiscard() {
        PlayerState ps = playerStateWithDiscard(PLAYER_1, List.of(POKEMON_CARD, "xy1-132"));
        ps.setDeck(new ArrayList<>(cardIds(5)));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1,
                "cardId", "xy1-120",
                "chosenCardId", POKEMON_CARD);

        effect.apply(state, act);

        assertThat(ps.getDiscard()).doesNotContain(POKEMON_CARD);
        assertThat(ps.getDiscard()).containsExactly("xy1-132");
    }

    @Test
    void apply_shouldPlacePokemonOnTopOfEmptyDeck() {
        PlayerState ps = playerStateWithDiscard(PLAYER_1, List.of(POKEMON_CARD));
        ps.setDeck(new ArrayList<>());
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1,
                "cardId", "xy1-120",
                "chosenCardId", POKEMON_CARD);

        effect.apply(state, act);

        assertThat(ps.getDeck()).containsExactly(POKEMON_CARD);
    }

    @Test
    void apply_shouldEmitTrainerPlayedEvent_withRevivedCardId() {
        PlayerState ps = playerStateWithDiscard(PLAYER_1, List.of(POKEMON_CARD));
        ps.setDeck(new ArrayList<>(cardIds(5)));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1,
                "cardId", "xy1-120",
                "chosenCardId", POKEMON_CARD);

        EngineResult result = effect.apply(state, act);

        assertThat(result.events()).hasSize(1);
        GameEvent event = result.events().get(0);
        assertThat(event.getType()).isEqualTo(GameEventType.TRAINER_PLAYED);
        assertThat(event.getData()).containsEntry("cardId", "xy1-120");
        assertThat(event.getData()).containsEntry("revivedCardId", POKEMON_CARD);
    }

    private PlayerState playerStateWithDiscard(String playerId, List<String> discard) {
        PlayerState ps = playerState(playerId, List.of(), List.of());
        ps.setDiscard(new ArrayList<>(discard));
        return ps;
    }

    private Card pokemonCard(String id) {
        return Card.builder().id(id).supertype(CardType.POKEMON).build();
    }

    private Card nonPokemonCard(String id) {
        return Card.builder().id(id).supertype(CardType.TRAINER).build();
    }
}
