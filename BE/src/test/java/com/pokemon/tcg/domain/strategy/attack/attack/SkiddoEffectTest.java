package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.Card;
import com.pokemon.tcg.domain.model.card.CardType;
import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.engine.CardLookupPort;
import com.pokemon.tcg.engine.CoinFlipService;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkiddoEffectTest {

    @Mock private CoinFlipService coinFlipService;
    @Mock private CardLookupPort  cardLookupPort;

    private SkiddoEffect effect;

    private static final String SUPPORTER_ID     = "xy1-122";
    private static final String NON_SUPPORTER_ID = "xy1-3";

    @BeforeEach
    void setUp() {
        effect = new SkiddoEffect(coinFlipService, cardLookupPort);
    }

    @Test
    void apply_shouldDoNothing_whenTails() {
        when(coinFlipService.flip()).thenReturn(CoinResult.TAILS);
        AttackContext ctx = buildContext(List.of(SUPPORTER_ID));

        effect.apply(ctx);

        assertThat(ctx.getBoardState().isPendingAttackSelection()).isFalse();
    }

    @Test
    void apply_shouldSetPendingAttackSelection_whenHeadsAndSupporterInDeck() {
        when(coinFlipService.flip()).thenReturn(CoinResult.HEADS);
        when(cardLookupPort.findCardById(SUPPORTER_ID))
                .thenReturn(Optional.of(supporterCard(SUPPORTER_ID)));
        AttackContext ctx = buildContext(List.of(SUPPORTER_ID));

        effect.apply(ctx);

        assertThat(ctx.getBoardState().isPendingAttackSelection()).isTrue();
        assertThat(ctx.getBoardState().getPendingAttackSelectionKey())
                .isEqualTo("skiddo|lead");
        assertThat(ctx.getBoardState().getPendingAttackSelectionPlayerId())
                .isEqualTo(PLAYER_1);
    }

    @Test
    void apply_shouldIncludeOnlySupporters_inPendingCards() {
        when(coinFlipService.flip()).thenReturn(CoinResult.HEADS);
        when(cardLookupPort.findCardById(SUPPORTER_ID))
                .thenReturn(Optional.of(supporterCard(SUPPORTER_ID)));
        when(cardLookupPort.findCardById(NON_SUPPORTER_ID))
                .thenReturn(Optional.of(nonSupporterCard(NON_SUPPORTER_ID)));
        AttackContext ctx = buildContext(List.of(SUPPORTER_ID, NON_SUPPORTER_ID));

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getPendingDeckSelectionCardIds())
                .containsExactly(SUPPORTER_ID);
    }

    @Test
    void apply_shouldDoNothing_whenHeadsButNoSupportersInDeck() {
        when(coinFlipService.flip()).thenReturn(CoinResult.HEADS);
        when(cardLookupPort.findCardById(NON_SUPPORTER_ID))
                .thenReturn(Optional.of(nonSupporterCard(NON_SUPPORTER_ID)));
        AttackContext ctx = buildContext(List.of(NON_SUPPORTER_ID));

        effect.apply(ctx);

        assertThat(ctx.getBoardState().isPendingAttackSelection()).isFalse();
    }

    @Test
    void apply_shouldDoNothing_whenHeadsButDeckIsEmpty() {
        when(coinFlipService.flip()).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext(List.of());

        effect.apply(ctx);

        assertThat(ctx.getBoardState().isPendingAttackSelection()).isFalse();
    }

    @Test
    void apply_shouldNotAddModifiers() {
        when(coinFlipService.flip()).thenReturn(CoinResult.HEADS);
        when(cardLookupPort.findCardById(SUPPORTER_ID))
                .thenReturn(Optional.of(supporterCard(SUPPORTER_ID)));
        AttackContext ctx = buildContext(List.of(SUPPORTER_ID));

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    private AttackContext buildContext(List<String> deck) {
        ActivePokemon skiddo = ActivePokemon.builder()
                .instanceId("skiddo-1")
                .cardId("xy1-18")
                .types(new ArrayList<>(List.of(EnergyType.GRASS)))
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();

        PlayerState attackerState = playerState(PLAYER_1, List.of(), new ArrayList<>(deck));
        attackerState.setActivePokemon(skiddo);

        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(ActivePokemon.builder()
                .instanceId("def-1")
                .cardId("xy1-20")
                .types(new ArrayList<>(List.of(EnergyType.FIRE)))
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build());

        BoardState state = boardState(attackerState, defenderState);
        GameAction act = action(GameActionType.DECLARE_ATTACK, PLAYER_1,
                "attackName", "Lead");

        return AttackContext.builder()
                .boardState(state)
                .action(act)
                .attackName("Lead")
                .modifiers(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
    }

    private Card supporterCard(String id) {
        return Card.builder()
                .id(id)
                .supertype(CardType.TRAINER)
                .subtypes(new ArrayList<>(List.of("Supporter")))
                .build();
    }

    private Card nonSupporterCard(String id) {
        return Card.builder()
                .id(id)
                .supertype(CardType.POKEMON)
                .subtypes(new ArrayList<>(List.of("Basic")))
                .build();
    }
}
