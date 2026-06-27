package com.pokemon.tcg.engine;

import com.pokemon.tcg.domain.model.card.Card;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.ability.ActiveAbilityEffect;
import com.pokemon.tcg.domain.strategy.ability.ActiveAbilityRegistry;
import com.pokemon.tcg.domain.strategy.trainer.TrainerEffectRegistry;
import com.pokemon.tcg.engine.attack.AttackPipeline;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TurnManagerTest {

    private RuleValidator ruleValidator;
    private CoinFlipService coinFlipService;
    private AttackPipeline attackPipeline;
    private StatusEffectManager statusEffectManager;
    private CardLookupPort cardLookupPort;
    private TrainerEffectRegistry trainerEffectRegistry;
    private ActiveAbilityRegistry activeAbilityRegistry;
    private SetupManager setupManager;
    private TurnManager turnManager;

    @BeforeEach
    void setUp() {
        ruleValidator         = mock(RuleValidator.class);
        coinFlipService       = mock(CoinFlipService.class);
        attackPipeline        = mock(AttackPipeline.class);
        statusEffectManager   = mock(StatusEffectManager.class);
        cardLookupPort        = mock(CardLookupPort.class);
        trainerEffectRegistry = mock(TrainerEffectRegistry.class);
        activeAbilityRegistry = mock(ActiveAbilityRegistry.class);
        setupManager          = mock(SetupManager.class);

        turnManager = new TurnManager(
                ruleValidator, coinFlipService, attackPipeline,
                statusEffectManager, cardLookupPort, trainerEffectRegistry,
                activeAbilityRegistry, setupManager);
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private ActivePokemon buildActive(String instanceId) {
        return ActivePokemon.builder()
                .instanceId(instanceId)
                .cardId("xy1-1")
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>(List.of("xy1-1")))
                .damageCounters(0)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();
    }

    private PlayerState buildPlayer(String playerId) {
        return PlayerState.builder()
                .playerId(playerId)
                .deck(new ArrayList<>(List.of("xy1-1", "xy1-2", "xy1-3")))
                .hand(new ArrayList<>())
                .discard(new ArrayList<>())
                .prizes(new ArrayList<>())
                .bench(new ArrayList<>())
                .activePokemon(buildActive("inst-" + playerId))
                .build();
    }

    private BoardState buildState(String currentPlayerId, TurnPhase phase) {
        PlayerState p1 = buildPlayer("p1");
        PlayerState p2 = buildPlayer("p2");
        return BoardState.builder()
                .gameId("game-1")
                .gameState(GameState.ACTIVE)
                .turnPhase(phase)
                .currentPlayerId(currentPlayerId)
                .turnNumber(2)
                .firstPlayerId("p1")
                .player1State(p1)
                .player2State(p2)
                .turnFlags(TurnFlags.fresh())
                .pendingEvents(new ArrayList<>())
                .build();
    }

    private GameAction buildAction(String playerId, GameActionType type,
                                   Map<String, Object> payload) {
        return GameAction.builder()
                .playerId(playerId)
                .type(type)
                .payload(payload)
                .build();
    }

    // ─── DRAW_CARD ────────────────────────────────────────────────────────────

    @Test
    void handleDrawCard_shouldMoveTopCardToHand() {
        BoardState state = buildState("p1", TurnPhase.DRAW);
        state.getPlayer1State().setDeck(new ArrayList<>(List.of("card-a", "card-b", "card-c")));
        state.getPlayer1State().setHand(new ArrayList<>());

        GameAction action = buildAction("p1", GameActionType.DRAW_CARD, Map.of());
        BoardState result = turnManager.advancePhase(state, action);

        assertThat(result.getPlayer1State().getHand()).containsExactly("card-a");
        assertThat(result.getPlayer1State().getDeck()).containsExactly("card-b", "card-c");
        assertThat(result.getTurnPhase()).isEqualTo(TurnPhase.MAIN);
    }

    @Test
    void handleDrawCard_shouldReturnUnchangedWhenDeckEmpty() {
        BoardState state = buildState("p1", TurnPhase.DRAW);
        state.getPlayer1State().setDeck(new ArrayList<>());
        state.getPlayer1State().setHand(new ArrayList<>());

        GameAction action = buildAction("p1", GameActionType.DRAW_CARD, Map.of());
        BoardState result = turnManager.advancePhase(state, action);

        assertThat(result.getPlayer1State().getHand()).isEmpty();
        assertThat(result.getTurnPhase()).isEqualTo(TurnPhase.DRAW);
    }

    @Test
    void handleDrawCard_firstPlayerShouldNotDrawOnTurn0() {
        BoardState state = buildState("p1", TurnPhase.DRAW);
        state = state.toBuilder().turnNumber(0).firstPlayerId("p1").build();
        state.getPlayer1State().setDeck(new ArrayList<>(List.of("card-a", "card-b")));
        state.getPlayer1State().setHand(new ArrayList<>());

        GameAction action = buildAction("p1", GameActionType.DRAW_CARD, Map.of());
        BoardState result = turnManager.advancePhase(state, action);

        assertThat(result.getPlayer1State().getHand()).isEmpty();
        assertThat(result.getPlayer1State().getDeck()).containsExactly("card-a", "card-b");
        assertThat(result.getTurnPhase()).isEqualTo(TurnPhase.MAIN);
    }

    @Test
    void handleDrawCard_secondPlayerShouldDrawNormallyOnTurn2() {
        BoardState state = buildState("p2", TurnPhase.DRAW);
        state = state.toBuilder().turnNumber(2).firstPlayerId("p1").build();
        state.getPlayer2State().setDeck(new ArrayList<>(List.of("card-a", "card-b")));
        state.getPlayer2State().setHand(new ArrayList<>());

        GameAction action = buildAction("p2", GameActionType.DRAW_CARD, Map.of());
        BoardState result = turnManager.advancePhase(state, action);

        assertThat(result.getPlayer2State().getHand()).containsExactly("card-a");
        assertThat(result.getTurnPhase()).isEqualTo(TurnPhase.MAIN);
    }

    @Test
    void handleDrawCard_shouldResetEnteredThisTurnForAllPokemon() {
        BoardState state = buildState("p1", TurnPhase.DRAW);
        state.getPlayer1State().setDeck(new ArrayList<>(List.of("card-a")));
        state.getPlayer1State().setHand(new ArrayList<>());
        state.getPlayer1State().getActivePokemon().setEnteredThisTurn(true);

        BenchPokemon bench = BenchPokemon.builder()
                .instanceId("bench-inst").cardId("xy1-3")
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .enteredThisTurn(true)
                .build();
        state.getPlayer1State().setBench(new ArrayList<>(List.of(bench)));

        GameAction action = buildAction("p1", GameActionType.DRAW_CARD, Map.of());
        BoardState result = turnManager.advancePhase(state, action);

        assertThat(result.getPlayer1State().getActivePokemon().isEnteredThisTurn()).isFalse();
        assertThat(result.getPlayer1State().getBench().get(0).isEnteredThisTurn()).isFalse();
    }

    // ─── PLACE_BASIC_POKEMON ──────────────────────────────────────────────────

    @Test
    void handlePlaceBasicPokemon_shouldAddToBench() {
        BoardState state = buildState("p1", TurnPhase.MAIN);
        state.getPlayer1State().setHand(new ArrayList<>(List.of("xy1-3")));

        GameAction action = buildAction("p1", GameActionType.PLACE_BASIC_POKEMON,
                Map.of("cardId", "xy1-3"));
        BoardState result = turnManager.advancePhase(state, action);

        assertThat(result.getPlayer1State().getBench()).hasSize(1);
        assertThat(result.getPlayer1State().getBench().get(0).getCardId()).isEqualTo("xy1-3");
        assertThat(result.getPlayer1State().getHand()).doesNotContain("xy1-3");
    }

    @Test
    void handlePlaceBasicPokemon_shouldNotAddWhenCardNotInHand() {
        BoardState state = buildState("p1", TurnPhase.MAIN);
        state.getPlayer1State().setHand(new ArrayList<>());

        GameAction action = buildAction("p1", GameActionType.PLACE_BASIC_POKEMON,
                Map.of("cardId", "xy1-3"));
        BoardState result = turnManager.advancePhase(state, action);

        assertThat(result.getPlayer1State().getBench()).isEmpty();
    }

    @Test
    void handlePlaceBasicPokemon_shouldNotAddWhenBenchFull() {
        BoardState state = buildState("p1", TurnPhase.MAIN);
        state.getPlayer1State().setHand(new ArrayList<>(List.of("xy1-3")));

        List<BenchPokemon> fullBench = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            fullBench.add(BenchPokemon.builder()
                    .instanceId("b-" + i).cardId("xy1-1")
                    .attachedEnergyIds(new ArrayList<>())
                    .evolutionStack(new ArrayList<>()).build());
        }
        state.getPlayer1State().setBench(fullBench);

        GameAction action = buildAction("p1", GameActionType.PLACE_BASIC_POKEMON,
                Map.of("cardId", "xy1-3"));
        BoardState result = turnManager.advancePhase(state, action);

        assertThat(result.getPlayer1State().getBench()).hasSize(5);
    }

    // ─── ATTACH_ENERGY ────────────────────────────────────────────────────────

    @Test
    void handleAttachEnergy_shouldAttachToActivePokemon() {
        BoardState state = buildState("p1", TurnPhase.MAIN);
        state.getPlayer1State().setHand(new ArrayList<>(List.of("xy1-132")));
        String activeInstanceId = state.getPlayer1State().getActivePokemon().getInstanceId();

        when(cardLookupPort.findCardById("xy1-132")).thenReturn(Optional.empty());

        GameAction action = buildAction("p1", GameActionType.ATTACH_ENERGY,
                Map.of("cardId", "xy1-132", "targetInstanceId", activeInstanceId));
        BoardState result = turnManager.advancePhase(state, action);

        assertThat(result.getPlayer1State().getActivePokemon().getAttachedEnergyIds())
                .contains("xy1-132");
        assertThat(result.getPlayer1State().getHand()).doesNotContain("xy1-132");
        assertThat(result.getTurnFlags().isEnergyAttachedThisTurn()).isTrue();
    }

    @Test
    void handleAttachEnergy_shouldAttachToBenchPokemon() {
        BoardState state = buildState("p1", TurnPhase.MAIN);
        state.getPlayer1State().setHand(new ArrayList<>(List.of("xy1-132")));

        BenchPokemon bench = BenchPokemon.builder()
                .instanceId("bench-inst").cardId("xy1-3")
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>()).build();
        state.getPlayer1State().setBench(new ArrayList<>(List.of(bench)));

        when(cardLookupPort.findCardById("xy1-132")).thenReturn(Optional.empty());

        GameAction action = buildAction("p1", GameActionType.ATTACH_ENERGY,
                Map.of("cardId", "xy1-132", "targetInstanceId", "bench-inst"));
        turnManager.advancePhase(state, action);

        assertThat(state.getPlayer1State().getBench().get(0).getAttachedEnergyIds())
                .contains("xy1-132");
    }

    @Test
    void handleAttachEnergy_shouldNotAttachWhenAlreadyAttachedThisTurn() {
        BoardState state = buildState("p1", TurnPhase.MAIN);
        state.getTurnFlags().setEnergyAttachedThisTurn(true);
        state.getPlayer1State().setHand(new ArrayList<>(List.of("xy1-132")));

        GameAction action = buildAction("p1", GameActionType.ATTACH_ENERGY,
                Map.of("cardId", "xy1-132",
                        "targetInstanceId", state.getPlayer1State().getActivePokemon().getInstanceId()));
        BoardState result = turnManager.advancePhase(state, action);

        assertThat(result.getPlayer1State().getActivePokemon().getAttachedEnergyIds()).isEmpty();
    }

    @Test
    void handleAttachEnergy_shouldNotAttachWhenCardIdNull() {
        BoardState state = buildState("p1", TurnPhase.MAIN);
        state.getPlayer1State().setHand(new ArrayList<>());

        GameAction action = buildAction("p1", GameActionType.ATTACH_ENERGY,
                Map.of("targetInstanceId", state.getPlayer1State().getActivePokemon().getInstanceId()));
        BoardState result = turnManager.advancePhase(state, action);

        assertThat(result.getPlayer1State().getActivePokemon().getAttachedEnergyIds()).isEmpty();
    }

    // ─── EVOLVE_POKEMON ───────────────────────────────────────────────────────

    @Test
    void handleEvolvePokemon_shouldUpdateCardIdAndStack() {
        BoardState state = buildState("p1", TurnPhase.MAIN);
        state.getPlayer1State().setHand(new ArrayList<>(List.of("xy1-2")));
        String activeInstanceId = state.getPlayer1State().getActivePokemon().getInstanceId();

        when(cardLookupPort.findCardById("xy1-2")).thenReturn(Optional.empty());

        GameAction action = buildAction("p1", GameActionType.EVOLVE_POKEMON,
                Map.of("cardId", "xy1-2", "targetInstanceId", activeInstanceId));
        BoardState result = turnManager.advancePhase(state, action);

        assertThat(result.getPlayer1State().getActivePokemon().getCardId()).isEqualTo("xy1-2");
        assertThat(result.getPlayer1State().getActivePokemon().getEvolutionStack())
                .contains("xy1-1", "xy1-2");
        assertThat(result.getPlayer1State().getHand()).doesNotContain("xy1-2");
    }

    @Test
    void handleEvolvePokemon_shouldClearSpecialConditions() {
        BoardState state = buildState("p1", TurnPhase.MAIN);
        state.getPlayer1State().setHand(new ArrayList<>(List.of("xy1-2")));
        ActivePokemon active = state.getPlayer1State().getActivePokemon();
        active.setConditions(new HashSet<>(Set.of(
                SpecialCondition.POISONED, SpecialCondition.CONFUSED)));
        String activeInstanceId = active.getInstanceId();

        when(cardLookupPort.findCardById("xy1-2")).thenReturn(Optional.empty());

        GameAction action = buildAction("p1", GameActionType.EVOLVE_POKEMON,
                Map.of("cardId", "xy1-2", "targetInstanceId", activeInstanceId));
        BoardState result = turnManager.advancePhase(state, action);

        assertThat(result.getPlayer1State().getActivePokemon().getConditions()).isEmpty();
    }

    @Test
    void handleEvolvePokemon_shouldEvolveTargetOnBench() {
        BoardState state = buildState("p1", TurnPhase.MAIN);
        state.getPlayer1State().setHand(new ArrayList<>(List.of("xy1-2")));

        BenchPokemon bench = BenchPokemon.builder()
                .instanceId("bench-inst").cardId("xy1-1")
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>(List.of("xy1-1"))).build();
        state.getPlayer1State().setBench(new ArrayList<>(List.of(bench)));

        when(cardLookupPort.findCardById("xy1-2")).thenReturn(Optional.empty());

        GameAction action = buildAction("p1", GameActionType.EVOLVE_POKEMON,
                Map.of("cardId", "xy1-2", "targetInstanceId", "bench-inst"));
        BoardState result = turnManager.advancePhase(state, action);

        assertThat(result.getPlayer1State().getBench().get(0).getCardId()).isEqualTo("xy1-2");
        assertThat(result.getPlayer1State().getBench().get(0).getEvolutionStack())
                .contains("xy1-1", "xy1-2");
    }

    @Test
    void handleEvolvePokemon_mega_shouldEndTurnImmediately() {
        BoardState state = buildState("p1", TurnPhase.MAIN);
        state.getPlayer1State().setHand(new ArrayList<>(List.of("xy1-mega")));
        String activeInstanceId = state.getPlayer1State().getActivePokemon().getInstanceId();

        Card megaCard = mock(Card.class);
        when(megaCard.getSubtypes()).thenReturn(List.of("MEGA"));
        when(cardLookupPort.findCardById("xy1-mega")).thenReturn(Optional.of(megaCard));

        GameAction action = buildAction("p1", GameActionType.EVOLVE_POKEMON,
                Map.of("cardId", "xy1-mega", "targetInstanceId", activeInstanceId));
        BoardState result = turnManager.advancePhase(state, action);

        // Turn should have ended and switched to opponent
        assertThat(result.getCurrentPlayerId()).isEqualTo("p2");
        assertThat(result.getTurnPhase()).isEqualTo(TurnPhase.DRAW);
        assertThat(result.getTurnNumber()).isEqualTo(3);
    }

    @Test
    void handleEvolvePokemon_shouldReturnUnchanged_whenCardNotInHand() {
        BoardState state = buildState("p1", TurnPhase.MAIN);
        state.getPlayer1State().setHand(new ArrayList<>());

        GameAction action = buildAction("p1", GameActionType.EVOLVE_POKEMON,
                Map.of("cardId", "xy1-2", "targetInstanceId", "inst-p1"));
        BoardState result = turnManager.advancePhase(state, action);

        // State should not change — card not in hand
        assertThat(result.getPlayer1State().getActivePokemon().getCardId()).isEqualTo("xy1-1");
    }

    // ─── RETREAT ──────────────────────────────────────────────────────────────

    @Test
    void handleRetreat_shouldSwapActiveAndBench() {
        BoardState state = buildState("p1", TurnPhase.MAIN);

        BenchPokemon benchPokemon = BenchPokemon.builder()
                .instanceId("bench-inst").cardId("xy1-3")
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>(List.of("xy1-3"))).build();
        state.getPlayer1State().setBench(new ArrayList<>(List.of(benchPokemon)));

        GameAction action = buildAction("p1", GameActionType.RETREAT,
                Map.of("replacementInstanceId", "bench-inst",
                        "energyCardIdsToDiscard", List.of()));
        BoardState result = turnManager.advancePhase(state, action);

        assertThat(result.getPlayer1State().getActivePokemon().getCardId()).isEqualTo("xy1-3");
        assertThat(result.getPlayer1State().getBench()).hasSize(1);
        assertThat(result.getPlayer1State().getBench().get(0).getCardId()).isEqualTo("xy1-1");
        assertThat(result.getTurnFlags().isRetreatedThisTurn()).isTrue();
    }

    @Test
    void handleRetreat_shouldReturnUnchanged_whenReplacementNull() {
        BoardState state = buildState("p1", TurnPhase.MAIN);

        GameAction action = buildAction("p1", GameActionType.RETREAT,
                new HashMap<>()); // no replacementInstanceId
        BoardState result = turnManager.advancePhase(state, action);

        assertThat(result.getPlayer1State().getActivePokemon().getCardId()).isEqualTo("xy1-1");
        assertThat(result.getTurnFlags().isRetreatedThisTurn()).isFalse();
    }

    @Test
    void handleRetreat_shouldReturnUnchanged_whenReplacementNotOnBench() {
        BoardState state = buildState("p1", TurnPhase.MAIN);

        BenchPokemon bench = BenchPokemon.builder()
                .instanceId("bench-inst").cardId("xy1-3")
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>()).build();
        state.getPlayer1State().setBench(new ArrayList<>(List.of(bench)));

        GameAction action = buildAction("p1", GameActionType.RETREAT,
                Map.of("replacementInstanceId", "wrong-inst",
                        "energyCardIdsToDiscard", List.of()));
        BoardState result = turnManager.advancePhase(state, action);

        assertThat(result.getPlayer1State().getActivePokemon().getCardId()).isEqualTo("xy1-1");
    }

    @Test
    void handleRetreat_shouldDiscardEnergyAsCost() {
        BoardState state = buildState("p1", TurnPhase.MAIN);
        state.getPlayer1State().getActivePokemon()
                .setAttachedEnergyIds(new ArrayList<>(List.of("energy-1", "energy-2")));

        BenchPokemon bench = BenchPokemon.builder()
                .instanceId("bench-inst").cardId("xy1-3")
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>()).build();
        state.getPlayer1State().setBench(new ArrayList<>(List.of(bench)));

        GameAction action = buildAction("p1", GameActionType.RETREAT,
                Map.of("replacementInstanceId", "bench-inst",
                        "energyCardIdsToDiscard", List.of("energy-1")));
        BoardState result = turnManager.advancePhase(state, action);

        assertThat(result.getPlayer1State().getDiscard()).contains("energy-1");
        assertThat(result.getPlayer1State().getActivePokemon().getAttachedEnergyIds())
                .doesNotContain("energy-1");
    }

    // ─── END_TURN ─────────────────────────────────────────────────────────────

    @Test
    void handleEndTurn_shouldSwitchToOpponentAndIncrementTurn() {
        BoardState state = buildState("p1", TurnPhase.MAIN);

        GameAction action = buildAction("p1", GameActionType.END_TURN, Map.of());
        BoardState result = turnManager.advancePhase(state, action);

        assertThat(result.getCurrentPlayerId()).isEqualTo("p2");
        assertThat(result.getTurnPhase()).isEqualTo(TurnPhase.DRAW);
        assertThat(result.getTurnNumber()).isEqualTo(3);
        assertThat(result.getTurnFlags().isEnergyAttachedThisTurn()).isFalse();
    }

    @Test
    void handleEndTurn_shouldProcessBetweenTurnEffects() {
        BoardState state = buildState("p1", TurnPhase.MAIN);

        GameAction action = buildAction("p1", GameActionType.END_TURN, Map.of());
        turnManager.advancePhase(state, action);

        verify(statusEffectManager, times(2)).processBetweenTurns(any());
    }

    // ─── CHOOSE_BENCH_POKEMON ─────────────────────────────────────────────────

    @Test
    void handleChooseBenchPokemon_shouldMakeChosenPokemonActive() {
        BoardState state = buildState("p2", TurnPhase.MAIN);
        state.getPlayer2State().setActivePokemon(null);
        state.setPendingBenchChoicePlayerId("p2");

        BenchPokemon bench = BenchPokemon.builder()
                .instanceId("bench-inst").cardId("xy1-3")
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>(List.of("xy1-3"))).build();
        state.getPlayer2State().setBench(new ArrayList<>(List.of(bench)));

        GameAction action = buildAction("p2", GameActionType.CHOOSE_BENCH_POKEMON,
                Map.of("instanceId", "bench-inst"));
        BoardState result = turnManager.advancePhase(state, action);

        assertThat(result.getPlayer2State().getActivePokemon()).isNotNull();
        assertThat(result.getPlayer2State().getActivePokemon().getCardId()).isEqualTo("xy1-3");
        assertThat(result.getPlayer2State().getBench()).isEmpty();
        assertThat(result.isPendingBenchChoice()).isFalse();
        assertThat(result.getCurrentPlayerId()).isEqualTo("p1");
        assertThat(result.getTurnPhase()).isEqualTo(TurnPhase.DRAW);
    }

    @Test
    void handleChooseBenchPokemon_shouldReturnUnchanged_whenInstanceIdNull() {
        BoardState state = buildState("p2", TurnPhase.MAIN);
        state.setPendingBenchChoicePlayerId("p2");

        BenchPokemon bench = BenchPokemon.builder()
                .instanceId("bench-inst").cardId("xy1-3")
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>()).build();
        state.getPlayer2State().setBench(new ArrayList<>(List.of(bench)));

        GameAction action = buildAction("p2", GameActionType.CHOOSE_BENCH_POKEMON,
                new HashMap<>()); // no instanceId
        BoardState result = turnManager.advancePhase(state, action);

        // State unchanged — invalid choice
        assertThat(result.isPendingBenchChoice()).isTrue();
    }

    @Test
    void handleChooseBenchPokemon_shouldReturnUnchanged_whenInstanceNotOnBench() {
        BoardState state = buildState("p2", TurnPhase.MAIN);
        state.setPendingBenchChoicePlayerId("p2");

        BenchPokemon bench = BenchPokemon.builder()
                .instanceId("bench-inst").cardId("xy1-3")
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>()).build();
        state.getPlayer2State().setBench(new ArrayList<>(List.of(bench)));

        GameAction action = buildAction("p2", GameActionType.CHOOSE_BENCH_POKEMON,
                Map.of("instanceId", "wrong-inst"));
        BoardState result = turnManager.advancePhase(state, action);

        assertThat(result.isPendingBenchChoice()).isTrue();
    }

    // ─── CONFIRM_SETUP ────────────────────────────────────────────────────────

    @Test
    void handleConfirmSetup_shouldMarkPlayerAsConfirmed() {
        BoardState state = buildState("p1", TurnPhase.SETUP);
        state.getPlayer1State().setSetupConfirmed(false);

        GameAction action = buildAction("p1", GameActionType.CONFIRM_SETUP, Map.of());
        BoardState result = turnManager.advancePhase(state, action);

        assertThat(result.getPlayer1State().isSetupConfirmed()).isTrue();
    }

    // ─── SETUP_PLACE_ACTIVE ───────────────────────────────────────────────────

    @Test
    void handleSetupPlaceActive_shouldSetActivePokemon() {
        BoardState state = buildState("p1", TurnPhase.SETUP);
        state.getPlayer1State().setActivePokemon(null);
        state.getPlayer1State().setHand(new ArrayList<>(List.of("xy1-6")));

        GameAction action = buildAction("p1", GameActionType.SETUP_PLACE_ACTIVE,
                Map.of("cardId", "xy1-6"));
        BoardState result = turnManager.advancePhase(state, action);

        assertThat(result.getPlayer1State().getActivePokemon()).isNotNull();
        assertThat(result.getPlayer1State().getActivePokemon().getCardId()).isEqualTo("xy1-6");
        assertThat(result.getPlayer1State().getHand()).doesNotContain("xy1-6");
    }

    @Test
    void handleSetupPlaceActive_shouldReturnUnchanged_whenCardNotInHand() {
        BoardState state = buildState("p1", TurnPhase.SETUP);
        state.getPlayer1State().setActivePokemon(null);
        state.getPlayer1State().setHand(new ArrayList<>());

        GameAction action = buildAction("p1", GameActionType.SETUP_PLACE_ACTIVE,
                Map.of("cardId", "xy1-6"));
        BoardState result = turnManager.advancePhase(state, action);

        assertThat(result.getPlayer1State().getActivePokemon()).isNull();
    }

    // ─── SETUP_PLACE_BENCH ────────────────────────────────────────────────────

    @Test
    void handleSetupPlaceBench_shouldAddToBench() {
        BoardState state = buildState("p1", TurnPhase.SETUP);
        state.getPlayer1State().setHand(new ArrayList<>(List.of("xy1-3")));

        GameAction action = buildAction("p1", GameActionType.SETUP_PLACE_BENCH,
                Map.of("cardId", "xy1-3"));
        BoardState result = turnManager.advancePhase(state, action);

        assertThat(result.getPlayer1State().getBench()).hasSize(1);
        assertThat(result.getPlayer1State().getBench().get(0).getCardId()).isEqualTo("xy1-3");
        assertThat(result.getPlayer1State().getHand()).doesNotContain("xy1-3");
    }

    @Test
    void handleSetupPlaceBench_shouldReturnUnchanged_whenCardNotInHand() {
        BoardState state = buildState("p1", TurnPhase.SETUP);
        state.getPlayer1State().setHand(new ArrayList<>());

        GameAction action = buildAction("p1", GameActionType.SETUP_PLACE_BENCH,
                Map.of("cardId", "xy1-3"));
        BoardState result = turnManager.advancePhase(state, action);

        assertThat(result.getPlayer1State().getBench()).isEmpty();
    }

    @Test
    void handleSetupPlaceBench_shouldReturnUnchanged_whenBenchFull() {
        BoardState state = buildState("p1", TurnPhase.SETUP);
        state.getPlayer1State().setHand(new ArrayList<>(List.of("xy1-3")));

        List<BenchPokemon> fullBench = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            fullBench.add(BenchPokemon.builder()
                    .instanceId("b-" + i).cardId("xy1-1")
                    .attachedEnergyIds(new ArrayList<>())
                    .evolutionStack(new ArrayList<>()).build());
        }
        state.getPlayer1State().setBench(fullBench);

        GameAction action = buildAction("p1", GameActionType.SETUP_PLACE_BENCH,
                Map.of("cardId", "xy1-3"));
        BoardState result = turnManager.advancePhase(state, action);

        assertThat(result.getPlayer1State().getBench()).hasSize(5);
    }

    // ─── MULLIGAN_CONFIRM ─────────────────────────────────────────────────────

    @Test
    void handleMulliganConfirm_shouldDelegateToSetupManager_whenNoBasic() {
        BoardState state = buildState("p1", TurnPhase.SETUP);
        state.getPlayer1State().setActivePokemon(null);
        state.getPlayer1State().setHand(new ArrayList<>(List.of("xy1-132")));

        when(setupManager.hasBasicPokemonInHand(state.getPlayer1State())).thenReturn(false);
        when(setupManager.handleMulligan(state, "p1")).thenReturn(state);

        GameAction action = buildAction("p1", GameActionType.MULLIGAN_CONFIRM, Map.of());
        turnManager.advancePhase(state, action);

        verify(setupManager).handleMulligan(state, "p1");
    }

    @Test
    void handleMulliganConfirm_shouldReturnUnchanged_whenHasBasic() {
        BoardState state = buildState("p1", TurnPhase.SETUP);
        state.getPlayer1State().setActivePokemon(null);

        when(setupManager.hasBasicPokemonInHand(state.getPlayer1State())).thenReturn(true);

        GameAction action = buildAction("p1", GameActionType.MULLIGAN_CONFIRM, Map.of());
        BoardState result = turnManager.advancePhase(state, action);

        verify(setupManager, never()).handleMulligan(any(), any());
        assertThat(result).isSameAs(state);
    }

    // ─── ACCEPT_MULLIGAN_BONUS ────────────────────────────────────────────────

    @Test
    void handleAcceptMulliganBonus_shouldTransitionToDrawWhenNoPendingBonus() {
        BoardState state = buildState("p1", TurnPhase.SETUP);
        state.getPlayer1State().setMulliganBonusDraws(2);
        state.getPlayer2State().setMulliganBonusDraws(0);
        state.setBonusDrawPending(true);

        doAnswer(inv -> {
            state.getPlayer1State().setMulliganBonusDraws(0);
            return null;
        }).when(setupManager).applyMulliganBonusDraws(any(), anyInt());

        GameAction action = buildAction("p1", GameActionType.ACCEPT_MULLIGAN_BONUS,
                Map.of("cardsToDraw", 0));
        BoardState result = turnManager.advancePhase(state, action);

        assertThat(result.getTurnPhase()).isEqualTo(TurnPhase.DRAW);
        assertThat(result.getGameState()).isEqualTo(GameState.ACTIVE);
        assertThat(result.isBonusDrawPending()).isFalse();
    }

    @Test
    void handleAcceptMulliganBonus_shouldStayPendingWhenOtherPlayerStillHasBonus() {
        BoardState state = buildState("p1", TurnPhase.SETUP);
        state.getPlayer1State().setMulliganBonusDraws(2);
        state.getPlayer2State().setMulliganBonusDraws(3); // p2 still has bonus
        state.setBonusDrawPending(true);

        doAnswer(inv -> {
            state.getPlayer1State().setMulliganBonusDraws(0);
            return null;
        }).when(setupManager).applyMulliganBonusDraws(eq(state.getPlayer1State()), anyInt());

        GameAction action = buildAction("p1", GameActionType.ACCEPT_MULLIGAN_BONUS,
                Map.of("cardsToDraw", 1));
        BoardState result = turnManager.advancePhase(state, action);

        assertThat(result.isBonusDrawPending()).isTrue();
        assertThat(result.getTurnPhase()).isEqualTo(TurnPhase.SETUP);
    }

    // ─── PLAY_TRAINER ─────────────────────────────────────────────────────────

    @Test
    void handlePlayTrainer_pokemonTool_shouldAttachAndNotDiscard() {
        BoardState state = buildState("p1", TurnPhase.MAIN);
        state.getPlayer1State().setHand(new ArrayList<>(List.of("xy1-121")));
        String activeInstanceId = state.getPlayer1State().getActivePokemon().getInstanceId();

        Card toolCard = mock(Card.class);
        when(toolCard.getId()).thenReturn("xy1-121");
        when(toolCard.getName()).thenReturn("Muscle Band");
        when(toolCard.getSubtypes()).thenReturn(List.of("Pokémon Tool"));
        when(cardLookupPort.findCardById("xy1-121")).thenReturn(Optional.of(toolCard));

        com.pokemon.tcg.domain.strategy.trainer.item.MuscleBandEffect muscleBand =
                new com.pokemon.tcg.domain.strategy.trainer.item.MuscleBandEffect();
        when(trainerEffectRegistry.findEffect("Muscle Band"))
                .thenReturn(Optional.of(muscleBand));

        GameAction action = buildAction("p1", GameActionType.PLAY_TRAINER,
                Map.of("cardId", "xy1-121", "targetInstanceId", activeInstanceId));
        BoardState result = turnManager.advancePhase(state, action);

        assertThat(result.getPlayer1State().getActivePokemon().getAttachedToolId())
                .isEqualTo("xy1-121");
        assertThat(result.getPlayer1State().getDiscard()).doesNotContain("xy1-121");
        assertThat(result.getPlayer1State().getHand()).doesNotContain("xy1-121");
    }

    @Test
    void handlePlayTrainer_shouldDiscardNonToolTrainer() {
        BoardState state = buildState("p1", TurnPhase.MAIN);
        state.getPlayer1State().setHand(new ArrayList<>(List.of("xy1-126")));

        Card supporterCard = mock(Card.class);
        when(supporterCard.getName()).thenReturn("Professor Sycamore");
        when(supporterCard.getSubtypes()).thenReturn(List.of("Supporter"));
        when(cardLookupPort.findCardById("xy1-126")).thenReturn(Optional.of(supporterCard));
        when(trainerEffectRegistry.findEffect("Professor Sycamore")).thenReturn(Optional.empty());

        GameAction action = buildAction("p1", GameActionType.PLAY_TRAINER,
                Map.of("cardId", "xy1-126"));
        BoardState result = turnManager.advancePhase(state, action);

        assertThat(result.getPlayer1State().getDiscard()).contains("xy1-126");
        assertThat(result.getPlayer1State().getHand()).doesNotContain("xy1-126");
    }

    @Test
    void handlePlayTrainer_shouldReturnUnchanged_whenCardNotInHand() {
        BoardState state = buildState("p1", TurnPhase.MAIN);
        state.getPlayer1State().setHand(new ArrayList<>());

        GameAction action = buildAction("p1", GameActionType.PLAY_TRAINER,
                Map.of("cardId", "xy1-126"));
        BoardState result = turnManager.advancePhase(state, action);

        assertThat(result.getPlayer1State().getDiscard()).isEmpty();
    }

    // ─── USE_ABILITY ──────────────────────────────────────────────────────────

    @Test
    void handleUseAbility_shouldApplyEffect_whenAbilityFound() {
        BoardState state = buildState("p1", TurnPhase.MAIN);
        String instanceId = state.getPlayer1State().getActivePokemon().getInstanceId();

        Card card = mock(Card.class);
        when(card.getName()).thenReturn("Delphox");
        when(cardLookupPort.findCardById("xy1-1")).thenReturn(Optional.of(card));

        ActiveAbilityEffect ability = mock(ActiveAbilityEffect.class);
        when(ability.canApply(any(), any())).thenReturn(ValidationResult.ok());
        when(ability.apply(any(), any())).thenReturn(state);
        when(activeAbilityRegistry.findAbility("Delphox", "Mystical Fire"))
                .thenReturn(Optional.of(ability));

        GameAction action = buildAction("p1", GameActionType.USE_ABILITY,
                Map.of("instanceId", instanceId, "abilityName", "Mystical Fire"));
        turnManager.advancePhase(state, action);

        verify(ability).apply(any(), any());
    }

    @Test
    void handleUseAbility_shouldReturnUnchanged_whenInstanceIdNull() {
        BoardState state = buildState("p1", TurnPhase.MAIN);

        GameAction action = buildAction("p1", GameActionType.USE_ABILITY,
                Map.of("abilityName", "Mystical Fire")); // no instanceId
        BoardState result = turnManager.advancePhase(state, action);

        assertThat(result).isSameAs(state);
        verifyNoInteractions(activeAbilityRegistry);
    }

    @Test
    void handleUseAbility_shouldReturnUnchanged_whenAbilityNameNull() {
        BoardState state = buildState("p1", TurnPhase.MAIN);
        String instanceId = state.getPlayer1State().getActivePokemon().getInstanceId();

        GameAction action = buildAction("p1", GameActionType.USE_ABILITY,
                Map.of("instanceId", instanceId)); // no abilityName
        BoardState result = turnManager.advancePhase(state, action);

        assertThat(result).isSameAs(state);
    }

    @Test
    void handleUseAbility_shouldReturnUnchanged_whenAbilityCannotApply() {
        BoardState state = buildState("p1", TurnPhase.MAIN);
        String instanceId = state.getPlayer1State().getActivePokemon().getInstanceId();

        Card card = mock(Card.class);
        when(card.getName()).thenReturn("Delphox");
        when(cardLookupPort.findCardById("xy1-1")).thenReturn(Optional.of(card));

        ActiveAbilityEffect ability = mock(ActiveAbilityEffect.class);
        when(ability.canApply(any(), any()))
                .thenReturn(ValidationResult.fail("Cannot use now"));
        when(activeAbilityRegistry.findAbility("Delphox", "Mystical Fire"))
                .thenReturn(Optional.of(ability));

        GameAction action = buildAction("p1", GameActionType.USE_ABILITY,
                Map.of("instanceId", instanceId, "abilityName", "Mystical Fire"));
        turnManager.advancePhase(state, action);

        verify(ability, never()).apply(any(), any());
    }

    // ─── DECLARE_ATTACK (pipeline interactions) ───────────────────────────────

    @Test
    void handleDeclareAttack_shouldReturnErrorEvent_whenPipelineCancelled() {
        BoardState state = buildState("p1", TurnPhase.MAIN);

        // Pipeline cancels the attack (e.g. confusion flip fails)
        doAnswer(inv -> {
            com.pokemon.tcg.domain.strategy.attack.AttackContext ctx =
                    inv.getArgument(0);
            ctx.setCancelled(true);
            ctx.setCancellationReason("Confused self-hit");
            return null;
        }).when(attackPipeline).execute(any());

        when(cardLookupPort.findAttack(anyString(), anyString())).thenReturn(Optional.empty());
        when(cardLookupPort.getMaxHp(anyString())).thenReturn(100);

        GameAction action = buildAction("p1", GameActionType.DECLARE_ATTACK,
                Map.of("attackName", "Tackle"));
        BoardState result = turnManager.advancePhase(state, action);

        // Turn should NOT switch — error event added to pending
        assertThat(result.getCurrentPlayerId()).isEqualTo("p1");
        assertThat(result.getPendingEvents()).isNotEmpty();
        assertThat(result.getPendingEvents().get(0).getType())
                .isEqualTo(GameEventType.TURN_ENDED);
        assertThat(result.getPendingEvents().get(0).getData())
                .containsKey("error");
    }

    @Test
    void handleDeclareAttack_shouldNotSwitchTurn_whenBenchChoicePending() {
        BoardState state = buildState("p1", TurnPhase.MAIN);

        // Pipeline KO'd the defender who has bench Pokémon
        doAnswer(inv -> {
            com.pokemon.tcg.domain.strategy.attack.AttackContext ctx =
                    inv.getArgument(0);
            BoardState boardState = ctx.getBoardState();
            // Simulate KO triggering bench choice
            ctx.setBoardState(boardState.toBuilder()
                    .pendingBenchChoicePlayerId("p2")
                    .build());
            return null;
        }).when(attackPipeline).execute(any());

        when(cardLookupPort.findAttack(anyString(), anyString())).thenReturn(Optional.empty());
        when(cardLookupPort.getMaxHp(anyString())).thenReturn(100);

        GameAction action = buildAction("p1", GameActionType.DECLARE_ATTACK,
                Map.of("attackName", "Tackle"));
        BoardState result = turnManager.advancePhase(state, action);

        // Turn should NOT switch yet — waiting for p2 to choose
        assertThat(result.getCurrentPlayerId()).isEqualTo("p1");
        assertThat(result.isPendingBenchChoice()).isTrue();
    }

    @Test
    void handleDeclareAttack_shouldReturnUnchanged_whenAttackNameNull() {
        BoardState state = buildState("p1", TurnPhase.MAIN);

        GameAction action = buildAction("p1", GameActionType.DECLARE_ATTACK,
                new HashMap<>()); // no attackName
        BoardState result = turnManager.advancePhase(state, action);

        assertThat(result.getCurrentPlayerId()).isEqualTo("p1");
        verifyNoInteractions(attackPipeline);
    }
}