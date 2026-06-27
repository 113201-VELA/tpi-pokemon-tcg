package com.pokemon.tcg.engine;

import com.pokemon.tcg.domain.model.card.Card;
import com.pokemon.tcg.domain.model.card.CardType;
import com.pokemon.tcg.domain.model.game.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SetupManagerTest {

    private CoinFlipService coinFlipService;
    private CardLookupPort cardLookupPort;
    private SetupManager setupManager;

    @BeforeEach
    void setUp() {
        coinFlipService = mock(CoinFlipService.class);
        cardLookupPort  = mock(CardLookupPort.class);
        setupManager    = new SetupManager(coinFlipService, cardLookupPort);
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private PlayerState playerWithDeck(List<String> cardIds) {
        return PlayerState.builder()
                .playerId("p1")
                .deck(new ArrayList<>(cardIds))
                .hand(new ArrayList<>())
                .discard(new ArrayList<>())
                .prizes(new ArrayList<>())
                .bench(new ArrayList<>())
                .build();
    }

    private Card basicPokemonCard() {
        return Card.builder()
                .id("basic-1")
                .name("Bulbasaur")
                .supertype(CardType.POKEMON)
                .subtypes(List.of("Basic"))
                .setId("xy1")
                .types(new ArrayList<>())
                .attacks(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .retreatCost(new ArrayList<>())
                .abilities(new ArrayList<>())
                .build();
    }

    private Card nonBasicCard() {
        return Card.builder()
                .id("energy-1")
                .name("Grass Energy")
                .supertype(CardType.ENERGY)
                .subtypes(List.of("Basic"))
                .setId("xy1")
                .types(new ArrayList<>())
                .attacks(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .retreatCost(new ArrayList<>())
                .abilities(new ArrayList<>())
                .build();
    }

    private BoardState buildState(PlayerState p1, PlayerState p2) {
        return BoardState.builder()
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
    }

    private PlayerState buildPlayer(String playerId, List<String> deck,
                                    int totalMulligans, int bonusDraws) {
        return PlayerState.builder()
                .playerId(playerId)
                .deck(new ArrayList<>(deck))
                .hand(new ArrayList<>())
                .discard(new ArrayList<>())
                .prizes(new ArrayList<>())
                .bench(new ArrayList<>())
                .totalMulligans(totalMulligans)
                .mulliganBonusDraws(bonusDraws)
                .build();
    }

    // ─── drawInitialHand ──────────────────────────────────────────────────────

    @Test
    void drawInitialHand_shouldDraw7Cards() {
        List<String> deck = new ArrayList<>();
        for (int i = 0; i < 20; i++) deck.add("card-" + i);
        PlayerState ps = playerWithDeck(deck);

        setupManager.drawInitialHand(ps);

        assertThat(ps.getHand()).hasSize(7);
        assertThat(ps.getDeck()).hasSize(13);
    }

    @Test
    void drawInitialHand_shouldDrawFromTopOfDeck() {
        PlayerState ps = playerWithDeck(List.of("a", "b", "c", "d", "e", "f", "g", "h"));

        setupManager.drawInitialHand(ps);

        assertThat(ps.getHand()).containsExactly("a", "b", "c", "d", "e", "f", "g");
        assertThat(ps.getDeck()).containsExactly("h");
    }

    @Test
    void drawInitialHand_shouldDrawAllIfDeckHasLessThan7() {
        PlayerState ps = playerWithDeck(List.of("a", "b", "c"));

        setupManager.drawInitialHand(ps);

        assertThat(ps.getHand()).hasSize(3);
        assertThat(ps.getDeck()).isEmpty();
    }

    @Test
    void drawInitialHand_shouldReturnTrueWhenHandHasBasicPokemon() {
        when(cardLookupPort.findCardById("basic-1")).thenReturn(Optional.of(basicPokemonCard()));
        when(cardLookupPort.findCardById("energy-1")).thenReturn(Optional.of(nonBasicCard()));
        when(cardLookupPort.findCardById("energy-2")).thenReturn(Optional.of(nonBasicCard()));
        when(cardLookupPort.findCardById("energy-3")).thenReturn(Optional.of(nonBasicCard()));
        when(cardLookupPort.findCardById("energy-4")).thenReturn(Optional.of(nonBasicCard()));
        when(cardLookupPort.findCardById("energy-5")).thenReturn(Optional.of(nonBasicCard()));
        when(cardLookupPort.findCardById("energy-6")).thenReturn(Optional.of(nonBasicCard()));

        // basic-1 is the 3rd card drawn
        PlayerState ps = playerWithDeck(
                List.of("energy-1", "energy-2", "basic-1",
                        "energy-3", "energy-4", "energy-5", "energy-6", "extra"));

        boolean result = setupManager.drawInitialHand(ps);

        assertThat(result).isTrue();
    }

    @Test
    void drawInitialHand_shouldReturnFalseWhenHandHasNoBasicPokemon() {
        when(cardLookupPort.findCardById(any())).thenReturn(Optional.of(nonBasicCard()));

        List<String> deck = new ArrayList<>();
        for (int i = 0; i < 10; i++) deck.add("energy-" + i);
        PlayerState ps = playerWithDeck(deck);

        boolean result = setupManager.drawInitialHand(ps);

        assertThat(result).isFalse();
    }

    // ─── setupPrizes ──────────────────────────────────────────────────────────

    @Test
    void setupPrizes_shouldTakeFirst6CardsFromDeck() {
        List<String> deck = new ArrayList<>();
        for (int i = 0; i < 20; i++) deck.add("card-" + i);
        PlayerState ps = playerWithDeck(deck);

        setupManager.setupPrizes(ps);

        assertThat(ps.getPrizes()).hasSize(6);
        assertThat(ps.getPrizes()).containsExactly(
                "card-0", "card-1", "card-2", "card-3", "card-4", "card-5");
        assertThat(ps.getDeck()).hasSize(14);
    }

    @Test
    void setupPrizes_shouldTakeAllIfDeckHasLessThan6() {
        PlayerState ps = playerWithDeck(List.of("a", "b", "c"));

        setupManager.setupPrizes(ps);

        assertThat(ps.getPrizes()).containsExactly("a", "b", "c");
        assertThat(ps.getDeck()).isEmpty();
    }

    // ─── determineFirstPlayer ─────────────────────────────────────────────────

    @Test
    void determineFirstPlayer_shouldReturnPlayer1OnHeads() {
        when(coinFlipService.flip()).thenReturn(CoinResult.HEADS);

        assertThat(setupManager.determineFirstPlayer("p1", "p2")).isEqualTo("p1");
    }

    @Test
    void determineFirstPlayer_shouldReturnPlayer2OnTails() {
        when(coinFlipService.flip()).thenReturn(CoinResult.TAILS);

        assertThat(setupManager.determineFirstPlayer("p1", "p2")).isEqualTo("p2");
    }

    // ─── shuffleDeck ──────────────────────────────────────────────────────────

    @Test
    void shuffleDeck_shouldPreserveAllCards() {
        List<String> original = new ArrayList<>();
        for (int i = 0; i < 20; i++) original.add("card-" + i);
        PlayerState ps = playerWithDeck(original);

        setupManager.shuffleDeck(ps);

        assertThat(ps.getDeck()).hasSize(20);
        assertThat(ps.getDeck()).containsExactlyInAnyOrderElementsOf(original);
    }

    // ─── hasBasicPokemonInHand ────────────────────────────────────────────────

    @Test
    void hasBasicPokemonInHand_shouldReturnTrueWhenBasicPresent() {
        when(cardLookupPort.findCardById("basic-1")).thenReturn(Optional.of(basicPokemonCard()));
        PlayerState ps = playerWithDeck(List.of());
        ps.setHand(new ArrayList<>(List.of("basic-1")));

        assertThat(setupManager.hasBasicPokemonInHand(ps)).isTrue();
    }

    @Test
    void hasBasicPokemonInHand_shouldReturnFalseWhenNoBasicPresent() {
        when(cardLookupPort.findCardById(any())).thenReturn(Optional.of(nonBasicCard()));
        PlayerState ps = playerWithDeck(List.of());
        ps.setHand(new ArrayList<>(List.of("energy-1", "energy-2")));

        assertThat(setupManager.hasBasicPokemonInHand(ps)).isFalse();
    }

    @Test
    void hasBasicPokemonInHand_shouldReturnFalseWhenHandIsEmpty() {
        PlayerState ps = playerWithDeck(List.of());
        ps.setHand(new ArrayList<>());

        assertThat(setupManager.hasBasicPokemonInHand(ps)).isFalse();
    }

    // ─── handleMulligan ───────────────────────────────────────────────────────

    @Test
    void handleMulligan_shouldShuffleHandBackAndRedraw() {
        when(cardLookupPort.findCardById(any())).thenReturn(Optional.of(nonBasicCard()));

        List<String> fullDeck = new ArrayList<>();
        for (int i = 0; i < 20; i++) fullDeck.add("card-" + i);

        PlayerState p1 = buildPlayer("p1", fullDeck.subList(7, 20), 0, 0);
        p1.setHand(new ArrayList<>(fullDeck.subList(0, 7)));

        PlayerState p2 = buildPlayer("p2", List.of(), 0, 0);
        BoardState state = buildState(p1, p2);

        setupManager.handleMulligan(state, "p1");

        assertThat(p1.getHand()).hasSize(7);
        assertThat(p1.getDeck()).hasSize(13);
        assertThat(p1.getTotalMulligans()).isEqualTo(1);
    }

    @Test
    void handleMulligan_shouldGrantBonusDrawsToOpponent() {
        when(cardLookupPort.findCardById(any())).thenReturn(Optional.of(nonBasicCard()));

        List<String> deck = new ArrayList<>();
        for (int i = 0; i < 13; i++) deck.add("card-" + i);

        PlayerState p1 = buildPlayer("p1", deck, 0, 0);
        PlayerState p2 = buildPlayer("p2", List.of(), 0, 0);
        BoardState state = buildState(p1, p2);

        setupManager.handleMulligan(state, "p1");

        assertThat(p2.getMulliganBonusDraws()).isEqualTo(1);
        assertThat(p1.getMulliganBonusDraws()).isEqualTo(0);
    }

    @Test
    void handleMulligan_simultaneousMulligans_shouldCancelOut() {
        when(cardLookupPort.findCardById(any())).thenReturn(Optional.of(nonBasicCard()));

        List<String> deck = new ArrayList<>();
        for (int i = 0; i < 13; i++) deck.add("card-" + i);

        // Both players have 1 mulligan each — p1 mulligans again (2 vs 1)
        PlayerState p1 = buildPlayer("p1", deck, 1, 0);
        PlayerState p2 = buildPlayer("p2", List.of(), 1, 0);
        BoardState state = buildState(p1, p2);

        setupManager.handleMulligan(state, "p1");

        // p1 now has 2, p2 has 1 → p2 gets net 1 bonus
        assertThat(p2.getMulliganBonusDraws()).isEqualTo(1);
        assertThat(p1.getMulliganBonusDraws()).isEqualTo(0);
    }

    // ─── applyMulliganBonusDraws ──────────────────────────────────────────────

    @Test
    void applyMulliganBonusDraws_shouldDrawRequestedCards() {
        PlayerState ps = PlayerState.builder()
                .playerId("p1")
                .deck(new ArrayList<>(List.of("a", "b", "c", "d", "e")))
                .hand(new ArrayList<>(List.of("x", "y")))
                .discard(new ArrayList<>())
                .prizes(new ArrayList<>())
                .bench(new ArrayList<>())
                .mulliganBonusDraws(3)
                .build();

        setupManager.applyMulliganBonusDraws(ps, 2);

        assertThat(ps.getHand()).hasSize(4);
        assertThat(ps.getDeck()).hasSize(3);
        assertThat(ps.getMulliganBonusDraws()).isEqualTo(0);
    }

    @Test
    void applyMulliganBonusDraws_shouldDrawZeroWhenPlayerDeclines() {
        PlayerState ps = PlayerState.builder()
                .playerId("p1")
                .deck(new ArrayList<>(List.of("a", "b", "c")))
                .hand(new ArrayList<>(List.of("x")))
                .discard(new ArrayList<>())
                .prizes(new ArrayList<>())
                .bench(new ArrayList<>())
                .mulliganBonusDraws(2)
                .build();

        setupManager.applyMulliganBonusDraws(ps, 0);

        assertThat(ps.getHand()).hasSize(1);
        assertThat(ps.getMulliganBonusDraws()).isEqualTo(0);
    }

    @Test
    void applyMulliganBonusDraws_shouldNotExceedDeckSize() {
        PlayerState ps = PlayerState.builder()
                .playerId("p1")
                .deck(new ArrayList<>(List.of("a")))
                .hand(new ArrayList<>())
                .discard(new ArrayList<>())
                .prizes(new ArrayList<>())
                .bench(new ArrayList<>())
                .mulliganBonusDraws(3)
                .build();

        setupManager.applyMulliganBonusDraws(ps, 3);

        assertThat(ps.getHand()).hasSize(1);
        assertThat(ps.getDeck()).isEmpty();
        assertThat(ps.getMulliganBonusDraws()).isEqualTo(0);
    }
}