package com.pokemon.tcg.engine;

import com.pokemon.tcg.domain.model.game.*;
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
        ruleValidator        = mock(RuleValidator.class);
        coinFlipService      = mock(CoinFlipService.class);
        attackPipeline       = mock(AttackPipeline.class);
        statusEffectManager  = mock(StatusEffectManager.class);
        cardLookupPort       = mock(CardLookupPort.class);
        trainerEffectRegistry  = mock(TrainerEffectRegistry.class);
        activeAbilityRegistry  = mock(ActiveAbilityRegistry.class);
        setupManager           = mock(SetupManager.class);

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

    // ─── EVOLVE_POKEMON ───────────────────────────────────────────────────────

    @Test
    void handleEvolvePokemon_shouldUpdateCardIdAndStack() {
        BoardState state = buildState("p1", TurnPhase.MAIN);
        state.getPlayer1State().setHand(new ArrayList<>(List.of("xy1-2")));
        String activeInstanceId = state.getPlayer1State().getActivePokemon().getInstanceId();

        GameAction action = buildAction("p1", GameActionType.EVOLVE_POKEMON,
                Map.of("cardId", "xy1-2", "targetInstanceId", activeInstanceId));
        BoardState result = turnManager.advancePhase(state, action);

        assertThat(result.getPlayer1State().getActivePokemon().getCardId()).isEqualTo("xy1-2");
        assertThat(result.getPlayer1State().getActivePokemon().getEvolutionStack())
                .contains("xy1-1", "xy1-2");
        assertThat(result.getPlayer1State().getHand()).doesNotContain("xy1-2");
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

    // ─── ACCEPT_MULLIGAN_BONUS ────────────────────────────────────────────────

    @Test
    void handleAcceptMulliganBonus_shouldTransitionToDrawWhenNoPendingBonus() {
        BoardState state = buildState("p1", TurnPhase.SETUP);
        state.getPlayer1State().setMulliganBonusDraws(2);
        state.getPlayer2State().setMulliganBonusDraws(0);
        state.setBonusDrawPending(true);

        GameAction action = buildAction("p1", GameActionType.ACCEPT_MULLIGAN_BONUS,
                Map.of("cardsToDraw", 0));

        // After p1 accepts, no more pending bonuses
        doAnswer(inv -> {
            state.getPlayer1State().setMulliganBonusDraws(0);
            return null;
        }).when(setupManager).applyMulliganBonusDraws(any(), anyInt());

        BoardState result = turnManager.advancePhase(state, action);

        assertThat(result.getTurnPhase()).isEqualTo(TurnPhase.DRAW);
        assertThat(result.getGameState()).isEqualTo(GameState.ACTIVE);
        assertThat(result.isBonusDrawPending()).isFalse();
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
    void handleDrawCard_shouldResetEnteredThisTurnForAllPokemon() {
        BoardState state = buildState("p1", TurnPhase.DRAW);
        state.getPlayer1State().setDeck(new ArrayList<>(List.of("card-a")));
        state.getPlayer1State().setHand(new ArrayList<>());

        // Mark active and bench Pokémon as entered this turn
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

    @Test
    void handlePlayTrainer_pokemonTool_shouldAttachAndNotDiscard() {
        BoardState state = buildState("p1", TurnPhase.MAIN);
        state.getPlayer1State().setHand(new ArrayList<>(List.of("xy1-121")));
        String activeInstanceId = state.getPlayer1State().getActivePokemon().getInstanceId();

        // Mock card lookup: xy1-121 is a Pokémon Tool
        com.pokemon.tcg.domain.model.card.Card toolCard =
                com.pokemon.tcg.domain.model.card.Card.builder()
                        .id("xy1-121")
                        .name("Muscle Band")
                        .subtypes(List.of("Pokémon Tool"))
                        .build();
        when(cardLookupPort.findCardById("xy1-121")).thenReturn(Optional.of(toolCard));

        // Mock trainer effect registry: return MuscleBandEffect
        com.pokemon.tcg.domain.strategy.trainer.item.MuscleBandEffect muscleBand =
                new com.pokemon.tcg.domain.strategy.trainer.item.MuscleBandEffect();
        when(trainerEffectRegistry.findEffect("Muscle Band"))
                .thenReturn(Optional.of(muscleBand));

        GameAction action = buildAction("p1", GameActionType.PLAY_TRAINER,
                Map.of("cardId", "xy1-121", "targetInstanceId", activeInstanceId));
        BoardState result = turnManager.advancePhase(state, action);

        // Tool should be attached, not in discard
        assertThat(result.getPlayer1State().getActivePokemon().getAttachedToolId())
                .isEqualTo("xy1-121");
        assertThat(result.getPlayer1State().getDiscard()).doesNotContain("xy1-121");
        assertThat(result.getPlayer1State().getHand()).doesNotContain("xy1-121");
    }

    @Test
    void handleDrawCard_firstPlayerShouldNotDrawOnTurn1() {
        BoardState state = buildState("p1", TurnPhase.DRAW);
        state = state.toBuilder().turnNumber(1).firstPlayerId("p1").build();
        state.getPlayer1State().setDeck(new ArrayList<>(List.of("card-a", "card-b")));
        state.getPlayer1State().setHand(new ArrayList<>());

        GameAction action = buildAction("p1", GameActionType.DRAW_CARD, Map.of());
        BoardState result = turnManager.advancePhase(state, action);

        // Hand should remain empty — no draw on first turn for first player
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
}