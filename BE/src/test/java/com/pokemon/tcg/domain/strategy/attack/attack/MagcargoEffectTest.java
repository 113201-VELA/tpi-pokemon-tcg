package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.Card;
import com.pokemon.tcg.domain.model.card.CardType;
import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.engine.CardLookupPort;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
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
class MagcargoEffectTest {

    @Mock
    private CardLookupPort cardLookupPort;

    private MagcargoEffect effect;

    private static final String FIRE_ENERGY_ID  = "xy1-133";
    private static final String WATER_ENERGY_ID = "xy1-134";
    private static final String POKEMON_CARD_ID = "xy1-3";

    @BeforeEach
    void setUp() {
        effect = new MagcargoEffect(cardLookupPort);
    }

    @Test
    void apply_shouldDoNothing_whenDiscardTopIsFalse() {
        AttackContext ctx = buildContext(List.of(FIRE_ENERGY_ID), false);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDeck())
                .containsExactly(FIRE_ENERGY_ID);
        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDiscard()).isEmpty();
        assertThat(ctx.getModifiers()).isEmpty();
    }

    @Test
    void apply_shouldDoNothing_whenDiscardTopIsAbsent() {
        AttackContext ctx = buildContextNoPayload(List.of(FIRE_ENERGY_ID));

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDeck())
                .containsExactly(FIRE_ENERGY_ID);
        assertThat(ctx.getModifiers()).isEmpty();
    }

    @Test
    void apply_shouldDiscardTopCard_whenDiscardTopIsTrue() {
        when(cardLookupPort.findCardById(FIRE_ENERGY_ID))
                .thenReturn(Optional.of(fireEnergyCard(FIRE_ENERGY_ID)));
        AttackContext ctx = buildContext(
                List.of(FIRE_ENERGY_ID, WATER_ENERGY_ID), true);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDeck())
                .containsExactly(WATER_ENERGY_ID);
        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDiscard())
                .containsExactly(FIRE_ENERGY_ID);
    }

    @Test
    void apply_shouldAdd50DamageModifier_whenTopCardIsFireEnergy() {
        when(cardLookupPort.findCardById(FIRE_ENERGY_ID))
                .thenReturn(Optional.of(fireEnergyCard(FIRE_ENERGY_ID)));
        AttackContext ctx = buildContext(List.of(FIRE_ENERGY_ID), true);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).hasSize(1);
        assertThat(ctx.getModifiers().get(0).amount()).isEqualTo(50);
        assertThat(ctx.getModifiers().get(0).beforeWeakness()).isTrue();
        assertThat(ctx.getModifiers().get(0).source()).isEqualTo("magma-mantle-fire");
    }

    @Test
    void apply_shouldDiscardTopCard_butNotAddBonus_whenTopCardIsNotFireEnergy() {
        when(cardLookupPort.findCardById(WATER_ENERGY_ID))
                .thenReturn(Optional.of(waterEnergyCard(WATER_ENERGY_ID)));
        AttackContext ctx = buildContext(List.of(WATER_ENERGY_ID), true);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDiscard())
                .containsExactly(WATER_ENERGY_ID);
        assertThat(ctx.getModifiers()).isEmpty();
    }

    @Test
    void apply_shouldDiscardTopCard_butNotAddBonus_whenTopCardIsPokemon() {
        when(cardLookupPort.findCardById(POKEMON_CARD_ID))
                .thenReturn(Optional.of(pokemonCard(POKEMON_CARD_ID)));
        AttackContext ctx = buildContext(List.of(POKEMON_CARD_ID), true);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDiscard())
                .containsExactly(POKEMON_CARD_ID);
        assertThat(ctx.getModifiers()).isEmpty();
    }

    @Test
    void apply_shouldDoNothing_whenDeckIsEmpty() {
        AttackContext ctx = buildContext(List.of(), true);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDiscard()).isEmpty();
        assertThat(ctx.getModifiers()).isEmpty();
    }

    @Test
    void apply_shouldNotAffectDefender() {
        when(cardLookupPort.findCardById(FIRE_ENERGY_ID))
                .thenReturn(Optional.of(fireEnergyCard(FIRE_ENERGY_ID)));
        AttackContext ctx = buildContext(List.of(FIRE_ENERGY_ID), true);
        int defenderCounters = ctx.getBoardState()
                .getStateFor(PLAYER_2).getActivePokemon().getDamageCounters();

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getDamageCounters()).isEqualTo(defenderCounters);
    }

    private AttackContext buildContext(List<String> deck, boolean discardTop) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("attackName", "Magma Mantle");
        payload.put("discardTop", discardTop);
        return buildContextWithPayload(deck, payload);
    }

    private AttackContext buildContextNoPayload(List<String> deck) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("attackName", "Magma Mantle");
        return buildContextWithPayload(deck, payload);
    }

    private AttackContext buildContextWithPayload(List<String> deck,
                                                  Map<String, Object> payload) {
        ActivePokemon magcargo = ActivePokemon.builder()
                .instanceId("magcargo-1")
                .cardId("xy1-21")
                .types(new ArrayList<>(List.of(EnergyType.FIRE)))
                .attachedEnergyIds(new ArrayList<>())
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
        attackerState.setActivePokemon(magcargo);
        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(defender);

        BoardState state = boardState(attackerState, defenderState);

        GameAction act = GameAction.builder()
                .type(GameActionType.DECLARE_ATTACK)
                .playerId(PLAYER_1)
                .payload(payload)
                .build();

        return AttackContext.builder()
                .boardState(state)
                .action(act)
                .attackName("Magma Mantle")
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

    private Card pokemonCard(String id) {
        return Card.builder()
                .id(id)
                .supertype(CardType.POKEMON)
                .basicEnergy(false)
                .build();
    }
}
