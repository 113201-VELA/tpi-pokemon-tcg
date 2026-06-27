package com.pokemon.tcg.domain.attack;

import com.pokemon.tcg.domain.model.card.Card;
import com.pokemon.tcg.domain.model.card.CardType;
import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.engine.CardLookupPort;
import com.pokemon.tcg.engine.CoinFlipService;
import com.pokemon.tcg.engine.DamageModifier;
import com.pokemon.tcg.engine.attack.AttackContext;
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
class IllumiseEffectTest {

    @Mock private CardLookupPort  cardLookupPort;
    @Mock private CoinFlipService coinFlipService;

    private IllumiseEffect effect;

    private static final String GRASS_POKEMON_ID    = "xy1-1";
    private static final String NON_GRASS_POKEMON_ID = "xy1-50";
    private static final String ENERGY_CARD_ID       = "xy1-132";

    @BeforeEach
    void setUp() {
        effect = new IllumiseEffect(cardLookupPort, coinFlipService);
    }

    @Test
    void pheromation_shouldSetPendingAttackSelection_whenGrassPokemonInDeck() {
        when(cardLookupPort.findCardById(GRASS_POKEMON_ID))
                .thenReturn(Optional.of(grassPokemonCard(GRASS_POKEMON_ID)));

        AttackContext ctx = buildContext("Pheromation",
                List.of(GRASS_POKEMON_ID, ENERGY_CARD_ID));

        effect.apply(ctx);

        assertThat(ctx.getBoardState().isPendingAttackSelection()).isTrue();
        assertThat(ctx.getBoardState().getPendingAttackSelectionKey())
                .isEqualTo("illumise|pheromation");
        assertThat(ctx.getBoardState().getPendingAttackSelectionPlayerId())
                .isEqualTo(PLAYER_1);
    }

    @Test
    void pheromation_shouldIncludeOnlyGrassPokemon_inPendingCards() {
        when(cardLookupPort.findCardById(GRASS_POKEMON_ID))
                .thenReturn(Optional.of(grassPokemonCard(GRASS_POKEMON_ID)));
        when(cardLookupPort.findCardById(NON_GRASS_POKEMON_ID))
                .thenReturn(Optional.of(nonGrassPokemonCard(NON_GRASS_POKEMON_ID)));
        when(cardLookupPort.findCardById(ENERGY_CARD_ID))
                .thenReturn(Optional.of(energyCard(ENERGY_CARD_ID)));

        AttackContext ctx = buildContext("Pheromation",
                List.of(GRASS_POKEMON_ID, NON_GRASS_POKEMON_ID, ENERGY_CARD_ID));

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getPendingDeckSelectionCardIds())
                .containsExactly(GRASS_POKEMON_ID);
    }

    @Test
    void pheromation_shouldDoNothing_whenNoGrassPokemonInDeck() {
        when(cardLookupPort.findCardById(ENERGY_CARD_ID))
                .thenReturn(Optional.of(energyCard(ENERGY_CARD_ID)));

        AttackContext ctx = buildContext("Pheromation", List.of(ENERGY_CARD_ID));

        effect.apply(ctx);

        assertThat(ctx.getBoardState().isPendingAttackSelection()).isFalse();
    }

    @Test
    void pheromation_shouldDoNothing_whenDeckIsEmpty() {
        AttackContext ctx = buildContext("Pheromation", List.of());

        effect.apply(ctx);

        assertThat(ctx.getBoardState().isPendingAttackSelection()).isFalse();
    }

    @Test
    void quickAttack_shouldAdd20DamageModifier_whenHeads() {
        when(coinFlipService.flip()).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext("Quick Attack", List.of());

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).hasSize(1);
        DamageModifier mod = ctx.getModifiers().get(0);
        assertThat(mod.amount()).isEqualTo(20);
        assertThat(mod.beforeWeakness()).isTrue();
    }

    @Test
    void quickAttack_shouldAddNoModifier_whenTails() {
        when(coinFlipService.flip()).thenReturn(CoinResult.TAILS);
        AttackContext ctx = buildContext("Quick Attack", List.of());

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    private AttackContext buildContext(String attackName, List<String> deck) {
        ActivePokemon illumise = ActivePokemon.builder()
                .instanceId("illumise-1")
                .cardId("xy1-9")
                .types(new ArrayList<>(List.of(EnergyType.COLORLESS)))
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();

        PlayerState attackerState = playerState(PLAYER_1, List.of(), new ArrayList<>(deck));
        attackerState.setActivePokemon(illumise);

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
                "attackName", attackName);

        return AttackContext.builder()
                .boardState(state)
                .action(act)
                .attackName(attackName)
                .modifiers(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
    }

    private Card grassPokemonCard(String id) {
        return Card.builder()
                .id(id)
                .supertype(CardType.POKEMON)
                .types(new ArrayList<>(List.of("GRASS")))
                .build();
    }

    private Card nonGrassPokemonCard(String id) {
        return Card.builder()
                .id(id)
                .supertype(CardType.POKEMON)
                .types(new ArrayList<>(List.of("FIRE")))
                .build();
    }

    private Card energyCard(String id) {
        return Card.builder()
                .id(id)
                .supertype(CardType.ENERGY)
                .types(new ArrayList<>(List.of("GRASS")))
                .basicEnergy(true)
                .build();
    }
}
