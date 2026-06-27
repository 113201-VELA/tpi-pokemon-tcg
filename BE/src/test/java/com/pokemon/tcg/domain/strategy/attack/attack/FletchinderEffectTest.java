package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.Card;
import com.pokemon.tcg.domain.model.card.CardType;
import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.engine.CardLookupPort;
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
class FletchinderEffectTest {

    @Mock
    private CardLookupPort cardLookupPort;

    private FletchinderEffect effect;

    private static final String FIRE_ENERGY_ID  = "xy1-133";
    private static final String WATER_ENERGY_ID = "xy1-134";
    private static final String POKEMON_CARD_ID = "xy1-3";

    @BeforeEach
    void setUp() {
        effect = new FletchinderEffect(cardLookupPort);
    }

    @Test
    void getSupportedAttacks_shouldReturn_fletchinderFlameCharge() {
        assertThat(effect.getSupportedAttacks())
                .containsExactly("fletchinder|flame charge");
    }

    @Test
    void apply_shouldAttachFireEnergyToFletchinder_whenFoundInDeck() {
        when(cardLookupPort.findCardById(FIRE_ENERGY_ID))
                .thenReturn(Optional.of(fireEnergyCard(FIRE_ENERGY_ID)));
        AttackContext ctx = buildContext(List.of(FIRE_ENERGY_ID, WATER_ENERGY_ID),
                List.of());

        effect.apply(ctx);

        ActivePokemon fletchinder = ctx.getBoardState()
                .getStateFor(PLAYER_1).getActivePokemon();
        assertThat(fletchinder.getAttachedEnergyIds()).containsExactly(FIRE_ENERGY_ID);
    }

    @Test
    void apply_shouldRemoveFireEnergy_fromDeck() {
        when(cardLookupPort.findCardById(FIRE_ENERGY_ID))
                .thenReturn(Optional.of(fireEnergyCard(FIRE_ENERGY_ID)));
        AttackContext ctx = buildContext(List.of(FIRE_ENERGY_ID, WATER_ENERGY_ID),
                List.of());

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDeck())
                .doesNotContain(FIRE_ENERGY_ID);
    }

    @Test
    void apply_shouldPreserveExistingAttachedEnergies_whenAttaching() {
        when(cardLookupPort.findCardById(FIRE_ENERGY_ID))
                .thenReturn(Optional.of(fireEnergyCard(FIRE_ENERGY_ID)));
        AttackContext ctx = buildContext(List.of(FIRE_ENERGY_ID),
                List.of(WATER_ENERGY_ID));

        effect.apply(ctx);

        ActivePokemon fletchinder = ctx.getBoardState()
                .getStateFor(PLAYER_1).getActivePokemon();
        assertThat(fletchinder.getAttachedEnergyIds())
                .containsExactlyInAnyOrder(WATER_ENERGY_ID, FIRE_ENERGY_ID);
    }

    @Test
    void apply_shouldTakeFirstFireEnergy_whenMultiplePresentInDeck() {
        when(cardLookupPort.findCardById(FIRE_ENERGY_ID))
                .thenReturn(Optional.of(fireEnergyCard(FIRE_ENERGY_ID)));
        AttackContext ctx = buildContext(
                List.of(FIRE_ENERGY_ID, FIRE_ENERGY_ID, WATER_ENERGY_ID),
                List.of());

        effect.apply(ctx);

        ActivePokemon fletchinder = ctx.getBoardState()
                .getStateFor(PLAYER_1).getActivePokemon();
        assertThat(fletchinder.getAttachedEnergyIds()).hasSize(1);
        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDeck()).hasSize(2);
    }

    @Test
    void apply_shouldDoNothing_whenNoFireEnergyInDeck() {
        when(cardLookupPort.findCardById(WATER_ENERGY_ID))
                .thenReturn(Optional.of(waterEnergyCard(WATER_ENERGY_ID)));
        when(cardLookupPort.findCardById(POKEMON_CARD_ID))
                .thenReturn(Optional.of(pokemonCard(POKEMON_CARD_ID)));
        AttackContext ctx = buildContext(
                List.of(WATER_ENERGY_ID, POKEMON_CARD_ID), List.of());

        effect.apply(ctx);

        ActivePokemon fletchinder = ctx.getBoardState()
                .getStateFor(PLAYER_1).getActivePokemon();
        assertThat(fletchinder.getAttachedEnergyIds()).isEmpty();
        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDeck())
                .hasSize(2);
    }

    @Test
    void apply_shouldDoNothing_whenDeckIsEmpty() {
        AttackContext ctx = buildContext(List.of(), List.of());

        effect.apply(ctx);

        ActivePokemon fletchinder = ctx.getBoardState()
                .getStateFor(PLAYER_1).getActivePokemon();
        assertThat(fletchinder.getAttachedEnergyIds()).isEmpty();
    }

    @Test
    void apply_shouldNotAddModifiers() {
        when(cardLookupPort.findCardById(FIRE_ENERGY_ID))
                .thenReturn(Optional.of(fireEnergyCard(FIRE_ENERGY_ID)));
        AttackContext ctx = buildContext(List.of(FIRE_ENERGY_ID), List.of());

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    @Test
    void apply_shouldNotAffectDefender() {
        when(cardLookupPort.findCardById(FIRE_ENERGY_ID))
                .thenReturn(Optional.of(fireEnergyCard(FIRE_ENERGY_ID)));
        AttackContext ctx = buildContext(List.of(FIRE_ENERGY_ID), List.of());
        int defenderCounters = ctx.getBoardState()
                .getStateFor(PLAYER_2).getActivePokemon().getDamageCounters();

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getDamageCounters()).isEqualTo(defenderCounters);
    }

    private AttackContext buildContext(List<String> deck,
                                      List<String> alreadyAttached) {
        ActivePokemon fletchinder = ActivePokemon.builder()
                .instanceId("fletchinder-1")
                .cardId("xy1-27")
                .types(new ArrayList<>(List.of(EnergyType.FIRE)))
                .attachedEnergyIds(new ArrayList<>(alreadyAttached))
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

        PlayerState attackerState = playerState(PLAYER_1, List.of(),
                new ArrayList<>(deck));
        attackerState.setActivePokemon(fletchinder);
        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(defender);

        BoardState state = boardState(attackerState, defenderState);
        GameAction act = action(GameActionType.DECLARE_ATTACK, PLAYER_1,
                "attackName", "Flame Charge");

        return AttackContext.builder()
                .boardState(state)
                .action(act)
                .attackName("Flame Charge")
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
