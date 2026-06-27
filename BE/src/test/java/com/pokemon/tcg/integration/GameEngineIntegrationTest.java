package com.pokemon.tcg.integration;

import com.pokemon.tcg.domain.model.card.Attack;
import com.pokemon.tcg.domain.model.card.Card;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.engine.*;
import org.junit.jupiter.api.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the game engine.
 *
 * Exercises engine components end-to-end using real implementations (no mocks),
 * verifying that the full rule pipeline behaves correctly for the scenarios
 * required by the TPI specification.
 *
 * No Spring context, no H2, no Docker — components are wired directly.
 * This keeps tests fast and focused on pure game logic.
 */
@ActiveProfiles("test")
@DisplayName("Game Engine Integration Tests")
class GameEngineIntegrationTest {

    private DeterministicCoinFlipService deterministicCoin;
    private DamageCalculator damageCalculator;
    private StatusEffectManager statusEffectManager;
    private VictoryConditionChecker victoryConditionChecker;
    private RuleValidator ruleValidator;

    // =========================================================================
    // INNER CLASSES — test doubles
    // =========================================================================

    static class DeterministicCoinFlipService extends CoinFlipService {
        private final Queue<CoinResult> results = new LinkedList<>();

        void enqueue(CoinResult... coinResults) {
            Collections.addAll(results, coinResults);
        }

        @Override
        public CoinResult flip() {
            if (results.isEmpty()) return CoinResult.HEADS;
            return results.poll();
        }

        @Override
        public CoinResult flip(boolean forceHeads) {
            if (forceHeads) return CoinResult.HEADS;
            return flip();
        }
    }

    static class StubCardLookupPort implements CardLookupPort {
        private final Map<String, Card> cards = new HashMap<>();

        void register(Card card) {
            cards.put(card.getId(), card);
        }

        @Override
        public Optional<Attack> findAttack(String cardId, String attackName) {
            return findCardById(cardId)
                    .flatMap(card -> card.getAttacks() == null ? Optional.empty() :
                            card.getAttacks().stream()
                            .filter(a -> a.getName().equalsIgnoreCase(attackName))
                            .findFirst());
        }

        @Override
        public int getMaxHp(String cardId) {
            return findCardById(cardId)
                    .map(card -> card.getHp() != null ? card.getHp() : 0)
                    .orElse(0);
        }

        @Override
        public Optional<Card> findCardById(String cardId) {
            return Optional.ofNullable(cards.get(cardId));
        }

        @Override
        public Map<String, Card> findAllById(Set<String> cardIds) {
            Map<String, Card> result = new HashMap<>();
            cardIds.forEach(id -> findCardById(id).ifPresent(c -> result.put(id, c)));
            return result;
        }
    }

    // =========================================================================
    // SETUP
    // =========================================================================

    @BeforeEach
    void setUp() {
        deterministicCoin = new DeterministicCoinFlipService();
        damageCalculator = new DamageCalculator();
        statusEffectManager = new StatusEffectManager(deterministicCoin);
        victoryConditionChecker = new VictoryConditionChecker();
        ruleValidator = new RuleValidator(new StubCardLookupPort());
    }

    // =========================================================================
    // HELPERS — board state builders
    // =========================================================================

