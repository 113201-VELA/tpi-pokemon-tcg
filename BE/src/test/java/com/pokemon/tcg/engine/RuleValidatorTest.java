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

    private Card buildStadiumCard(String id) {
        Card card = mock(Card.class);
        when(card.getSupertype()).thenReturn(CardType.TRAINER);
        when(card.getSubtypes()).thenReturn(List.of("Stadium"));
        when(card.getId()).thenReturn(id);
        return card;
    }

    private BenchPokemon buildBench(String instanceId, String cardId) {
        return BenchPokemon.builder()
                .instanceId(instanceId)
                .cardId(cardId)
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .build();
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

    // ─── SETUP: PLACE_ACTIVE ──────────────────────────────────────────────────

    @Test
    void validate_setupPlaceActive_shouldSucceed_whenValidBasicInHand() {
        BoardState state = buildActiveState("p1", TurnPhase.SETUP, 0);
        state.getPlayer1State().setActivePokemon(null);
        state.getPlayer1State().setHand(new ArrayList<>(List.of("xy1-1")));
        Card basic = buildBasicPokemonCard("xy1-1");
        when(cardLookupPort.findCardById("xy1-1")).thenReturn(Optional.of(basic));

        GameAction action = buildAction("p1", GameActionType.SETUP_PLACE_ACTIVE,
                Map.of("cardId", "xy1-1"));
        assertThat(validator.validate(state, action).isValid()).isTrue();
    }

    @Test
    void validate_setupPlaceActive_shouldFail_whenNotSetupPhase() {
        BoardState state = buildActiveState("p1", TurnPhase.MAIN, 2);
        state.getPlayer1State().setActivePokemon(null);
        state.getPlayer1State().setHand(new ArrayList<>(List.of("xy1-1")));

        GameAction action = buildAction("p1", GameActionType.SETUP_PLACE_ACTIVE,
                Map.of("cardId", "xy1-1"));
        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    @Test
    void validate_setupPlaceActive_shouldFail_whenActiveAlreadyPlaced() {
        BoardState state = buildActiveState("p1", TurnPhase.SETUP, 0);
        // active is already set by buildPlayerState
        state.getPlayer1State().setHand(new ArrayList<>(List.of("xy1-1")));

        GameAction action = buildAction("p1", GameActionType.SETUP_PLACE_ACTIVE,
                Map.of("cardId", "xy1-1"));
        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    @Test
    void validate_setupPlaceActive_shouldFail_whenCardNotInHand() {
        BoardState state = buildActiveState("p1", TurnPhase.SETUP, 0);
        state.getPlayer1State().setActivePokemon(null);
        state.getPlayer1State().setHand(new ArrayList<>());

        GameAction action = buildAction("p1", GameActionType.SETUP_PLACE_ACTIVE,
                Map.of("cardId", "xy1-1"));
        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    // ─── SETUP: PLACE_BENCH ───────────────────────────────────────────────────

    @Test
    void validate_setupPlaceBench_shouldSucceed_whenValidConditions() {
        BoardState state = buildActiveState("p1", TurnPhase.SETUP, 0);
        state.getPlayer1State().setHand(new ArrayList<>(List.of("xy1-2")));
        Card basic = buildBasicPokemonCard("xy1-2");
        when(cardLookupPort.findCardById("xy1-2")).thenReturn(Optional.of(basic));

        GameAction action = buildAction("p1", GameActionType.SETUP_PLACE_BENCH,
                Map.of("cardId", "xy1-2"));
        assertThat(validator.validate(state, action).isValid()).isTrue();
    }

    @Test
    void validate_setupPlaceBench_shouldFail_whenNotSetupPhase() {
        BoardState state = buildActiveState("p1", TurnPhase.MAIN, 2);
        state.getPlayer1State().setHand(new ArrayList<>(List.of("xy1-2")));

        GameAction action = buildAction("p1", GameActionType.SETUP_PLACE_BENCH,
                Map.of("cardId", "xy1-2"));
        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    @Test
    void validate_setupPlaceBench_shouldFail_whenNoActivePokemon() {
        BoardState state = buildActiveState("p1", TurnPhase.SETUP, 0);
        state.getPlayer1State().setActivePokemon(null);
        state.getPlayer1State().setHand(new ArrayList<>(List.of("xy1-2")));

        GameAction action = buildAction("p1", GameActionType.SETUP_PLACE_BENCH,
                Map.of("cardId", "xy1-2"));
        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    @Test
    void validate_setupPlaceBench_shouldFail_whenBenchFull() {
        BoardState state = buildActiveState("p1", TurnPhase.SETUP, 0);
        state.getPlayer1State().setHand(new ArrayList<>(List.of("xy1-2")));
        List<BenchPokemon> fullBench = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            fullBench.add(buildBench("b-" + i, "xy1-1"));
        }
        state.getPlayer1State().setBench(fullBench);

        GameAction action = buildAction("p1", GameActionType.SETUP_PLACE_BENCH,
                Map.of("cardId", "xy1-2"));
        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    // ─── MULLIGAN_CONFIRM ─────────────────────────────────────────────────────

    @Test
    void validate_mulliganConfirm_shouldSucceed_whenNoBasicInHand() {
        BoardState state = buildActiveState("p1", TurnPhase.SETUP, 0);
        state.getPlayer1State().setActivePokemon(null);
        state.getPlayer1State().setHand(new ArrayList<>(List.of("xy1-132"))); // energy, not basic
        Card energy = mock(Card.class);
        when(energy.getSupertype()).thenReturn(CardType.ENERGY);
        when(energy.getSubtypes()).thenReturn(List.of());
        when(cardLookupPort.findCardById("xy1-132")).thenReturn(Optional.of(energy));

        GameAction action = buildAction("p1", GameActionType.MULLIGAN_CONFIRM, Map.of());
        assertThat(validator.validate(state, action).isValid()).isTrue();
    }

    @Test
    void validate_mulliganConfirm_shouldSucceed_whenHandIsEmpty() {
        BoardState state = buildActiveState("p1", TurnPhase.SETUP, 0);
        state.getPlayer1State().setActivePokemon(null);
        state.getPlayer1State().setHand(new ArrayList<>());

        GameAction action = buildAction("p1", GameActionType.MULLIGAN_CONFIRM, Map.of());
        assertThat(validator.validate(state, action).isValid()).isTrue();
    }

    @Test
    void validate_mulliganConfirm_shouldFail_whenHasBasicInHand() {
        BoardState state = buildActiveState("p1", TurnPhase.SETUP, 0);
        state.getPlayer1State().setActivePokemon(null);
        state.getPlayer1State().setHand(new ArrayList<>(List.of("xy1-1")));
        Card basic = buildBasicPokemonCard("xy1-1");
        when(cardLookupPort.findCardById("xy1-1")).thenReturn(Optional.of(basic));

        GameAction action = buildAction("p1", GameActionType.MULLIGAN_CONFIRM, Map.of());
        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    @Test
    void validate_mulliganConfirm_shouldFail_whenActiveAlreadyPlaced() {
        BoardState state = buildActiveState("p1", TurnPhase.SETUP, 0);
        // active is already set
        GameAction action = buildAction("p1", GameActionType.MULLIGAN_CONFIRM, Map.of());
        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    @Test
    void validate_mulliganConfirm_shouldFail_whenNotSetupPhase() {
        BoardState state = buildActiveState("p1", TurnPhase.MAIN, 2);
        state.getPlayer1State().setActivePokemon(null);

        GameAction action = buildAction("p1", GameActionType.MULLIGAN_CONFIRM, Map.of());
        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    // ─── ACCEPT_MULLIGAN_BONUS ────────────────────────────────────────────────

    @Test
    void validate_acceptMulliganBonus_shouldSucceed_whenValid() {
        BoardState state = buildActiveState("p1", TurnPhase.SETUP, 0);
        state.setBonusDrawPending(true);
        state.getPlayer1State().setMulliganBonusDraws(2);

        GameAction action = buildAction("p1", GameActionType.ACCEPT_MULLIGAN_BONUS,
                Map.of("cardsToDraw", 1));
        assertThat(validator.validate(state, action).isValid()).isTrue();
    }

    @Test
    void validate_acceptMulliganBonus_shouldFail_whenNoPendingBonus() {
        BoardState state = buildActiveState("p1", TurnPhase.SETUP, 0);
        state.setBonusDrawPending(true);
        state.getPlayer1State().setMulliganBonusDraws(0); // no bonus for this player

        GameAction action = buildAction("p1", GameActionType.ACCEPT_MULLIGAN_BONUS,
                Map.of("cardsToDraw", 1));
        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    @Test
    void validate_acceptMulliganBonus_shouldFail_whenCardsToDrawIsNull() {
        BoardState state = buildActiveState("p1", TurnPhase.SETUP, 0);
        state.setBonusDrawPending(true);
        state.getPlayer1State().setMulliganBonusDraws(2);

        GameAction action = buildAction("p1", GameActionType.ACCEPT_MULLIGAN_BONUS,
                new HashMap<>());  // no cardsToDraw key
        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    @Test
    void validate_acceptMulliganBonus_shouldFail_whenCardsToDrawIsNegative() {
        BoardState state = buildActiveState("p1", TurnPhase.SETUP, 0);
        state.setBonusDrawPending(true);
        state.getPlayer1State().setMulliganBonusDraws(2);

        GameAction action = buildAction("p1", GameActionType.ACCEPT_MULLIGAN_BONUS,
                Map.of("cardsToDraw", -1));
        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    @Test
    void validate_acceptMulliganBonus_shouldFail_whenCardsToDrawExceedsBonus() {
        BoardState state = buildActiveState("p1", TurnPhase.SETUP, 0);
        state.setBonusDrawPending(true);
        state.getPlayer1State().setMulliganBonusDraws(2);

        GameAction action = buildAction("p1", GameActionType.ACCEPT_MULLIGAN_BONUS,
                Map.of("cardsToDraw", 5));
        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    // ─── CONFIRM_SETUP ────────────────────────────────────────────────────────

    @Test
    void validate_confirmSetup_shouldSucceed_whenActiveIsPlaced() {
        BoardState state = buildActiveState("p1", TurnPhase.SETUP, 0);
        // active is set by buildPlayerState

        GameAction action = buildAction("p1", GameActionType.CONFIRM_SETUP, Map.of());
        assertThat(validator.validate(state, action).isValid()).isTrue();
    }

    @Test
    void validate_confirmSetup_shouldFail_whenNoActivePokemon() {
        BoardState state = buildActiveState("p1", TurnPhase.SETUP, 0);
        state.getPlayer1State().setActivePokemon(null);

        GameAction action = buildAction("p1", GameActionType.CONFIRM_SETUP, Map.of());
        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    @Test
    void validate_confirmSetup_shouldFail_whenAlreadyConfirmed() {
        BoardState state = buildActiveState("p1", TurnPhase.SETUP, 0);
        state.getPlayer1State().setSetupConfirmed(true);

        GameAction action = buildAction("p1", GameActionType.CONFIRM_SETUP, Map.of());
        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    @Test
    void validate_confirmSetup_shouldFail_whenNotSetupPhase() {
        BoardState state = buildActiveState("p1", TurnPhase.MAIN, 2);

        GameAction action = buildAction("p1", GameActionType.CONFIRM_SETUP, Map.of());
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
            bench.add(buildBench("bench-" + i, "xy1-1"));
        }
        state.getPlayer1State().setBench(bench);
        state.getPlayer1State().setHand(new ArrayList<>(List.of("xy1-1")));

        GameAction action = buildAction("p1", GameActionType.PLACE_BASIC_POKEMON,
                Map.of("cardId", "xy1-1"));
        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    @Test
    void validate_placeBasicPokemon_shouldFail_whenNotMainPhase() {
        BoardState state = buildActiveState("p1", TurnPhase.DRAW, 2);
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
                Map.of("cardId", "xy1-132", "targetInstanceId", "inst-p1"));
        assertThat(validator.validate(state, action).isValid()).isTrue();
    }

    @Test
    void validate_attachEnergy_shouldFail_whenAlreadyAttachedThisTurn() {
        BoardState state = buildActiveState("p1");
        state.getTurnFlags().setEnergyAttachedThisTurn(true);
        state.getPlayer1State().setHand(new ArrayList<>(List.of("xy1-132")));

        GameAction action = buildAction("p1", GameActionType.ATTACH_ENERGY,
                Map.of("cardId", "xy1-132", "targetInstanceId", "inst-p1"));
        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    @Test
    void validate_attachEnergy_shouldFail_whenCardNotInHand() {
        BoardState state = buildActiveState("p1");
        state.getPlayer1State().setHand(new ArrayList<>());

        GameAction action = buildAction("p1", GameActionType.ATTACH_ENERGY,
                Map.of("cardId", "xy1-132", "targetInstanceId", "inst-p1"));
        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    @Test
    void validate_attachEnergy_shouldFail_whenTargetNotInPlay() {
        BoardState state = buildActiveState("p1");
        state.getPlayer1State().setHand(new ArrayList<>(List.of("xy1-132")));

        GameAction action = buildAction("p1", GameActionType.ATTACH_ENERGY,
                Map.of("cardId", "xy1-132", "targetInstanceId", "nonexistent-inst"));
        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    @Test
    void validate_attachEnergy_shouldFail_whenNotMainPhase() {
        BoardState state = buildActiveState("p1", TurnPhase.DRAW, 2);
        state.getPlayer1State().setHand(new ArrayList<>(List.of("xy1-132")));

        GameAction action = buildAction("p1", GameActionType.ATTACH_ENERGY,
                Map.of("cardId", "xy1-132", "targetInstanceId", "inst-p1"));
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

    @Test
    void validate_evolution_shouldFail_whenCardNotInHand() {
        BoardState state = buildActiveState("p1", TurnPhase.MAIN, 2);
        state.getPlayer1State().setHand(new ArrayList<>());

        GameAction action = buildAction("p1", GameActionType.EVOLVE_POKEMON,
                Map.of("cardId", "xy1-2", "targetInstanceId", "inst-p1"));
        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    @Test
    void validate_evolution_shouldFail_whenTargetNotInPlay() {
        BoardState state = buildActiveState("p1", TurnPhase.MAIN, 2);
        state.getPlayer1State().setHand(new ArrayList<>(List.of("xy1-2")));
        state.getPlayer1State().setBench(new ArrayList<>());

        GameAction action = buildAction("p1", GameActionType.EVOLVE_POKEMON,
                Map.of("cardId", "xy1-2", "targetInstanceId", "nonexistent"));
        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    @Test
    void validate_evolution_shouldFail_whenEvolvesFromDoesNotMatch() {
        BoardState state = buildActiveState("p1", TurnPhase.MAIN, 2);
        state.getPlayer1State().setHand(new ArrayList<>(List.of("xy1-3")));
        state.getPlayer1State().getActivePokemon().setEnteredThisTurn(false);
        state.getPlayer1State().getActivePokemon().setCardId("xy1-1");

        Card evolutionCard = mock(Card.class);
        when(evolutionCard.getName()).thenReturn("Charmeleon");
        when(evolutionCard.getEvolvesFrom()).thenReturn("Charmander");
        when(cardLookupPort.findCardById("xy1-3")).thenReturn(Optional.of(evolutionCard));

        Card baseCard = mock(Card.class);
        when(baseCard.getName()).thenReturn("Bulbasaur"); // wrong base
        when(cardLookupPort.findCardById("xy1-1")).thenReturn(Optional.of(baseCard));

        GameAction action = buildAction("p1", GameActionType.EVOLVE_POKEMON,
                Map.of("cardId", "xy1-3", "targetInstanceId", "inst-p1"));
        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    @Test
    void validate_evolution_shouldSucceed_whenTargetIsOnBench() {
        BoardState state = buildActiveState("p1", TurnPhase.MAIN, 2);
        state.getPlayer1State().setHand(new ArrayList<>(List.of("xy1-2")));

        BenchPokemon bench = buildBench("bench-inst", "xy1-1");
        state.getPlayer1State().setBench(new ArrayList<>(List.of(bench)));

        GameAction action = buildAction("p1", GameActionType.EVOLVE_POKEMON,
                Map.of("cardId", "xy1-2", "targetInstanceId", "bench-inst"));
        assertThat(validator.validate(state, action).isValid()).isTrue();
    }

    @Test
    void validate_evolution_shouldFail_whenNotMainPhase() {
        BoardState state = buildActiveState("p1", TurnPhase.DRAW, 2);
        state.getPlayer1State().setHand(new ArrayList<>(List.of("xy1-2")));

        GameAction action = buildAction("p1", GameActionType.EVOLVE_POKEMON,
                Map.of("cardId", "xy1-2", "targetInstanceId", "inst-p1"));
        assertThat(validator.validate(state, action).isValid()).isFalse();
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

    @Test
    void validate_playTrainer_stadium_shouldFail_whenAlreadyPlayedThisTurn() {
        Card stadium = buildStadiumCard("xy1-117");
        when(cardLookupPort.findCardById("xy1-117")).thenReturn(Optional.of(stadium));

        BoardState state = buildActiveState("p1");
        state.getTurnFlags().setStadiumPlayedThisTurn(true);
        state.getPlayer1State().setHand(new ArrayList<>(List.of("xy1-117")));

        GameAction action = buildAction("p1", GameActionType.PLAY_TRAINER,
                Map.of("cardId", "xy1-117"));
        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    @Test
    void validate_playTrainer_shouldFail_whenCardNotInHand() {
        BoardState state = buildActiveState("p1");
        state.getPlayer1State().setHand(new ArrayList<>());

        GameAction action = buildAction("p1", GameActionType.PLAY_TRAINER,
                Map.of("cardId", "xy1-118"));
        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    @Test
    void validate_playTrainer_shouldFail_whenNotMainPhase() {
        BoardState state = buildActiveState("p1", TurnPhase.DRAW, 2);
        state.getPlayer1State().setHand(new ArrayList<>(List.of("xy1-118")));

        GameAction action = buildAction("p1", GameActionType.PLAY_TRAINER,
                Map.of("cardId", "xy1-118"));
        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    // ─── RETREAT ──────────────────────────────────────────────────────────────

    @Test
    void validate_retreat_shouldFail_whenAsleep() {
        BoardState state = buildActiveState("p1");
        state.getPlayer1State().getActivePokemon()
                .setConditions(Set.of(SpecialCondition.ASLEEP));
        state.getPlayer1State().setBench(List.of(buildBench("b1", "xy1-3")));

        GameAction action = buildAction("p1", GameActionType.RETREAT,
                Map.of("replacementInstanceId", "b1"));
        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    @Test
    void validate_retreat_shouldFail_whenParalyzed() {
        BoardState state = buildActiveState("p1");
        state.getPlayer1State().getActivePokemon()
                .setConditions(Set.of(SpecialCondition.PARALYZED));
        state.getPlayer1State().setBench(List.of(buildBench("b1", "xy1-3")));

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

    @Test
    void validate_retreat_shouldFail_whenAlreadyRetreatedThisTurn() {
        BoardState state = buildActiveState("p1");
        state.getTurnFlags().setRetreatedThisTurn(true);
        state.getPlayer1State().setBench(List.of(buildBench("b1", "xy1-3")));

        GameAction action = buildAction("p1", GameActionType.RETREAT,
                Map.of("replacementInstanceId", "b1",
                        "energyCardIdsToDiscard", List.of()));
        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    @Test
    void validate_retreat_shouldFail_whenWrongEnergyCount() {
        BoardState state = buildActiveState("p1");
        state.getPlayer1State().setBench(List.of(buildBench("b1", "xy1-3")));

        // Card has retreat cost of 1 but player provides 0 energies
        Card card = mock(Card.class);
        when(card.getRetreatCost()).thenReturn(List.of("Colorless"));
        when(cardLookupPort.findCardById("xy1-1")).thenReturn(Optional.of(card));

        GameAction action = buildAction("p1", GameActionType.RETREAT,
                Map.of("replacementInstanceId", "b1",
                        "energyCardIdsToDiscard", List.of())); // 0 energies but cost is 1
        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    @Test
    void validate_retreat_shouldFail_whenEnergyNotAttached() {
        BoardState state = buildActiveState("p1");
        state.getPlayer1State().setBench(List.of(buildBench("b1", "xy1-3")));
        state.getPlayer1State().getActivePokemon().setAttachedEnergyIds(new ArrayList<>());

        Card card = mock(Card.class);
        when(card.getRetreatCost()).thenReturn(List.of("Colorless"));
        when(cardLookupPort.findCardById("xy1-1")).thenReturn(Optional.of(card));

        GameAction action = buildAction("p1", GameActionType.RETREAT,
                Map.of("replacementInstanceId", "b1",
                        "energyCardIdsToDiscard", List.of("xy1-132"))); // energy not attached
        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    @Test
    void validate_retreat_shouldSucceed_withCorrectEnergyCount() {
        BoardState state = buildActiveState("p1");
        state.getPlayer1State().setBench(List.of(buildBench("b1", "xy1-3")));
        state.getPlayer1State().getActivePokemon()
                .setAttachedEnergyIds(new ArrayList<>(List.of("xy1-132")));

        Card card = mock(Card.class);
        when(card.getRetreatCost()).thenReturn(List.of("Colorless"));
        when(cardLookupPort.findCardById("xy1-1")).thenReturn(Optional.of(card));

        GameAction action = buildAction("p1", GameActionType.RETREAT,
                Map.of("replacementInstanceId", "b1",
                        "energyCardIdsToDiscard", List.of("xy1-132")));
        assertThat(validator.validate(state, action).isValid()).isTrue();
    }

    @Test
    void validate_retreat_shouldFail_whenNotMainPhase() {
        BoardState state = buildActiveState("p1", TurnPhase.DRAW, 2);
        state.getPlayer1State().setBench(List.of(buildBench("b1", "xy1-3")));

        GameAction action = buildAction("p1", GameActionType.RETREAT,
                Map.of("replacementInstanceId", "b1",
                        "energyCardIdsToDiscard", List.of()));
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

        GameAction action = buildAction("p1", GameActionType.DECLARE_ATTACK,
                Map.of("attackName", "Tackle"));
        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    @Test
    void validate_attack_shouldSucceed_whenSecondPlayerOnTurnZero() {
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

    @Test
    void validate_attack_shouldFail_whenAttackBlockedByTorment() {
        BoardState state = buildActiveState("p1", TurnPhase.MAIN, 2);
        state.getPlayer1State().getActivePokemon().setBlockedAttackName("Tackle");

        GameAction action = buildAction("p1", GameActionType.DECLARE_ATTACK,
                Map.of("attackName", "Tackle"));
        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    @Test
    void validate_attack_shouldSucceed_whenAttackNameDiffersFromBlocked() {
        BoardState state = buildActiveState("p1", TurnPhase.MAIN, 2);
        state.getPlayer1State().getActivePokemon().setBlockedAttackName("Tackle");

        GameAction action = buildAction("p1", GameActionType.DECLARE_ATTACK,
                Map.of("attackName", "Scratch")); // different attack, allowed
        assertThat(validator.validate(state, action).isValid()).isTrue();
    }

    @Test
    void validate_attack_shouldFail_whenNotMainPhase() {
        BoardState state = buildActiveState("p1", TurnPhase.DRAW, 2);

        GameAction action = buildAction("p1", GameActionType.DECLARE_ATTACK,
                Map.of("attackName", "Tackle"));
        assertThat(validator.validate(state, action).isValid()).isFalse();
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

        BenchPokemon bench = buildBench("bench-inst", "xy1-3");
        state.getPlayer2State().setBench(new ArrayList<>(List.of(bench)));

        GameAction action = buildAction("p2", GameActionType.CHOOSE_BENCH_POKEMON,
                Map.of("instanceId", "bench-inst"));
        assertThat(validator.validate(state, action).isValid()).isTrue();
    }

    @Test
    void validate_chooseBenchPokemon_shouldFail_whenWrongPlayer() {
        BoardState state = buildActiveState("p1");
        state.setPendingBenchChoicePlayerId("p2");

        BenchPokemon bench = buildBench("bench-inst", "xy1-3");
        state.getPlayer2State().setBench(new ArrayList<>(List.of(bench)));

        // p1 tries to choose but it's p2's turn
        GameAction action = buildAction("p1", GameActionType.CHOOSE_BENCH_POKEMON,
                Map.of("instanceId", "bench-inst"));
        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    @Test
    void validate_chooseBenchPokemon_shouldFail_whenInstanceIdNull() {
        BoardState state = buildActiveState("p1");
        state.setPendingBenchChoicePlayerId("p2");
        state.getPlayer2State().setBench(List.of(buildBench("bench-inst", "xy1-3")));

        GameAction action = buildAction("p2", GameActionType.CHOOSE_BENCH_POKEMON,
                new HashMap<>()); // no instanceId
        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    @Test
    void validate_chooseBenchPokemon_shouldFail_whenInstanceIdNotOnBench() {
        BoardState state = buildActiveState("p1");
        state.setPendingBenchChoicePlayerId("p2");
        state.getPlayer2State().setBench(List.of(buildBench("bench-inst", "xy1-3")));

        GameAction action = buildAction("p2", GameActionType.CHOOSE_BENCH_POKEMON,
                Map.of("instanceId", "wrong-inst"));
        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    // ─── USE_ABILITY ──────────────────────────────────────────────────────────

    @Test
    void validate_useAbility_shouldSucceed_whenPokemonInPlay() {
        BoardState state = buildActiveState("p1");

        GameAction action = buildAction("p1", GameActionType.USE_ABILITY,
                Map.of("instanceId", "inst-p1", "abilityName", "Mystical Fire"));
        assertThat(validator.validate(state, action).isValid()).isTrue();
    }

    @Test
    void validate_useAbility_shouldFail_whenInstanceIdNull() {
        BoardState state = buildActiveState("p1");

        GameAction action = buildAction("p1", GameActionType.USE_ABILITY,
                Map.of("abilityName", "Mystical Fire")); // no instanceId
        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    @Test
    void validate_useAbility_shouldFail_whenAbilityNameNull() {
        BoardState state = buildActiveState("p1");

        GameAction action = buildAction("p1", GameActionType.USE_ABILITY,
                Map.of("instanceId", "inst-p1")); // no abilityName
        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    @Test
    void validate_useAbility_shouldFail_whenPokemonNotInPlay() {
        BoardState state = buildActiveState("p1");

        GameAction action = buildAction("p1", GameActionType.USE_ABILITY,
                Map.of("instanceId", "nonexistent", "abilityName", "Mystical Fire"));
        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    @Test
    void validate_useAbility_shouldFail_whenAbilityAlreadyUsedThisTurn() {
        BoardState state = buildActiveState("p1");
        state.getTurnFlags().markAbilityUsed("inst-p1", "Mystical Fire");

        GameAction action = buildAction("p1", GameActionType.USE_ABILITY,
                Map.of("instanceId", "inst-p1", "abilityName", "Mystical Fire"));
        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    @Test
    void validate_useAbility_shouldFail_whenNotMainPhase() {
        BoardState state = buildActiveState("p1", TurnPhase.DRAW, 2);

        GameAction action = buildAction("p1", GameActionType.USE_ABILITY,
                Map.of("instanceId", "inst-p1", "abilityName", "Mystical Fire"));
        assertThat(validator.validate(state, action).isValid()).isFalse();
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

    // ─── PENDING STATES (bonus draw, forced switch, deck selection) ───────────

    @Test
    void validate_shouldBlockNonBonusActions_whenBonusDrawPending() {
        BoardState state = buildActiveState("p1", TurnPhase.SETUP, 0);
        state.setBonusDrawPending(true);
        state.getPlayer1State().setMulliganBonusDraws(1);

        GameAction action = buildAction("p1", GameActionType.END_TURN, Map.of());
        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    @Test
    void validate_shouldBlockNonSwitchActions_whenForcedSwitchPending() {
        BoardState state = buildActiveState("p1");
        state.setPendingForcedSwitchPlayerId("p2");

        GameAction action = buildAction("p1", GameActionType.END_TURN, Map.of());
        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    @Test
    void validate_forcedSwitch_shouldFail_whenWrongPlayer() {
        BoardState state = buildActiveState("p1");
        state.setPendingForcedSwitchPlayerId("p2");
        state.getPlayer2State().setBench(List.of(buildBench("b1", "xy1-3")));

        // p1 sends FORCED_SWITCH but it's p2's obligation
        GameAction action = buildAction("p1", GameActionType.FORCED_SWITCH,
                Map.of("instanceId", "b1"));
        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    @Test
    void validate_forcedSwitch_shouldSucceed_whenCorrectPlayer() {
        BoardState state = buildActiveState("p1");
        state.setPendingForcedSwitchPlayerId("p2");
        state.getPlayer2State().setBench(List.of(buildBench("b1", "xy1-3")));

        GameAction action = buildAction("p2", GameActionType.FORCED_SWITCH,
                Map.of("instanceId", "b1"));
        assertThat(validator.validate(state, action).isValid()).isTrue();
    }

    @Test
    void validate_shouldBlockNonSelectActions_whenDeckSelectionPending() {
        BoardState state = buildActiveState("p1");
        state.setPendingDeckSelectionPlayerId("p1");
        state.setPendingDeckSelectionCardIds(List.of("xy1-1"));

        GameAction action = buildAction("p1", GameActionType.END_TURN, Map.of());
        assertThat(validator.validate(state, action).isValid()).isFalse();
    }

    @Test
    void validate_selectFromDeck_shouldSucceed_whenValidCard() {
        BoardState state = buildActiveState("p1");
        state.setPendingDeckSelectionPlayerId("p1");
        state.setPendingDeckSelectionCardIds(new ArrayList<>(List.of("xy1-1", "xy1-2")));

        GameAction action = buildAction("p1", GameActionType.SELECT_FROM_DECK,
                Map.of("chosenCardIds", List.of("xy1-1")));
        assertThat(validator.validate(state, action).isValid()).isTrue();
    }

    @Test
    void validate_selectFromDeck_shouldFail_whenWrongPlayer() {
        BoardState state = buildActiveState("p1");
        state.setPendingDeckSelectionPlayerId("p1");
        state.setPendingDeckSelectionCardIds(List.of("xy1-1"));

        GameAction action = buildAction("p2", GameActionType.SELECT_FROM_DECK,
                Map.of("chosenCardIds", List.of("xy1-1")));
        assertThat(validator.validate(state, action).isValid()).isFalse();
    }
}