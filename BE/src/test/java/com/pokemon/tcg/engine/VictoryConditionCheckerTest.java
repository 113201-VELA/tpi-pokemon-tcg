package com.pokemon.tcg.engine;

import com.pokemon.tcg.domain.model.game.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class VictoryConditionCheckerTest {

    private VictoryConditionChecker checker;

    @BeforeEach
    void setUp() {
        checker = new VictoryConditionChecker();
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private PlayerState playerWithDeck(String playerId, List<String> deck) {
        return PlayerState.builder()
                .playerId(playerId)
                .deck(deck)
                .hand(new ArrayList<>())
                .prizes(List.of("p1", "p2", "p3", "p4", "p5", "p6"))
                .activePokemon(buildActive())
                .bench(new ArrayList<>())
                .build();
    }

    private ActivePokemon buildActive() {
        return ActivePokemon.builder()
                .instanceId("inst-1")
                .cardId("xy1-6")
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .damageCounters(0)
                .conditions(new java.util.HashSet<>())
                .build();
    }

    private BoardState buildState(PlayerState p1, PlayerState p2,
                                  String currentPlayerId, TurnPhase phase) {
        return BoardState.builder()
                .gameId("game-1")
                .gameState(GameState.ACTIVE)
                .turnPhase(phase)
                .currentPlayerId(currentPlayerId)
                .turnNumber(5)
                .player1State(p1)
                .player2State(p2)
                .turnFlags(TurnFlags.fresh())
                .pendingEvents(new ArrayList<>())
                .build();
    }

    // ─── condición 1: premios ─────────────────────────────────────────────────

    @Test
    void shouldDetectWinByPrizes() {
        PlayerState winner = PlayerState.builder()
                .playerId("p1")
                .deck(List.of("xy1-1"))
                .hand(new ArrayList<>())
                .prizes(new ArrayList<>())   // sin premios = ganó
                .activePokemon(buildActive())
                .bench(new ArrayList<>())
                .build();
        PlayerState loser = playerWithDeck("p2", List.of("xy1-1"));

        BoardState state = buildState(winner, loser, "p1", TurnPhase.MAIN);

        Optional<GameEvent> result = checker.check(state);

        assertThat(result).isPresent();
        assertThat(result.get().getType()).isEqualTo(GameEventType.GAME_OVER);
        assertThat(result.get().getPlayerId()).isEqualTo("p1");
    }

    // ─── condición 2: sin pokémon ─────────────────────────────────────────────

    @Test
    void shouldDetectWinByOpponentHasNoPokemon() {
        PlayerState winner = playerWithDeck("p1", List.of("xy1-1"));
        PlayerState loser = PlayerState.builder()
                .playerId("p2")
                .deck(List.of("xy1-1"))
                .hand(new ArrayList<>())
                .prizes(List.of("p1"))
                .activePokemon(null)         // sin activo
                .bench(new ArrayList<>())    // sin banca
                .build();

        BoardState state = buildState(winner, loser, "p1", TurnPhase.MAIN);

        Optional<GameEvent> result = checker.check(state);

        assertThat(result).isPresent();
        assertThat(result.get().getType()).isEqualTo(GameEventType.GAME_OVER);
        assertThat(result.get().getPlayerId()).isEqualTo("p1");
    }

    // ─── condición 3: mazo vacío ──────────────────────────────────────────────

    @Test
    void shouldDetectWinWhenOpponentDrawsFromEmptyDeck() {
        // El jugador 2 intenta robar pero su mazo ya está vacío (DRAW phase)
        PlayerState winner = playerWithDeck("p1", List.of("xy1-1"));
        PlayerState loser  = playerWithDeck("p2", new ArrayList<>());  // mazo vacío

        // Es el turno de p2 (el que no puede robar) en fase DRAW
        BoardState state = buildState(winner, loser, "p2", TurnPhase.DRAW);

        Optional<GameEvent> result = checker.check(state);

        assertThat(result).isPresent();
        assertThat(result.get().getType()).isEqualTo(GameEventType.GAME_OVER);
        assertThat(result.get().getPlayerId()).isEqualTo("p1");
    }

    @Test
    void shouldNotTriggerDeckOutWhenPlayerJustDrewLastCard() {
        // El jugador 2 acaba de robar su última carta — mazo vacío pero en fase MAIN
        // No debe perder hasta su próximo turno
        PlayerState p1 = playerWithDeck("p1", List.of("xy1-1"));
        PlayerState p2 = playerWithDeck("p2", new ArrayList<>());  // mazo vacío

        // Misma situación pero en MAIN (ya robó, ahora está jugando su turno)
        BoardState state = buildState(p1, p2, "p2", TurnPhase.MAIN);

        Optional<GameEvent> result = checker.check(state);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldNotTriggerDeckOutForOpponentDuringTheirTurn() {
        // El mazo del jugador 1 está vacío pero es el turno del jugador 2
        // No debe perder hasta que sea su propio turno en DRAW
        PlayerState p1 = playerWithDeck("p1", new ArrayList<>());  // mazo vacío
        PlayerState p2 = playerWithDeck("p2", List.of("xy1-1"));

        // Es el turno de p2, no de p1
        BoardState state = buildState(p1, p2, "p2", TurnPhase.DRAW);

        Optional<GameEvent> result = checker.check(state);

        assertThat(result).isEmpty();
    }

    // ─── muerte súbita ────────────────────────────────────────────────────────

    @Test
    void shouldDetectSuddenDeathWhenBothWinSimultaneously() {
        // Ambos sin premios al mismo tiempo
        PlayerState p1 = PlayerState.builder()
                .playerId("p1")
                .deck(List.of("xy1-1"))
                .hand(new ArrayList<>())
                .prizes(new ArrayList<>())
                .activePokemon(buildActive())
                .bench(new ArrayList<>())
                .build();
        PlayerState p2 = PlayerState.builder()
                .playerId("p2")
                .deck(List.of("xy1-1"))
                .hand(new ArrayList<>())
                .prizes(new ArrayList<>())
                .activePokemon(buildActive())
                .bench(new ArrayList<>())
                .build();

        BoardState state = buildState(p1, p2, "p1", TurnPhase.MAIN);

        Optional<GameEvent> result = checker.check(state);

        assertThat(result).isPresent();
        assertThat(result.get().getType()).isEqualTo(GameEventType.SUDDEN_DEATH_STARTED);
    }

    // ─── setup ignorado ───────────────────────────────────────────────────────

    @Test
    void shouldNotEvaluateConditionsDuringSetup() {
        PlayerState p1 = PlayerState.builder()
                .playerId("p1")
                .deck(new ArrayList<>())
                .hand(new ArrayList<>())
                .prizes(new ArrayList<>())
                .activePokemon(null)
                .bench(new ArrayList<>())
                .build();
        PlayerState p2 = playerWithDeck("p2", List.of("xy1-1"));

        BoardState state = BoardState.builder()
                .gameId("game-1")
                .gameState(GameState.SETUP)
                .turnPhase(TurnPhase.SETUP)
                .currentPlayerId("p1")
                .turnNumber(0)
                .player1State(p1)
                .player2State(p2)
                .turnFlags(TurnFlags.fresh())
                .pendingEvents(new ArrayList<>())
                .build();

        Optional<GameEvent> result = checker.check(state);

        assertThat(result).isEmpty();
    }

    // ─── sin ganador ─────────────────────────────────────────────────────────

    @Test
    void shouldReturnEmptyWhenGameContinues() {
        PlayerState p1 = playerWithDeck("p1", List.of("xy1-1"));
        PlayerState p2 = playerWithDeck("p2", List.of("xy1-2"));

        BoardState state = buildState(p1, p2, "p1", TurnPhase.MAIN);

        Optional<GameEvent> result = checker.check(state);

        assertThat(result).isEmpty();
    }
}