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
class TalonflameEffectTest {

    @Mock
    private CardLookupPort cardLookupPort;

    private TalonflameEffect effect;

    private static final String FIRE_ENERGY_1  = "xy1-133";
    private static final String FIRE_ENERGY_2  = "xy1-133b";
    private static final String WATER_ENERGY   = "xy1-134";

    @BeforeEach
    void setUp() {
        effect = new TalonflameEffect(cardLookupPort);
    }

    @Test
    void getSupportedAttacks_shouldReturnBothAttacks() {
        assertThat(effect.getSupportedAttacks()).containsExactlyInAnyOrder(
                "talonflame|devastating wind",
                "talonflame|flare blitz"
        );
    }

    @Test
    void devastatingWind_shouldShuffleOpponentHandIntoDeckAndDraw4() {
        AttackContext ctx = buildContext("Devastating Wind",
                List.of(), cardIds(7), cardIds(10), List.of());

        effect.apply(ctx);

        PlayerState opponent = ctx.getBoardState().getStateFor(PLAYER_2);
        assertThat(opponent.getHand()).hasSize(4);
        assertThat(opponent.getDeck()).hasSize(13);
    }

    @Test
    void devastatingWind_shouldDrawAllCards_whenCombinedTotalLessThan4() {
        AttackContext ctx = buildContext("Devastating Wind",
                List.of(), cardIds(1), cardIds(2), List.of());

        effect.apply(ctx);

        PlayerState opponent = ctx.getBoardState().getStateFor(PLAYER_2);
        assertThat(opponent.getHand()).hasSize(3);
        assertThat(opponent.getDeck()).isEmpty();
    }

    @Test
    void devastatingWind_shouldNotAffectAttacker() {
        AttackContext ctx = buildContext("Devastating Wind",
                List.of(), cardIds(5), cardIds(5), List.of());
        PlayerState attacker = ctx.getBoardState().getStateFor(PLAYER_1);
        int initialDeckSize = attacker.getDeck().size();

        effect.apply(ctx);

        assertThat(attacker.getDeck()).hasSize(initialDeckSize);
    }

    @Test
    void flareBlitz_shouldDiscardAllFireEnergies() {
        stubFireEnergy(FIRE_ENERGY_1);
        stubFireEnergy(FIRE_ENERGY_2);
        stubWaterEnergy(WATER_ENERGY);

        AttackContext ctx = buildContext("Flare Blitz",
                List.of(FIRE_ENERGY_1, FIRE_ENERGY_2, WATER_ENERGY),
                List.of(), List.of(), List.of());

        effect.apply(ctx);

        ActivePokemon talonflame = ctx.getBoardState()
                .getStateFor(PLAYER_1).getActivePokemon();
        assertThat(talonflame.getAttachedEnergyIds())
                .containsExactly(WATER_ENERGY);
        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDiscard())
                .containsExactlyInAnyOrder(FIRE_ENERGY_1, FIRE_ENERGY_2);
    }

    @Test
    void flareBlitz_shouldDiscardOnlyFireEnergies_keepingNonFire() {
        stubFireEnergy(FIRE_ENERGY_1);
        stubWaterEnergy(WATER_ENERGY);

        AttackContext ctx = buildContext("Flare Blitz",
                List.of(FIRE_ENERGY_1, WATER_ENERGY),
                List.of(), List.of(), List.of());

        effect.apply(ctx);

        ActivePokemon talonflame = ctx.getBoardState()
                .getStateFor(PLAYER_1).getActivePokemon();
        assertThat(talonflame.getAttachedEnergyIds())
                .containsExactly(WATER_ENERGY);
        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDiscard())
                .containsExactly(FIRE_ENERGY_1);
    }

    @Test
    void flareBlitz_shouldDoNothing_whenNoFireEnergyAttached() {
        stubWaterEnergy(WATER_ENERGY);

        AttackContext ctx = buildContext("Flare Blitz",
                List.of(WATER_ENERGY),
                List.of(), List.of(), List.of());

        effect.apply(ctx);

        ActivePokemon talonflame = ctx.getBoardState()
                .getStateFor(PLAYER_1).getActivePokemon();
        assertThat(talonflame.getAttachedEnergyIds())
                .containsExactly(WATER_ENERGY);
        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDiscard()).isEmpty();
    }

    @Test
    void flareBlitz_shouldDoNothing_whenNoEnergiesAttached() {
        AttackContext ctx = buildContext("Flare Blitz",
                List.of(), List.of(), List.of(), List.of());

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDiscard()).isEmpty();
    }

    @Test
    void flareBlitz_shouldNotAddModifiers() {
        stubFireEnergy(FIRE_ENERGY_1);
        AttackContext ctx = buildContext("Flare Blitz",
                List.of(FIRE_ENERGY_1), List.of(), List.of(), List.of());

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    private AttackContext buildContext(String attackName,
                                      List<String> attachedEnergies,
                                      List<String> opponentHand,
                                      List<String> opponentDeck,
                                      List<String> attackerDeck) {
        ActivePokemon talonflame = ActivePokemon.builder()
                .instanceId("talonflame-1")
                .cardId("xy1-28")
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

        PlayerState attackerState = playerState(PLAYER_1, List.of(),
                new ArrayList<>(attackerDeck));
        attackerState.setActivePokemon(talonflame);

        PlayerState defenderState = playerState(PLAYER_2,
                new ArrayList<>(opponentHand), new ArrayList<>(opponentDeck));
        defenderState.setActivePokemon(defender);

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

    private void stubFireEnergy(String cardId) {
        when(cardLookupPort.findCardById(cardId)).thenReturn(Optional.of(
                Card.builder()
                        .id(cardId)
                        .supertype(CardType.ENERGY)
                        .basicEnergy(true)
                        .types(new ArrayList<>(List.of(EnergyType.FIRE.name())))
                        .build()));
    }

    private void stubWaterEnergy(String cardId) {
        when(cardLookupPort.findCardById(cardId)).thenReturn(Optional.of(
                Card.builder()
                        .id(cardId)
                        .supertype(CardType.ENERGY)
                        .basicEnergy(true)
                        .types(new ArrayList<>(List.of(EnergyType.WATER.name())))
                        .build()));
    }
}
