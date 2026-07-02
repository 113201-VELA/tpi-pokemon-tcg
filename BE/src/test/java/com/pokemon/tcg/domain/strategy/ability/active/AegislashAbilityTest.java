package com.pokemon.tcg.domain.strategy.ability.active;

import com.pokemon.tcg.domain.model.card.Card;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.engine.CardLookupPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AegislashAbilityTest {

    @Mock private CardLookupPort cardLookupPort;

    private AegislashAbility ability;

    private static final String PLAYER_1 = "player-1";
    private static final String AEGISLASH_INSTANCE = "aegislash-1";
    private static final String CARD_85 = "xy1-85";
    private static final String CARD_86 = "xy1-86";

    @BeforeEach
    void setUp() {
        ability = new AegislashAbility(cardLookupPort);
    }

    // ── canApply ─────────────────────────────────────────────────────────────

    @Test
    void canApply_shouldSucceed_whenActiveAndValidSwitch() {
        when(cardLookupPort.findCardById(CARD_86))
                .thenReturn(Optional.of(Card.builder().id(CARD_86).name("Aegislash").build()));

        BoardState state = buildState(true, CARD_85, List.of(CARD_86));
        GameAction action = buildAction(CARD_86);

        assertThat(ability.canApply(state, action).isValid()).isTrue();
    }

    @Test
    void canApply_shouldSucceed_whenOnBench() {
        when(cardLookupPort.findCardById(CARD_86))
                .thenReturn(Optional.of(Card.builder().id(CARD_86).name("Aegislash").build()));

        BoardState state = buildState(false, CARD_85, List.of(CARD_86));
        GameAction action = buildAction(CARD_86);

        assertThat(ability.canApply(state, action).isValid()).isTrue();
    }

    @Test
    void canApply_shouldFail_whenCardNotInHand() {
        BoardState state = buildState(true, CARD_85, List.of());
        GameAction action = buildAction(CARD_86);

        assertThat(ability.canApply(state, action).isValid()).isFalse();
    }

    @Test
    void canApply_shouldSucceed_whenSwitchingToSameCardIdAsCurrent() {
        when(cardLookupPort.findCardById(CARD_86))
                .thenReturn(Optional.of(Card.builder().id(CARD_86).name("Aegislash").build()));

        BoardState state = buildState(true, CARD_86, List.of(CARD_86));
        GameAction action = buildAction(CARD_86);

        assertThat(ability.canApply(state, action).isValid()).isTrue();
    }

    @Test
    void canApply_shouldFail_whenCardIsNotAegislash() {
        when(cardLookupPort.findCardById(CARD_86))
                .thenReturn(Optional.of(Card.builder().id(CARD_86).name("Doublade").build()));

        BoardState state = buildState(true, CARD_85, List.of(CARD_86));
        GameAction action = buildAction(CARD_86);

        assertThat(ability.canApply(state, action).isValid()).isFalse();
    }

    @Test
    void canApply_shouldFail_whenPokemonNotInPlay() {
        BoardState state = buildState(true, CARD_85, List.of(CARD_86));
        GameAction action = buildAction(CARD_86, "not-in-play-instance");

        assertThat(ability.canApply(state, action).isValid()).isFalse();
    }

    // ── apply ────────────────────────────────────────────────────────────────

    @Test
    void apply_shouldSwitchCardId_andPreserveState_whenActive() {
        BoardState state = buildState(true, CARD_85, new ArrayList<>(List.of(CARD_86)));
        ActivePokemon active = state.getStateFor(PLAYER_1).getActivePokemon();
        active.setDamageCounters(3);
        active.getConditions().add(SpecialCondition.POISONED);
        active.setAttachedEnergyIds(new ArrayList<>(List.of("xy1-132")));
        active.setAttachedToolId("xy1-121");
        active.setEnteredThisTurn(false);
        active.getActiveEffects().add(PokemonEffect.INVULNERABLE);
        active.setBlockedAttackName("king's shield");

        GameAction action = buildAction(CARD_86);
        BoardState result = ability.apply(state, action);

        ActivePokemon updated = result.getStateFor(PLAYER_1).getActivePokemon();
        assertThat(updated.getCardId()).isEqualTo(CARD_86);
        assertThat(updated.getDamageCounters()).isEqualTo(3);
        assertThat(updated.getConditions()).contains(SpecialCondition.POISONED);
        assertThat(updated.getAttachedEnergyIds()).containsExactly("xy1-132");
        assertThat(updated.getAttachedToolId()).isEqualTo("xy1-121");
        assertThat(updated.isEnteredThisTurn()).isFalse();
        assertThat(updated.getActiveEffects()).contains(PokemonEffect.INVULNERABLE);
        assertThat(updated.getBlockedAttackName()).isEqualTo("king's shield");
        assertThat(result.getStateFor(PLAYER_1).getHand()).doesNotContain(CARD_86);
    }

    @Test
    void apply_shouldSwitchCardId_whenOnBench() {
        BoardState state = buildState(false, CARD_85, new ArrayList<>(List.of(CARD_86)));
        BenchPokemon bench = state.getStateFor(PLAYER_1).getBench().get(0);
        bench.setDamageCounters(5);

        GameAction action = buildAction(CARD_86);
        BoardState result = ability.apply(state, action);

        BenchPokemon updated = result.getStateFor(PLAYER_1).getBench().get(0);
        assertThat(updated.getCardId()).isEqualTo(CARD_86);
        assertThat(updated.getDamageCounters()).isEqualTo(5);
    }

    @Test
    void apply_shouldAppendNewCardId_toEvolutionStack_replacingCurrentTop() {
        BoardState state = buildState(true, CARD_85, new ArrayList<>(List.of(CARD_86)));

        GameAction action = buildAction(CARD_86);
        BoardState result = ability.apply(state, action);

        ActivePokemon updated = result.getStateFor(PLAYER_1).getActivePokemon();
        assertThat(updated.getEvolutionStack()).endsWith(CARD_86);
        assertThat(updated.getEvolutionStack()).doesNotContain(CARD_85);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private BoardState buildState(boolean asActive, String currentCardId, List<String> hand) {
        String stack1 = "xy1-83"; // Doublade, base of the evolution line
        List<String> evolutionStack = new ArrayList<>(List.of(stack1, currentCardId));

        PlayerState ps1 = PlayerState.builder()
                .playerId(PLAYER_1)
                .hand(new ArrayList<>(hand))
                .deck(new ArrayList<>())
                .discard(new ArrayList<>())
                .prizes(new ArrayList<>())
                .bench(new ArrayList<>())
                .build();

        if (asActive) {
            ActivePokemon active = ActivePokemon.builder()
                    .instanceId(AEGISLASH_INSTANCE)
                    .cardId(currentCardId)
                    .attachedEnergyIds(new ArrayList<>())
                    .evolutionStack(evolutionStack)
                    .damageCounters(0)
                    .conditions(new HashSet<>())
                    .activeEffects(new ArrayList<>())
                    .build();
            ps1.setActivePokemon(active);
        } else {
            BenchPokemon bench = BenchPokemon.builder()
                    .instanceId(AEGISLASH_INSTANCE)
                    .cardId(currentCardId)
                    .attachedEnergyIds(new ArrayList<>())
                    .evolutionStack(evolutionStack)
                    .damageCounters(0)
                    .build();
            ps1.setBench(new ArrayList<>(List.of(bench)));
        }

        PlayerState ps2 = PlayerState.builder()
                .playerId("player-2")
                .hand(new ArrayList<>())
                .deck(new ArrayList<>())
                .discard(new ArrayList<>())
                .prizes(new ArrayList<>())
                .bench(new ArrayList<>())
                .build();

        return BoardState.builder()
                .gameId("game-1")
                .player1State(ps1)
                .player2State(ps2)
                .turnFlags(TurnFlags.fresh())
                .build();
    }

    private GameAction buildAction(String cardId) {
        return buildAction(cardId, AEGISLASH_INSTANCE);
    }

    private GameAction buildAction(String cardId, String instanceId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("instanceId", instanceId);
        payload.put("abilityName", "Stance Change");
        payload.put("cardId", cardId);

        return GameAction.builder()
                .type(GameActionType.USE_ABILITY)
                .playerId(PLAYER_1)
                .payload(payload)
                .build();
    }
}