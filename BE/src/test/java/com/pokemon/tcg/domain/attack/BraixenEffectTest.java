package com.pokemon.tcg.domain.attack;

import com.pokemon.tcg.domain.model.card.Card;
import com.pokemon.tcg.domain.model.card.CardType;
import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.engine.CardLookupPort;
import com.pokemon.tcg.engine.attack.AttackContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BraixenEffectTest {

    @Mock
    private CardLookupPort cardLookupPort;

    private BraixenEffect effect;

    private static final String CARD_1         = "xy1-1";
    private static final String CARD_2         = "xy1-2";
    private static final String CARD_3         = "xy1-3";
    private static final String CARD_4         = "xy1-4";
    private static final String FIRE_ENERGY_ID = "xy1-133";
    private static final String WATER_ENERGY   = "xy1-134";

    @BeforeEach
    void setUp() {
        effect = new BraixenEffect(cardLookupPort);
    }

    @Test
    void clairvoyantDeck_shouldSetPendingReorderSelection_withTop3Cards() {
        AttackContext ctx = buildContext("Clairvoyant Deck",
                List.of(CARD_1, CARD_2, CARD_3, CARD_4), List.of(), null);

        effect.apply(ctx);

        BoardState state = ctx.getBoardState();
        assertThat(state.isPendingAttackSelection()).isTrue();
        assertThat(state.getPendingAttackSelectionKey()).isEqualTo("braixen|clairvoyant deck");
        assertThat(state.getPendingAttackSelectionType()).isEqualTo(AttackSelectionType.REORDER);
        assertThat(state.getPendingDeckSelectionCardIds())
                .containsExactly(CARD_1, CARD_2, CARD_3);
    }

    @Test
    void clairvoyantDeck_shouldRevealAllCards_whenDeckHasLessThan3() {
        AttackContext ctx = buildContext("Clairvoyant Deck",
                List.of(CARD_1, CARD_2), List.of(), null);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getPendingDeckSelectionCardIds())
                .containsExactly(CARD_1, CARD_2);
    }

    @Test
    void clairvoyantDeck_shouldDoNothing_whenDeckIsEmpty() {
        AttackContext ctx = buildContext("Clairvoyant Deck",
                List.of(), List.of(), null);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().isPendingAttackSelection()).isFalse();
    }

    @Test
    void clairvoyantDeck_shouldSetMaxCards3() {
        AttackContext ctx = buildContext("Clairvoyant Deck",
                List.of(CARD_1, CARD_2, CARD_3), List.of(), null);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getPendingAttackSelectionMaxCards()).isEqualTo(3);
    }

    @Test
    void firetailSlap_shouldDiscardFireEnergy_whenSpecifiedAndAttached() {
        when(cardLookupPort.findCardById(FIRE_ENERGY_ID))
                .thenReturn(Optional.of(fireEnergyCard(FIRE_ENERGY_ID)));
        AttackContext ctx = buildContext("Firetail Slap",
                List.of(), List.of(FIRE_ENERGY_ID, WATER_ENERGY), FIRE_ENERGY_ID);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getAttachedEnergyIds())
                .containsExactly(WATER_ENERGY);
        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDiscard())
                .containsExactly(FIRE_ENERGY_ID);
    }

    @Test
    void firetailSlap_shouldDoNothing_whenEnergyIsNotFireType() {
        when(cardLookupPort.findCardById(WATER_ENERGY))
                .thenReturn(Optional.of(waterEnergyCard(WATER_ENERGY)));
        AttackContext ctx = buildContext("Firetail Slap",
                List.of(), List.of(WATER_ENERGY), WATER_ENERGY);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getAttachedEnergyIds())
                .containsExactly(WATER_ENERGY);
        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDiscard()).isEmpty();
    }

    @Test
    void firetailSlap_shouldDoNothing_whenEnergyToDiscardIsNull() {
        AttackContext ctx = buildContext("Firetail Slap",
                List.of(), List.of(FIRE_ENERGY_ID), null);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getAttachedEnergyIds())
                .containsExactly(FIRE_ENERGY_ID);
    }

    @Test
    void firetailSlap_shouldNotAddModifiers() {
        when(cardLookupPort.findCardById(FIRE_ENERGY_ID))
                .thenReturn(Optional.of(fireEnergyCard(FIRE_ENERGY_ID)));
        AttackContext ctx = buildContext("Firetail Slap",
                List.of(), List.of(FIRE_ENERGY_ID), FIRE_ENERGY_ID);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    private AttackContext buildContext(String attackName, List<String> deck,
                                      List<String> attachedEnergies,
                                      String energyToDiscardId) {
        ActivePokemon braixen = ActivePokemon.builder()
                .instanceId("braixen-1")
                .cardId("xy1-25")
                .types(new ArrayList<>(List.of(EnergyType.FIRE)))
                .attachedEnergyIds(new ArrayList<>(attachedEnergies))
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();

        ActivePokemon defender = ActivePokemon.builder()
                .instanceId("def-1")
                .cardId("xy1-30")
                .types(new ArrayList<>(List.of(EnergyType.WATER)))
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();

        PlayerState attackerState = playerState(PLAYER_1, List.of(), new ArrayList<>(deck));
        attackerState.setActivePokemon(braixen);
        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(defender);

        BoardState state = boardState(attackerState, defenderState);

        Map<String, Object> payload = new HashMap<>();
        payload.put("attackName", attackName);
        if (energyToDiscardId != null) {
            payload.put("energyToDiscardId", energyToDiscardId);
        }

        GameAction act = GameAction.builder()
                .type(GameActionType.DECLARE_ATTACK)
                .playerId(PLAYER_1)
                .payload(payload)
                .build();

        return AttackContext.builder()
                .boardState(state)
                .action(act)
                .attackName(attackName)
                .modifiers(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
    }

    private Card fireEnergyCard(String id) {
        return Card.builder()
                .id(id)
                .supertype(CardType.ENERGY)
                .basicEnergy(true)
                .types(new ArrayList<>(List.of(EnergyType.FIRE.name())))
                .build();
    }

    private Card waterEnergyCard(String id) {
        return Card.builder()
                .id(id)
                .supertype(CardType.ENERGY)
                .basicEnergy(true)
                .types(new ArrayList<>(List.of(EnergyType.WATER.name())))
                .build();
    }
}