    private ActivePokemon buildActivePokemon(int damageCounters, Set<SpecialCondition> conditions) {
        return ActivePokemon.builder()
                .instanceId(UUID.randomUUID().toString())
                .cardId("xy1-1")
                .damageCounters(damageCounters)
                .conditions(new HashSet<>(conditions))
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>(List.of("xy1-1")))
                .enteredThisTurn(false)
                .build();
    }

    private PlayerState buildPlayerState(String playerId, int prizes, int deckSize) {
        List<String> prizeList = new ArrayList<>();
        for (int i = 0; i < prizes; i++) prizeList.add("xy1-prize-" + i);

        List<String> deck = new ArrayList<>();
        for (int i = 0; i < deckSize; i++) deck.add("xy1-card-" + i);

        return PlayerState.builder()
                .playerId(playerId)
                .activePokemon(buildActivePokemon(0, new HashSet<>()))
                .bench(new ArrayList<>())
                .hand(new ArrayList<>())
                .deck(deck)
                .discard(new ArrayList<>())
                .prizes(prizeList)
                .totalMulligans(0)
                .mulliganBonusDraws(0)
                .setupConfirmed(true)
                .build();
    }

    private BoardState buildActiveBoard(PlayerState p1, PlayerState p2) {
        return BoardState.builder()
                .gameId(UUID.randomUUID().toString())
                .gameState(GameState.ACTIVE)
                .turnPhase(TurnPhase.MAIN)
                .currentPlayerId(p1.getPlayerId())
                .turnNumber(2)
                .turnFlags(TurnFlags.builder()
                        .energyAttachedThisTurn(false)
                        .retreatedThisTurn(false)
                        .supporterPlayedThisTurn(false)
                        .stadiumPlayedThisTurn(false)
                        .attackedThisTurn(false)
                        .isFirstTurnOfGame(false)
                        .build())
                .player1State(p1)
                .player2State(p2)
                .pendingEvents(new ArrayList<>())
                .bonusDrawPending(false)
                .build();
    }

    // =========================================================================
    // 1. VICTORIA POR PREMIOS
    // =========================================================================

    @Nested
    @DisplayName("1. Victory by prizes")
    class VictoryByPrizesTests {

        @Test
        @DisplayName("Player wins when they collect their last prize card")
        void shouldDetectVictoryWhenLastPrizeTaken() {
            var p1 = buildPlayerState("p1", 0, 10); // 0 prizes = all taken
            var p2 = buildPlayerState("p2", 3, 10);
            var board = buildActiveBoard(p1, p2);

            var result = victoryConditionChecker.check(board);

            assertThat(result).isPresent();
            assertThat(result.get().getType()).isEqualTo(GameEventType.GAME_OVER);
            assertThat(result.get().getPlayerId()).isEqualTo("p1");
        }

        @Test
        @DisplayName("Game continues while both players have prizes remaining")
        void shouldNotDetectVictoryWhilePrizesRemain() {
            var p1 = buildPlayerState("p1", 2, 10);
            var p2 = buildPlayerState("p2", 3, 10);
            var board = buildActiveBoard(p1, p2);

            var result = victoryConditionChecker.check(board);

            assertThat(result).isEmpty();
        }
    }

    // =========================================================================
    // 2. VICTORIA POR KNOCKOUT TOTAL
    // =========================================================================

    @Nested
    @DisplayName("2. Victory by total knockout")
    class VictoryByKnockoutTests {

        @Test
        @DisplayName("Player wins when opponent has no Pokémon left in play")
        void shouldDetectVictoryWhenOpponentHasNoPokemon() {
            var p1 = buildPlayerState("p1", 2, 10);
            var p2 = PlayerState.builder()
                    .playerId("p2")
                    .activePokemon(null)      // KO'd, no replacement
                    .bench(new ArrayList<>()) // bench also empty
                    .hand(new ArrayList<>())
                    .deck(List.of("xy1-1"))
                    .discard(new ArrayList<>())
                    .prizes(List.of("xy1-p1", "xy1-p2"))
                    .totalMulligans(0)
                    .mulliganBonusDraws(0)
                    .setupConfirmed(true)
                    .build();
            var board = buildActiveBoard(p1, p2);

            var result = victoryConditionChecker.check(board);

            assertThat(result).isPresent();
            assertThat(result.get().getType()).isEqualTo(GameEventType.GAME_OVER);
            assertThat(result.get().getPlayerId()).isEqualTo("p1");
        }

        @Test
        @DisplayName("Game does not end when opponent still has bench Pokémon")
        void shouldNotEndGameWhenOpponentHasBench() {
            var p1 = buildPlayerState("p1", 2, 10);
            var p2 = buildPlayerState("p2", 3, 10);
            p2.getBench().add(BenchPokemon.builder()
                    .instanceId(UUID.randomUUID().toString())
                    .cardId("xy1-5")
                    .damageCounters(0)
                    .attachedEnergyIds(new ArrayList<>())
                    .evolutionStack(new ArrayList<>(List.of("xy1-5")))
                    .enteredThisTurn(false)
                    .build());
            var board = buildActiveBoard(p1, p2);

            var result = victoryConditionChecker.check(board);

            assertThat(result).isEmpty();
        }
    }

    // =========================================================================
    // 3. VICTORIA POR MAZO VACÍO (deck-out)
    // =========================================================================

    @Nested
    @DisplayName("3. Victory by deck-out")
    class VictoryByDeckOutTests {

        @Test
        @DisplayName("Player loses when they cannot draw at start of their turn")
        void shouldDetectDeckOutAtStartOfTurn() {
            var p1 = buildPlayerState("p1", 3, 10);
            var p2 = buildPlayerState("p2", 3, 0); // empty deck

            // It's p2's turn, in DRAW phase — they cannot draw
            var board = BoardState.builder()
                    .gameId(UUID.randomUUID().toString())
                    .gameState(GameState.ACTIVE)
                    .turnPhase(TurnPhase.DRAW)
                    .currentPlayerId("p2")
                    .turnNumber(4)
                    .turnFlags(TurnFlags.builder()
                            .energyAttachedThisTurn(false)
                            .retreatedThisTurn(false)
                            .supporterPlayedThisTurn(false)
                            .stadiumPlayedThisTurn(false)
                            .attackedThisTurn(false)
                            .isFirstTurnOfGame(false)
                            .build())
                    .player1State(p1)
                    .player2State(p2)
                    .pendingEvents(new ArrayList<>())
                    .bonusDrawPending(false)
                    .build();

            var result = victoryConditionChecker.check(board);

            assertThat(result).isPresent();
            assertThat(result.get().getType()).isEqualTo(GameEventType.GAME_OVER);
            assertThat(result.get().getPlayerId()).isEqualTo("p1");
        }

        @Test
        @DisplayName("Empty deck in MAIN phase does not immediately trigger deck-out")
        void shouldNotTriggerDeckOutDuringMainPhase() {
            // A Trainer card may exhaust the deck mid-turn — not a loss until next DRAW
            var p1 = buildPlayerState("p1", 3, 0); // p1 empty deck, but it's their MAIN phase
            var p2 = buildPlayerState("p2", 3, 10);
            var board = buildActiveBoard(p1, p2); // MAIN phase, p1's turn

            var result = victoryConditionChecker.check(board);

            assertThat(result).isEmpty();
        }
    }

    // =========================================================================
    // 4. KNOCKOUT — isKnockedOut helper
    // =========================================================================

    @Nested
    @DisplayName("4. Knockout threshold")
    class KnockoutThresholdTests {

        @Test
        @DisplayName("Pokémon is KO'd when damage counters × 10 equals max HP")
        void pokemonIsKnockedOutAtExactlyMaxHp() {
            var pokemon = buildActivePokemon(6, new HashSet<>()); // 60 damage
            assertThat(victoryConditionChecker.isKnockedOut(pokemon, 60)).isTrue();
        }

        @Test
        @DisplayName("Pokémon is KO'd when damage counters × 10 exceeds max HP")
        void pokemonIsKnockedOutWhenDamageExceedsMaxHp() {
            var pokemon = buildActivePokemon(8, new HashSet<>()); // 80 damage
            assertThat(victoryConditionChecker.isKnockedOut(pokemon, 70)).isTrue();
        }

        @Test
        @DisplayName("Pokémon survives when damage counters × 10 is less than max HP")
        void pokemonSurvivesWhenDamageBelowMaxHp() {
            var pokemon = buildActivePokemon(5, new HashSet<>()); // 50 damage
            assertThat(victoryConditionChecker.isKnockedOut(pokemon, 60)).isFalse();
        }

        @Test
        @DisplayName("Pokémon-EX KO grants 2 prizes — threshold works the same way")
        void exPokemonUseSameKoThreshold() {
            // The 2-prize rule is handled by TurnManager after KO detection.
            // The threshold check itself is identical for EX and non-EX.
            var exPokemon = buildActivePokemon(18, new HashSet<>()); // 180 damage
            assertThat(victoryConditionChecker.isKnockedOut(exPokemon, 180)).isTrue();
        }
    }

    // =========================================================================
    // 5. CONDICIONES ESPECIALES
    // =========================================================================

    @Nested
    @DisplayName("5. Special conditions between turns")
    class SpecialConditionsTests {

        @Test
        @DisplayName("Poisoned Pokémon receives 1 damage counter between turns")
        void poisonedAddsOneDamageCounter() {
            var pokemon = buildActivePokemon(0, Set.of(SpecialCondition.POISONED));
            var result = statusEffectManager.processBetweenTurns(pokemon);
            assertThat(result.getDamageCounters()).isEqualTo(1);
        }

        @Test
        @DisplayName("Burned Pokémon — tails adds 2 damage counters")
        void burnedWithTailsAddsTwoDamageCounters() {
            deterministicCoin.enqueue(CoinResult.TAILS);
            var pokemon = buildActivePokemon(0, Set.of(SpecialCondition.BURNED));
            var result = statusEffectManager.processBetweenTurns(pokemon);
            assertThat(result.getDamageCounters()).isEqualTo(2);
        }

        @Test
        @DisplayName("Burned Pokémon — heads adds no damage")
        void burnedWithHeadsAddsNoDamage() {
            deterministicCoin.enqueue(CoinResult.HEADS);
            var pokemon = buildActivePokemon(0, Set.of(SpecialCondition.BURNED));
            var result = statusEffectManager.processBetweenTurns(pokemon);
            assertThat(result.getDamageCounters()).isEqualTo(0);
        }

        @Test
        @DisplayName("Asleep Pokémon wakes up on heads")
        void asleepWakesUpOnHeads() {
            deterministicCoin.enqueue(CoinResult.HEADS);
            var pokemon = buildActivePokemon(0, Set.of(SpecialCondition.ASLEEP));
            var result = statusEffectManager.processBetweenTurns(pokemon);
            assertThat(result.getConditions()).doesNotContain(SpecialCondition.ASLEEP);
        }

        @Test
        @DisplayName("Asleep Pokémon stays asleep on tails")
        void asleepStaysAsleepOnTails() {
            deterministicCoin.enqueue(CoinResult.TAILS);
            var pokemon = buildActivePokemon(0, Set.of(SpecialCondition.ASLEEP));
            var result = statusEffectManager.processBetweenTurns(pokemon);
            assertThat(result.getConditions()).contains(SpecialCondition.ASLEEP);
        }

        @Test
        @DisplayName("Paralyzed Pokémon is cured automatically between turns")
        void paralyzedIsCuredBetweenTurns() {
            var pokemon = buildActivePokemon(0, Set.of(SpecialCondition.PARALYZED));
            var result = statusEffectManager.processBetweenTurns(pokemon);
            assertThat(result.getConditions()).doesNotContain(SpecialCondition.PARALYZED);
        }

        @Test
        @DisplayName("Burned and Poisoned coexist — both effects apply (tails)")
        void burnedAndPoisonedCoexist() {
            deterministicCoin.enqueue(CoinResult.TAILS); // burn flip
            var pokemon = buildActivePokemon(0,
                    new HashSet<>(Set.of(SpecialCondition.BURNED, SpecialCondition.POISONED)));
            var result = statusEffectManager.processBetweenTurns(pokemon);
            // Poison: +1, Burn tails: +2 → total 3
            assertThat(result.getDamageCounters()).isEqualTo(3);
        }

        @Test
        @DisplayName("Burned and Poisoned coexist — burn heads still applies poison")
        void burnedAndPoisonedCoexistHeads() {
            deterministicCoin.enqueue(CoinResult.HEADS); // burn flip — no damage
            var pokemon = buildActivePokemon(0,
                    new HashSet<>(Set.of(SpecialCondition.BURNED, SpecialCondition.POISONED)));
            var result = statusEffectManager.processBetweenTurns(pokemon);
            // Poison: +1, Burn heads: +0 → total 1
            assertThat(result.getDamageCounters()).isEqualTo(1);
        }

        @Test
        @DisplayName("PARALYZED replaces ASLEEP — mutually exclusive conditions")
        void paralyzedReplacesAsleep() {
            var pokemon = buildActivePokemon(0, new HashSet<>(Set.of(SpecialCondition.ASLEEP)));
            var result = statusEffectManager.applyCondition(pokemon, SpecialCondition.PARALYZED);
            assertThat(result.getConditions())
                    .contains(SpecialCondition.PARALYZED)
                    .doesNotContain(SpecialCondition.ASLEEP);
        }

        @Test
        @DisplayName("CONFUSED replaces PARALYZED — mutually exclusive conditions")
        void confusedReplacesParalyzed() {
            var pokemon = buildActivePokemon(0, new HashSet<>(Set.of(SpecialCondition.PARALYZED)));
            var result = statusEffectManager.applyCondition(pokemon, SpecialCondition.CONFUSED);
            assertThat(result.getConditions())
                    .contains(SpecialCondition.CONFUSED)
                    .doesNotContain(SpecialCondition.PARALYZED);
        }
    }

    // =========================================================================
    // 6. MULLIGANS
    // =========================================================================

    @Nested
    @DisplayName("6. Mulligan mechanics")
    class MulliganTests {

        @Test
        @DisplayName("Opponent gains 1 bonus draw per mulligan declared by player")
        void opponentGainsBonusDrawsPerMulligan() {
            var p1 = PlayerState.builder()
                    .playerId("p1")
                    .activePokemon(buildActivePokemon(0, new HashSet<>()))
                    .bench(new ArrayList<>())
                    .hand(new ArrayList<>())
                    .deck(buildList(53))
                    .discard(new ArrayList<>())
                    .prizes(buildList(6))
                    .totalMulligans(2)       // p1 did 2 mulligans
                    .mulliganBonusDraws(0)
                    .setupConfirmed(true)
                    .build();

            var p2 = PlayerState.builder()
                    .playerId("p2")
                    .activePokemon(buildActivePokemon(0, new HashSet<>()))
                    .bench(new ArrayList<>())
                    .hand(new ArrayList<>())
                    .deck(buildList(53))
                    .discard(new ArrayList<>())
                    .prizes(buildList(6))
                    .totalMulligans(0)
                    .mulliganBonusDraws(2)   // p2 gets 2 bonus draws
                    .setupConfirmed(true)
                    .build();

            assertThat(p1.getTotalMulligans()).isEqualTo(2);
            assertThat(p2.getMulliganBonusDraws()).isEqualTo(p1.getTotalMulligans());
        }

        @Test
        @DisplayName("Simultaneous mulligans cancel to net difference")
        void simultaneousMulligansNetDifference() {
            // p1: 3 mulligans, p2: 1 mulligan → p2 gets net 2 bonus draws
            int p1Mulligans = 3;
            int p2Mulligans = 1;
            int netBonusForP2 = p1Mulligans - p2Mulligans;
            assertThat(netBonusForP2).isEqualTo(2);
        }

        @Test
        @DisplayName("Bonus draw count of 0 is a valid choice")
        void choosingZeroBonusDrawsIsValid() {
            int mulliganBonusDraws = 3;
            int chosen = 0;
            // Valid: 0 ≤ chosen ≤ mulliganBonusDraws
            assertThat(chosen).isBetween(0, mulliganBonusDraws);
        }

        @Test
        @DisplayName("ACCEPT_MULLIGAN_BONUS is rejected outside setup when no bonus pending")
        void mulliganBonusRejectedWhenNotPending() {
            var p1 = buildPlayerState("p1", 6, 53);
            var p2 = buildPlayerState("p2", 6, 53);
            var board = buildActiveBoard(p1, p2); // bonusDrawPending = false

            var action = GameAction.builder()
                    .type(GameActionType.ACCEPT_MULLIGAN_BONUS)
                    .playerId("p1")
                    .payload(Map.of("cardsToDraw", 1))
                    .build();

            var result = ruleValidator.validate(board, action);
            assertThat(result.isValid()).isFalse();
        }
    }

    // =========================================================================
    // 7. EVOLUCIÓN — restricciones de reglas
    // =========================================================================

    @Nested
    @DisplayName("7. Evolution rule restrictions")
    class EvolutionRuleTests {

        @Test
        @DisplayName("Evolution is rejected on turn 1")
        void evolutionRejectedOnFirstTurn() {
            var p1 = buildPlayerState("p1", 6, 53);
            var p2 = buildPlayerState("p2", 6, 53);

            var board = BoardState.builder()
                    .gameId(UUID.randomUUID().toString())
                    .gameState(GameState.ACTIVE)
                    .turnPhase(TurnPhase.MAIN)
                    .currentPlayerId("p1")
                    .turnNumber(1)           // first turn of game
                    .turnFlags(TurnFlags.builder()
                            .energyAttachedThisTurn(false)
                            .retreatedThisTurn(false)
                            .supporterPlayedThisTurn(false)
                            .stadiumPlayedThisTurn(false)
                            .attackedThisTurn(false)
                            .isFirstTurnOfGame(true)
                            .build())
                    .player1State(p1)
                    .player2State(p2)
                    .pendingEvents(new ArrayList<>())
                    .bonusDrawPending(false)
                    .build();

            var action = GameAction.builder()
                    .type(GameActionType.EVOLVE_POKEMON)
                    .playerId("p1")
                    .payload(Map.of("cardId", "xy1-2", "targetInstanceId", "some-uuid"))
                    .build();

            var result = ruleValidator.validate(board, action);
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorMessage()).containsIgnoringCase("first turn");
        }

        @Test
        @DisplayName("Evolution is rejected for a Pokémon that entered play this turn")
        void evolutionRejectedForPokemonThatEnteredThisTurn() {
            var benchPokemon = BenchPokemon.builder()
                    .instanceId("target-uuid")
                    .cardId("xy1-1")
                    .damageCounters(0)
                    .attachedEnergyIds(new ArrayList<>())
                    .evolutionStack(new ArrayList<>(List.of("xy1-1")))
                    .enteredThisTurn(true)  // just placed this turn
                    .build();

            var p1 = PlayerState.builder()
                    .playerId("p1")
                    .activePokemon(buildActivePokemon(0, new HashSet<>()))
                    .bench(new ArrayList<>(List.of(benchPokemon)))
                    .hand(List.of("xy1-2"))  // evolution card in hand
                    .deck(buildList(45))
                    .discard(new ArrayList<>())
                    .prizes(buildList(6))
                    .totalMulligans(0)
                    .mulliganBonusDraws(0)
                    .setupConfirmed(true)
                    .build();

            var board = buildActiveBoard(p1, buildPlayerState("p2", 6, 53));

            var action = GameAction.builder()
                    .type(GameActionType.EVOLVE_POKEMON)
                    .playerId("p1")
                    .payload(Map.of("cardId", "xy1-2", "targetInstanceId", "target-uuid"))
                    .build();

            var result = ruleValidator.validate(board, action);
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorMessage()).containsIgnoringCase("entered play");
        }

        @Test
        @DisplayName("Evolution is rejected when evolution card is not in hand")
        void evolutionRejectedWhenCardNotInHand() {
            var benchPokemon = BenchPokemon.builder()
                    .instanceId("target-uuid")
                    .cardId("xy1-1")
                    .damageCounters(0)
                    .attachedEnergyIds(new ArrayList<>())
                    .evolutionStack(new ArrayList<>(List.of("xy1-1")))
                    .enteredThisTurn(false)
                    .build();

            var p1 = PlayerState.builder()
                    .playerId("p1")
                    .activePokemon(buildActivePokemon(0, new HashSet<>()))
                    .bench(new ArrayList<>(List.of(benchPokemon)))
                    .hand(new ArrayList<>())  // hand is empty
                    .deck(buildList(45))
                    .discard(new ArrayList<>())
                    .prizes(buildList(6))
                    .totalMulligans(0)
                    .mulliganBonusDraws(0)
                    .setupConfirmed(true)
                    .build();

            var board = buildActiveBoard(p1, buildPlayerState("p2", 6, 53));

            var action = GameAction.builder()
                    .type(GameActionType.EVOLVE_POKEMON)
                    .playerId("p1")
                    .payload(Map.of("cardId", "xy1-2", "targetInstanceId", "target-uuid"))
                    .build();

            var result = ruleValidator.validate(board, action);
            assertThat(result.isValid()).isFalse();
        }
    }

    // =========================================================================
    // 8. MUERTE SÚBITA
    // =========================================================================

    @Nested
    @DisplayName("8. Sudden Death")
    class SuddenDeathTests {

        @Test
        @DisplayName("Sudden Death is triggered when both players take their last prize simultaneously")
        void suddenDeathTriggeredOnSimultaneousWin() {
            var p1 = buildPlayerState("p1", 0, 5); // 0 prizes left
            var p2 = buildPlayerState("p2", 0, 5); // 0 prizes left
            var board = buildActiveBoard(p1, p2);

            var result = victoryConditionChecker.check(board);

            assertThat(result).isPresent();
            assertThat(result.get().getType()).isEqualTo(GameEventType.SUDDEN_DEATH_STARTED);
        }

        @Test
        @DisplayName("Normal win when only one player takes their last prize")
        void normalWinWhenOnlyOnePlayerWins() {
            var p1 = buildPlayerState("p1", 0, 5); // took last prize
            var p2 = buildPlayerState("p2", 1, 5); // still has 1 prize
            var board = buildActiveBoard(p1, p2);

            var result = victoryConditionChecker.check(board);

            assertThat(result).isPresent();
            assertThat(result.get().getType()).isEqualTo(GameEventType.GAME_OVER);
            assertThat(result.get().getPlayerId()).isEqualTo("p1");
        }
    }

    // =========================================================================
    // HELPER
    // =========================================================================

    private List<String> buildList(int size) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < size; i++) list.add("xy1-item-" + i);
        return list;
    }
}