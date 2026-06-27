package com.pokemon.tcg.engine;

import com.pokemon.tcg.domain.model.card.Card;
import com.pokemon.tcg.domain.model.card.CardType;
import com.pokemon.tcg.domain.model.game.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RuleValidatorTest {

    private CardLookupPort cardLookupPort;
    private RuleValidator validator;

    @BeforeEach
    void setUp() {
        cardLookupPort = mock(CardLookupPort.class);
        validator = new RuleValidator(cardLookupPort);
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private BoardState buildActiveState(String currentPlayerId) {
        return buildActiveState(currentPlayerId, TurnPhase.MAIN, 2);
    }

    private BoardState buildActiveState(String currentPlayerId, TurnPhase phase, int turnNumber) {
        PlayerState ps1 = buildPlayerState("p1", new ArrayList<>());
        PlayerState ps2 = buildPlayerState("p2", new ArrayList<>());
        return BoardState.builder()
                .gameId("game-1")
                .gameState(GameState.ACTIVE)
                .turnPhase(phase)
                .currentPlayerId(currentPlayerId)
                .turnNumber(turnNumber)
                .firstPlayerId("p1")
                .player1State(ps1)
                .player2State(ps2)
                .turnFlags(TurnFlags.fresh())
                .pendingEvents(new ArrayList<>())
                .build();
    }

    private PlayerState buildPlayerState(String playerId, List<String> hand) {
        return PlayerState.builder()
                .playerId(playerId)
                .hand(new ArrayList<>(hand))
                .deck(new ArrayList<>())
                .discard(new ArrayList<>())
                .prizes(new ArrayList<>())
                .bench(new ArrayList<>())
                .activePokemon(buildActive("inst-" + playerId))
                .build();
    }

    private ActivePokemon buildActive(String instanceId) {
        return ActivePokemon.builder()
                .instanceId(instanceId)
                .cardId("xy1-1")
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .build();
    }

    private GameAction buildAction(String playerId, GameActionType type, Map<String, Object> payload) {
        return GameAction.builder()
                .playerId(playerId)
                .type(type)
                .payload(payload)
                .build();
    }

    private Card buildBasicPokemonCard(String id) {
        Card card = mock(Card.class);
        when(card.getSupertype()).thenReturn(CardType.POKEMON);
        when(card.getSubtypes()).thenReturn(List.of("Basic"));
        when(card.getId()).thenReturn(id);
        when(card.getRetreatCost()).thenReturn(List.of("Colorless"));
        return card;
    }

    private Card buildSupporterCard(String id) {
        Card card = mock(Card.class);
        when(card.getSupertype()).thenReturn(CardType.TRAINER);
        when(card.getSubtypes()).thenReturn(List.of("Supporter"));
        when(card.getId()).thenReturn(id);
        return card;
    }

    private Card buildItemCard(String id) {
        Card card = mock(Card.class);
        when(card.getSupertype()).thenReturn(CardType.TRAINER);
        when(card.getSubtypes()).thenReturn(List.of("Item"));
        when(card.getId()).thenReturn(id);
        return card;
    }

    // ─── turn order ───────────────────────────────────────────────────────────

    @Test
    void validate_shouldRejectAction_whenNotCurrentPlayer() {
        BoardState state = buildActiveState("p1");
        GameAction action = buildAction("p2", GameActionType.END_TURN, Map.of());

        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    // ─── DRAW_CARD ────────────────────────────────────────────────────────────

    @Test
    void validate_drawCard_shouldSucceed_whenInDrawPhase() {
        BoardState state = buildActiveState("p1", TurnPhase.DRAW, 2);
        GameAction action = buildAction("p1", GameActionType.DRAW_CARD, Map.of());

        assertThat(validator.validate(state, action).isValid()).isTrue();
    }

    @Test
    void validate_drawCard_shouldSucceed_whenDeckIsEmpty() {
        // Empty deck is a victory condition, not a validation error
        BoardState state = buildActiveState("p1", TurnPhase.DRAW, 2);
        GameAction action = buildAction("p1", GameActionType.DRAW_CARD, Map.of());

        assertThat(validator.validate(state, action).isValid()).isTrue();
    }

    @Test
    void validate_drawCard_shouldFail_whenNotInDrawPhase() {
        BoardState state = buildActiveState("p1", TurnPhase.MAIN, 2);
        GameAction action = buildAction("p1", GameActionType.DRAW_CARD, Map.of());

        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    // ─── PLACE_BASIC_POKEMON ──────────────────────────────────────────────────

    @Test
    void validate_placeBasicPokemon_shouldSucceed() {
        Card card = buildBasicPokemonCard("xy1-1");
        when(cardLookupPort.findCardById("xy1-1")).thenReturn(Optional.of(card));

        BoardState state = buildActiveState("p1");
        state.getPlayer1State().setHand(new ArrayList<>(List.of("xy1-1")));

        GameAction action = buildAction("p1", GameActionType.PLACE_BASIC_POKEMON,
                Map.of("cardId", "xy1-1"));

        assertThat(validator.validate(state, action).isValid()).isTrue();
    }

    @Test
    void validate_placeBasicPokemon_shouldFail_whenBenchIsFull() {
        BoardState state = buildActiveState("p1");
        List<BenchPokemon> bench = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            bench.add(BenchPokemon.builder()
                    .instanceId("bench-" + i)
                    .cardId("xy1-1")
                    .attachedEnergyIds(new ArrayList<>())
                    .evolutionStack(new ArrayList<>())
                    .build());
        }
        state.getPlayer1State().setBench(bench);
        state.getPlayer1State().setHand(new ArrayList<>(List.of("xy1-1")));

        GameAction action = buildAction("p1", GameActionType.PLACE_BASIC_POKEMON,
                Map.of("cardId", "xy1-1"));

        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    // ─── ATTACH_ENERGY ────────────────────────────────────────────────────────

    @Test
    void validate_attachEnergy_shouldSucceed() {
        BoardState state = buildActiveState("p1");
        state.getPlayer1State().setHand(new ArrayList<>(List.of("xy1-132")));

        GameAction action = buildAction("p1", GameActionType.ATTACH_ENERGY,
                Map.of("cardId", "xy1-132",
                        "targetInstanceId", "inst-p1"));

        assertThat(validator.validate(state, action).isValid()).isTrue();
    }

    @Test
    void validate_attachEnergy_shouldFail_whenAlreadyAttachedThisTurn() {
        BoardState state = buildActiveState("p1");
        state.getTurnFlags().setEnergyAttachedThisTurn(true);
        state.getPlayer1State().setHand(new ArrayList<>(List.of("xy1-132")));

        GameAction action = buildAction("p1", GameActionType.ATTACH_ENERGY,
                Map.of("cardId", "xy1-132",
                        "targetInstanceId", "inst-p1"));

        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    @Test
    void validate_attachEnergy_shouldFail_whenCardNotInHand() {
        BoardState state = buildActiveState("p1");
        state.getPlayer1State().setHand(new ArrayList<>());

        GameAction action = buildAction("p1", GameActionType.ATTACH_ENERGY,
                Map.of("cardId", "xy1-132",
                        "targetInstanceId", "inst-p1"));

        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    // ─── EVOLVE_POKEMON ───────────────────────────────────────────────────────

    @Test
    void validate_evolution_shouldFail_onFirstTurn() {
        BoardState state = buildActiveState("p1", TurnPhase.MAIN, 1);
        state.getPlayer1State().setHand(new ArrayList<>(List.of("xy1-2")));

        GameAction action = buildAction("p1", GameActionType.EVOLVE_POKEMON,
                Map.of("cardId", "xy1-2", "targetInstanceId", "inst-p1"));

        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    @Test
    void validate_evolution_shouldFail_whenTargetEnteredThisTurn() {
        BoardState state = buildActiveState("p1", TurnPhase.MAIN, 2);
        state.getPlayer1State().setHand(new ArrayList<>(List.of("xy1-2")));
        state.getPlayer1State().getActivePokemon().setEnteredThisTurn(true);

        GameAction action = buildAction("p1", GameActionType.EVOLVE_POKEMON,
                Map.of("cardId", "xy1-2", "targetInstanceId", "inst-p1"));

        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    @Test
    void validate_evolution_shouldSucceed_whenTargetIsEligible() {
        BoardState state = buildActiveState("p1", TurnPhase.MAIN, 2);
        state.getPlayer1State().setHand(new ArrayList<>(List.of("xy1-2")));
        state.getPlayer1State().getActivePokemon().setEnteredThisTurn(false);

        GameAction action = buildAction("p1", GameActionType.EVOLVE_POKEMON,
                Map.of("cardId", "xy1-2", "targetInstanceId", "inst-p1"));

        assertThat(validator.validate(state, action).isValid()).isTrue();
    }

    // ─── PLAY_TRAINER ─────────────────────────────────────────────────────────

    @Test
    void validate_playTrainer_supporter_shouldFail_whenAlreadyPlayedThisTurn() {
        Card supporter = buildSupporterCard("xy1-122");
        when(cardLookupPort.findCardById("xy1-122")).thenReturn(Optional.of(supporter));

        BoardState state = buildActiveState("p1");
        state.getTurnFlags().setSupporterPlayedThisTurn(true);
        state.getPlayer1State().setHand(new ArrayList<>(List.of("xy1-122")));

        GameAction action = buildAction("p1", GameActionType.PLAY_TRAINER,
                Map.of("cardId", "xy1-122"));

        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    @Test
    void validate_playTrainer_item_shouldSucceed_evenIfSupporterPlayed() {
        Card item = buildItemCard("xy1-118");
        when(cardLookupPort.findCardById("xy1-118")).thenReturn(Optional.of(item));

        BoardState state = buildActiveState("p1");
        state.getTurnFlags().setSupporterPlayedThisTurn(true);
        state.getPlayer1State().setHand(new ArrayList<>(List.of("xy1-118")));

        GameAction action = buildAction("p1", GameActionType.PLAY_TRAINER,
                Map.of("cardId", "xy1-118"));

        assertThat(validator.validate(state, action).isValid()).isTrue();
    }

    // ─── RETREAT ──────────────────────────────────────────────────────────────

    @Test
    void validate_retreat_shouldFail_whenAsleep() {
        BoardState state = buildActiveState("p1");
        state.getPlayer1State().getActivePokemon()
                .setConditions(Set.of(SpecialCondition.ASLEEP));
        state.getPlayer1State().setBench(List.of(
                BenchPokemon.builder().instanceId("b1").cardId("xy1-3")
                        .attachedEnergyIds(new ArrayList<>())
                        .evolutionStack(new ArrayList<>()).build()));

        GameAction action = buildAction("p1", GameActionType.RETREAT,
                Map.of("replacementInstanceId", "b1"));

        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    @Test
    void validate_retreat_shouldFail_whenParalyzed() {
        BoardState state = buildActiveState("p1");
        state.getPlayer1State().getActivePokemon()
                .setConditions(Set.of(SpecialCondition.PARALYZED));
        state.getPlayer1State().setBench(List.of(
                BenchPokemon.builder().instanceId("b1").cardId("xy1-3")
                        .attachedEnergyIds(new ArrayList<>())
                        .evolutionStack(new ArrayList<>()).build()));

        GameAction action = buildAction("p1", GameActionType.RETREAT,
                Map.of("replacementInstanceId", "b1",
                        "energyCardIdsToDiscard", List.of()));

        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    @Test
    void validate_retreat_shouldFail_whenNoBench() {
        BoardState state = buildActiveState("p1");
        state.getPlayer1State().setBench(new ArrayList<>());

        GameAction action = buildAction("p1", GameActionType.RETREAT,
                Map.of("replacementInstanceId", "b1"));

        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    // ─── DECLARE_ATTACK ───────────────────────────────────────────────────────

    @Test
    void validate_attack_shouldFail_whenAsleep() {
        BoardState state = buildActiveState("p1");
        state.getPlayer1State().getActivePokemon()
                .setConditions(Set.of(SpecialCondition.ASLEEP));

        GameAction action = buildAction("p1", GameActionType.DECLARE_ATTACK,
                Map.of("attackName", "Tackle"));

        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    @Test
    void validate_attack_shouldFail_whenParalyzed() {
        BoardState state = buildActiveState("p1");
        state.getPlayer1State().getActivePokemon()
                .setConditions(Set.of(SpecialCondition.PARALYZED));

        GameAction action = buildAction("p1", GameActionType.DECLARE_ATTACK,
                Map.of("attackName", "Tackle"));

        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    @Test
    void validate_attack_shouldFail_whenFirstPlayerOnTurnZero() {
        BoardState state = buildActiveState("p1", TurnPhase.MAIN, 0);
        // p1 is the firstPlayerId

        GameAction action = buildAction("p1", GameActionType.DECLARE_ATTACK,
                Map.of("attackName", "Tackle"));

        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    @Test
    void validate_attack_shouldSucceed_whenSecondPlayerOnTurnZero() {
        // p2 goes second — can attack on turn 0
        BoardState state = buildActiveState("p2", TurnPhase.MAIN, 0);

        GameAction action = buildAction("p2", GameActionType.DECLARE_ATTACK,
                Map.of("attackName", "Tackle"));

        assertThat(validator.validate(state, action).isValid()).isTrue();
    }

    @Test
    void validate_attack_shouldSucceed_onTurnOne() {
        BoardState state = buildActiveState("p1", TurnPhase.MAIN, 1);

        GameAction action = buildAction("p1", GameActionType.DECLARE_ATTACK,
                Map.of("attackName", "Tackle"));

        assertThat(validator.validate(state, action).isValid()).isTrue();
    }

    // ─── PENDING_BENCH_CHOICE ─────────────────────────────────────────────────

    @Test
    void validate_shouldBlockAllActions_whenPendingBenchChoice() {
        BoardState state = buildActiveState("p1");
        state.setPendingBenchChoicePlayerId("p2");

        GameAction action = buildAction("p1", GameActionType.END_TURN, Map.of());

        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    @Test
    void validate_chooseBenchPokemon_shouldSucceed_whenValidInstanceId() {
        BoardState state = buildActiveState("p1");
        state.setPendingBenchChoicePlayerId("p2");

        BenchPokemon bench = BenchPokemon.builder()
                .instanceId("bench-inst")
                .cardId("xy1-3")
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .build();
        state.getPlayer2State().setBench(new ArrayList<>(List.of(bench)));

        GameAction action = buildAction("p2", GameActionType.CHOOSE_BENCH_POKEMON,
                Map.of("instanceId", "bench-inst"));

        assertThat(validator.validate(state, action).isValid()).isTrue();
    }

    // ─── END_TURN ─────────────────────────────────────────────────────────────

    @Test
    void validate_endTurn_shouldSucceed_duringMainPhase() {
        BoardState state = buildActiveState("p1");
        GameAction action = buildAction("p1", GameActionType.END_TURN, Map.of());

        assertThat(validator.validate(state, action).isValid()).isTrue();
    }

    @Test
    void validate_endTurn_shouldFail_whenNotMainPhase() {
        BoardState state = buildActiveState("p1", TurnPhase.DRAW, 2);
        GameAction action = buildAction("p1", GameActionType.END_TURN, Map.of());

        assertThat(validator.validate(state, action).isValid()).isFalse();
    }
}