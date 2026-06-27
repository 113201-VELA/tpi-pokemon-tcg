package com.pokemon.tcg.engine;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.state.GameStateHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GameEngineFacadeImplTest {

    private SetupManager setupManager;
    private TurnManager turnManager;
    private RuleValidator ruleValidator;
    private VictoryConditionChecker victoryChecker;
    private StatusEffectManager statusEffectManager;
    private GameStateHandler activeHandler;
    private GameEngineFacadeImpl facade;

    @BeforeEach
    void setUp() {
        setupManager        = mock(SetupManager.class);
        turnManager         = mock(TurnManager.class);
        ruleValidator       = mock(RuleValidator.class);
        victoryChecker      = mock(VictoryConditionChecker.class);
        statusEffectManager = mock(StatusEffectManager.class);
        activeHandler       = mock(GameStateHandler.class);

        when(activeHandler.getState()).thenReturn(GameState.ACTIVE);
        when(victoryChecker.check(any())).thenReturn(java.util.Optional.empty());

        facade = new GameEngineFacadeImpl(
                setupManager, turnManager, ruleValidator,
                victoryChecker, statusEffectManager,
                List.of(activeHandler));
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private PlayerState buildPlayer(String playerId) {
        return PlayerState.builder()
                .playerId(playerId)
                .deck(new ArrayList<>(List.of("c1","c2","c3","c4","c5",
                        "c6","c7","c8","c9","c10","c11","c12","c13")))
                .hand(new ArrayList<>())
                .discard(new ArrayList<>())
                .prizes(new ArrayList<>())
                .bench(new ArrayList<>())
                .build();
    }

    private BoardState buildActiveState() {
        PlayerState p1 = buildPlayer("p1");
        PlayerState p2 = buildPlayer("p2");
        return BoardState.builder()
                .gameId("game-1")
                .gameState(GameState.ACTIVE)
                .turnPhase(TurnPhase.MAIN)
                .currentPlayerId("p1")
                .turnNumber(2)
                .firstPlayerId("p1")
                .player1State(p1)
                .player2State(p2)
                .turnFlags(TurnFlags.fresh())
                .pendingEvents(new ArrayList<>())
                .build();
    }

    private GameAction buildAction(String playerId, GameActionType type) {
        return GameAction.builder()
                .playerId(playerId)
                .type(type)
                .payload(Map.of())
                .build();
    }

    // ─── initializeGame ───────────────────────────────────────────────────────

    @Test
    void initializeGame_shouldShuffleAndDealForBothPlayers() {
        when(setupManager.determineFirstPlayer(any(), any())).thenReturn("p1");

        PlayerState p1 = buildPlayer("p1");
        PlayerState p2 = buildPlayer("p2");

        EngineResult result = facade.initializeGame("game-1", p1, p2);

        verify(setupManager).shuffleDeck(p1);
        verify(setupManager).shuffleDeck(p2);
        verify(setupManager).drawInitialHand(p1);
        verify(setupManager).drawInitialHand(p2);
        verify(setupManager).setupPrizes(p1);
        verify(setupManager).setupPrizes(p2);
    }

    @Test
    void initializeGame_shouldSetFirstPlayerFromCoinFlip() {
        when(setupManager.determineFirstPlayer("p1", "p2")).thenReturn("p2");

        PlayerState p1 = buildPlayer("p1");
        PlayerState p2 = buildPlayer("p2");

        EngineResult result = facade.initializeGame("game-1", p1, p2);

        assertThat(result.newState().getCurrentPlayerId()).isEqualTo("p2");
        assertThat(result.newState().getFirstPlayerId()).isEqualTo("p2");
    }

    @Test
    void initializeGame_shouldReturnSetupState() {
        when(setupManager.determineFirstPlayer(any(), any())).thenReturn("p1");

        PlayerState p1 = buildPlayer("p1");
        PlayerState p2 = buildPlayer("p2");

        EngineResult result = facade.initializeGame("game-1", p1, p2);

        assertThat(result.newState().getGameState()).isEqualTo(GameState.SETUP);
        assertThat(result.newState().getTurnPhase()).isEqualTo(TurnPhase.SETUP);
        assertThat(result.newState().getTurnNumber()).isEqualTo(0);
    }

    @Test
    void initializeGame_shouldEmitGameStartedEvent() {
        when(setupManager.determineFirstPlayer(any(), any())).thenReturn("p1");

        PlayerState p1 = buildPlayer("p1");
        PlayerState p2 = buildPlayer("p2");

        EngineResult result = facade.initializeGame("game-1", p1, p2);

        assertThat(result.events()).hasSize(1);
        assertThat(result.events().get(0).getType()).isEqualTo(GameEventType.GAME_STARTED);
    }

    // ─── processAction — validation ───────────────────────────────────────────

    @Test
    void processAction_shouldReturnErrorEventWhenValidationFails() {
        BoardState state = buildActiveState();
        GameAction action = buildAction("p1", GameActionType.END_TURN);

        when(ruleValidator.validate(state, action))
                .thenReturn(ValidationResult.fail("Not your turn."));

        EngineResult result = facade.processAction(state, action);

        assertThat(result.events()).hasSize(1);
        assertThat(result.events().get(0).getType()).isEqualTo(GameEventType.TURN_ENDED);
        assertThat(result.events().get(0).getData()).containsKey("error");
        assertThat(result.newState()).isSameAs(state);
    }

    @Test
    void processAction_shouldReturnEmptyResultWhenNoHandlerFound() {
        BoardState state = buildActiveState();
        state = state.toBuilder().gameState(GameState.FINISHED).build();
        GameAction action = buildAction("p1", GameActionType.END_TURN);

        when(ruleValidator.validate(any(), any())).thenReturn(ValidationResult.ok());

        EngineResult result = facade.processAction(state, action);

        assertThat(result.events()).isEmpty();
    }

    // ─── processAction — handler dispatch ─────────────────────────────────────

    @Test
    void processAction_shouldDelegateToCorrectStateHandler() {
        BoardState state = buildActiveState();
        GameAction action = buildAction("p1", GameActionType.END_TURN);

        when(ruleValidator.validate(state, action)).thenReturn(ValidationResult.ok());
        when(activeHandler.handle(state, action))
                .thenReturn(EngineResult.of(state, List.of()));

        facade.processAction(state, action);

        verify(activeHandler).handle(state, action);
    }

    @Test
    void processAction_shouldCollectPendingEventsFromState() {
        BoardState state = buildActiveState();
        GameAction action = buildAction("p1", GameActionType.END_TURN);

        GameEvent pendingEvent = GameEvent.builder()
                .type(GameEventType.CARD_DRAWN)
                .gameId("game-1")
                .turnNumber(2)
                .data(Map.of())
                .occurredAt(java.time.Instant.now())
                .build();

        BoardState stateWithPending = state.toBuilder()
                .pendingEvents(new ArrayList<>(List.of(pendingEvent)))
                .build();

        when(ruleValidator.validate(any(), any())).thenReturn(ValidationResult.ok());
        when(activeHandler.handle(any(), any()))
                .thenReturn(EngineResult.of(stateWithPending, List.of()));

        EngineResult result = facade.processAction(state, action);

        assertThat(result.events()).contains(pendingEvent);
        assertThat(result.newState().getPendingEvents()).isEmpty();
    }

    // ─── processAction — victory check ────────────────────────────────────────

    @Test
    void processAction_shouldDetectVictoryAndTransitionToFinished() {
        BoardState state = buildActiveState();
        GameAction action = buildAction("p1", GameActionType.END_TURN);

        GameEvent victoryEvent = GameEvent.builder()
                .type(GameEventType.GAME_OVER)
                .gameId("game-1")
                .playerId("p1")
                .turnNumber(2)
                .data(Map.of("winnerId", "p1", "reason", "WIN"))
                .occurredAt(java.time.Instant.now())
                .build();

        when(ruleValidator.validate(any(), any())).thenReturn(ValidationResult.ok());
        when(activeHandler.handle(any(), any()))
                .thenReturn(EngineResult.of(state, List.of()));
        when(victoryChecker.check(any())).thenReturn(java.util.Optional.of(victoryEvent));

        EngineResult result = facade.processAction(state, action);

        assertThat(result.events()).contains(victoryEvent);
        assertThat(result.newState().getGameState()).isEqualTo(GameState.FINISHED);
    }

    @Test
    void processAction_shouldNotCheckVictoryWhenAlreadyFinished() {
        BoardState state = buildActiveState().toBuilder()
                .gameState(GameState.FINISHED)
                .build();
        GameAction action = buildAction("p1", GameActionType.END_TURN);

        when(ruleValidator.validate(any(), any())).thenReturn(ValidationResult.ok());

        facade.processAction(state, action);

        verify(victoryChecker, never()).check(any());
    }
}