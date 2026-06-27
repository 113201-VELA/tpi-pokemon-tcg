package com.pokemon.tcg.domain.strategy.ability.active;

import com.pokemon.tcg.domain.model.game.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

class DelphoxAbilityTest {

    private DelphoxAbility ability;

    @BeforeEach
    void setUp() {
        ability = new DelphoxAbility();
    }

    @Test
    void getIdentifier_shouldReturn_delphoxMysticalFire() {
        assertThat(ability.getIdentifier()).isEqualTo("delphox|mystical fire");
    }

    @Test
    void canApply_shouldFail_whenHandAlreadyHas6Cards() {
        PlayerState ps = playerState(PLAYER_1, cardIds(6), cardIds(10));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = abilityAction("inst-1", "Mystical Fire");

        assertThat(ability.canApply(state, act).isValid()).isFalse();
        assertThat(ability.canApply(state, act).getErrorMessage())
                .isEqualTo("You already have 6 or more cards in hand.");
    }

    @Test
    void canApply_shouldFail_whenDeckIsEmpty() {
        PlayerState ps = playerState(PLAYER_1, cardIds(3), List.of());
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = abilityAction("inst-1", "Mystical Fire");

        assertThat(ability.canApply(state, act).isValid()).isFalse();
        assertThat(ability.canApply(state, act).getErrorMessage())
                .isEqualTo("Your deck is empty.");
    }

    @Test
    void canApply_shouldSucceed_whenHandHasLessThan6AndDeckHasCards() {
        PlayerState ps = playerState(PLAYER_1, cardIds(3), cardIds(10));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = abilityAction("inst-1", "Mystical Fire");

        assertThat(ability.canApply(state, act).isValid()).isTrue();
    }

    @Test
    void apply_shouldDrawUntilHandHas6Cards() {
        PlayerState ps = playerState(PLAYER_1, cardIds(3), cardIds(10));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = abilityAction("inst-1", "Mystical Fire");

        ability.apply(state, act);

        assertThat(ps.getHand()).hasSize(6);
        assertThat(ps.getDeck()).hasSize(7);
    }

    @Test
    void apply_shouldDrawAllRemainingCards_whenDeckHasLessThanNeeded() {
        PlayerState ps = playerState(PLAYER_1, cardIds(4), cardIds(1));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = abilityAction("inst-1", "Mystical Fire");

        ability.apply(state, act);

        assertThat(ps.getHand()).hasSize(5);
        assertThat(ps.getDeck()).isEmpty();
    }

    @Test
    void apply_shouldMarkAbilityAsUsedThisTurn() {
        PlayerState ps = playerState(PLAYER_1, cardIds(3), cardIds(10));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = abilityAction("inst-1", "Mystical Fire");

        ability.apply(state, act);

        assertThat(state.getTurnFlags().isAbilityUsed("inst-1", "Mystical Fire"))
                .isTrue();
    }

    @Test
    void apply_shouldNotAffectOpponent() {
        PlayerState ps       = playerState(PLAYER_1, cardIds(3), cardIds(10));
        PlayerState opponent = playerState(PLAYER_2, cardIds(4), cardIds(8));
        BoardState state = boardState(ps, opponent);
        GameAction act = abilityAction("inst-1", "Mystical Fire");

        ability.apply(state, act);

        assertThat(opponent.getHand()).hasSize(4);
        assertThat(opponent.getDeck()).hasSize(8);
    }

    private GameAction abilityAction(String instanceId, String abilityName) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("instanceId", instanceId);
        payload.put("abilityName", abilityName);
        return GameAction.builder()
                .type(GameActionType.USE_ABILITY)
                .playerId(PLAYER_1)
                .payload(payload)
                .build();
    }
}
