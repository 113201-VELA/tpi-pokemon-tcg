package com.pokemon.tcg.engine.attack.steps;

import com.pokemon.tcg.domain.model.card.Attack;
import com.pokemon.tcg.domain.model.card.Card;
import com.pokemon.tcg.domain.model.card.CardType;
import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.engine.CardLookupPort;
import com.pokemon.tcg.engine.attack.AttackChain;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnergyCheckStepTest {

    @Mock private CardLookupPort cardLookupPort;
    @Mock private AttackChain chain;

    private EnergyCheckStep step;

    private static final String FIRE_ENERGY_ID     = "xy1-133";
    private static final String WATER_ENERGY_ID    = "xy1-134";
    private static final String DBL_COLORLESS_ID   = "xy1-130";
    private static final String RAINBOW_ID         = "xy1-131";

    @BeforeEach
    void setUp() {
        step = new EnergyCheckStep(cardLookupPort);
    }

    // ── No cost ───────────────────────────────────────────────────────────────

    @Test
    void execute_shouldProceed_whenAttackHasNoCost() {
        AttackContext ctx = contextWith(List.of(), List.of());

        step.execute(ctx, chain);

        assertThat(ctx.isCancelled()).isFalse();
        verify(chain).next(ctx);
    }

    // ── Exact type matching ───────────────────────────────────────────────────

    @Test
    void execute_shouldProceed_whenExactTypesMatch() {
        stubBasicEnergy(FIRE_ENERGY_ID, EnergyType.FIRE);
        AttackContext ctx = contextWith(
                List.of(EnergyType.FIRE, EnergyType.FIRE),
                List.of(FIRE_ENERGY_ID, FIRE_ENERGY_ID));

        step.execute(ctx, chain);

        assertThat(ctx.isCancelled()).isFalse();
        verify(chain).next(ctx);
    }

    @Test
    void execute_shouldCancel_whenTypedEnergyMissing() {
        stubBasicEnergy(WATER_ENERGY_ID, EnergyType.WATER);
        AttackContext ctx = contextWith(
                List.of(EnergyType.FIRE),
                List.of(WATER_ENERGY_ID));

        step.execute(ctx, chain);

        assertThat(ctx.isCancelled()).isTrue();
        verify(chain, never()).next(ctx);
    }

    @Test
    void execute_shouldCancel_whenNotEnoughEnergiesTotal() {
        stubBasicEnergy(FIRE_ENERGY_ID, EnergyType.FIRE);
        AttackContext ctx = contextWith(
                List.of(EnergyType.FIRE, EnergyType.COLORLESS),
                List.of(FIRE_ENERGY_ID));

        step.execute(ctx, chain);

        assertThat(ctx.isCancelled()).isTrue();
    }

    // ── COLORLESS matching ────────────────────────────────────────────────────

    @Test
    void execute_shouldProceed_whenColorlessIsSatisfiedByAnyEnergy() {
        stubBasicEnergy(FIRE_ENERGY_ID, EnergyType.FIRE);
        stubBasicEnergy(WATER_ENERGY_ID, EnergyType.WATER);
        AttackContext ctx = contextWith(
                List.of(EnergyType.COLORLESS, EnergyType.COLORLESS),
                List.of(FIRE_ENERGY_ID, WATER_ENERGY_ID));

        step.execute(ctx, chain);

        assertThat(ctx.isCancelled()).isFalse();
        verify(chain).next(ctx);
    }

    // ── Double Colorless Energy ───────────────────────────────────────────────

    @Test
    void execute_shouldProceed_whenDoubleColorlessSatisfiesTwoColorlessCosts() {
        stubDoubleColorless(DBL_COLORLESS_ID);
        AttackContext ctx = contextWith(
                List.of(EnergyType.COLORLESS, EnergyType.COLORLESS),
                List.of(DBL_COLORLESS_ID));

        step.execute(ctx, chain);

        assertThat(ctx.isCancelled()).isFalse();
        verify(chain).next(ctx);
    }

    @Test
    void execute_shouldCancel_whenDoubleColorlessCannotSatisfyTypedCost() {
        stubDoubleColorless(DBL_COLORLESS_ID);
        AttackContext ctx = contextWith(
                List.of(EnergyType.FIRE),
                List.of(DBL_COLORLESS_ID));

        step.execute(ctx, chain);

        assertThat(ctx.isCancelled()).isTrue();
    }

    @Test
    void execute_shouldProceed_whenDoubleColorlessSatisfiesMixedCost() {
        stubBasicEnergy(FIRE_ENERGY_ID, EnergyType.FIRE);
        stubDoubleColorless(DBL_COLORLESS_ID);
        AttackContext ctx = contextWith(
                List.of(EnergyType.FIRE, EnergyType.COLORLESS, EnergyType.COLORLESS),
                List.of(FIRE_ENERGY_ID, DBL_COLORLESS_ID));

        step.execute(ctx, chain);

        assertThat(ctx.isCancelled()).isFalse();
        verify(chain).next(ctx);
    }

    // ── Rainbow Energy ────────────────────────────────────────────────────────

    @Test
    void execute_shouldProceed_whenRainbowEnergySatisfiesTypedCost() {
        stubRainbow(RAINBOW_ID);
        AttackContext ctx = contextWith(
                List.of(EnergyType.FIRE),
                List.of(RAINBOW_ID));

        step.execute(ctx, chain);

        assertThat(ctx.isCancelled()).isFalse();
        verify(chain).next(ctx);
    }

    @Test
    void execute_shouldProceed_whenRainbowEnergySatisfiesColorlessCost() {
        stubRainbow(RAINBOW_ID);
        AttackContext ctx = contextWith(
                List.of(EnergyType.COLORLESS),
                List.of(RAINBOW_ID));

        step.execute(ctx, chain);

        assertThat(ctx.isCancelled()).isFalse();
        verify(chain).next(ctx);
    }

    @Test
    void execute_shouldPreferExactMatchOverRainbow_forTypedCost() {
        stubBasicEnergy(FIRE_ENERGY_ID, EnergyType.FIRE);
        stubRainbow(RAINBOW_ID);
        AttackContext ctx = contextWith(
                List.of(EnergyType.FIRE, EnergyType.WATER),
                List.of(FIRE_ENERGY_ID, RAINBOW_ID));

        step.execute(ctx, chain);

        assertThat(ctx.isCancelled()).isFalse();
        verify(chain).next(ctx);
    }

    @Test
    void execute_shouldCancel_whenRainbowAloneCannotSatisfyTwoTypedCosts() {
        stubRainbow(RAINBOW_ID);
        AttackContext ctx = contextWith(
                List.of(EnergyType.FIRE, EnergyType.WATER),
                List.of(RAINBOW_ID));

        step.execute(ctx, chain);

        assertThat(ctx.isCancelled()).isTrue();
    }

    // ── Test helpers ───────────────────────────────────────────────────────────

    private AttackContext contextWith(List<EnergyType> cost, List<String> attachedIds) {
        Attack attack = Attack.builder()
                .name("Test Attack")
                .cost(cost)
                .damage("50")
                .build();

        ActivePokemon attacker = ActivePokemon.builder()
                .instanceId("att-1")
                .cardId("xy1-1")
                .attachedEnergyIds(new ArrayList<>(attachedIds))
                .evolutionStack(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .types(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .build();

        PlayerState ps = playerState(PLAYER_1, List.of(), cardIds(5));
        ps.setActivePokemon(attacker);
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));

        GameAction act = action(GameActionType.DECLARE_ATTACK, PLAYER_1, "attackName", "Test Attack");

        return AttackContext.builder()
                .boardState(state)
                .action(act)
                .attack(attack)
                .attackName("Test Attack")
                .modifiers(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
    }

    private void stubBasicEnergy(String cardId, EnergyType type) {
        when(cardLookupPort.findCardById(cardId)).thenReturn(Optional.of(
                Card.builder()
                        .id(cardId)
                        .name(type.name() + " Energy")
                        .supertype(CardType.ENERGY)
                        .basicEnergy(true)
                        .types(new ArrayList<>(List.of(type.name())))
                        .build()));
    }

    private void stubDoubleColorless(String cardId) {
        when(cardLookupPort.findCardById(cardId)).thenReturn(Optional.of(
                Card.builder()
                        .id(cardId)
                        .name("Double Colorless Energy")
                        .supertype(CardType.ENERGY)
                        .basicEnergy(false)
                        .types(new ArrayList<>(List.of("COLORLESS")))
                        .build()));
    }

    private void stubRainbow(String cardId) {
        when(cardLookupPort.findCardById(cardId)).thenReturn(Optional.of(
                Card.builder()
                        .id(cardId)
                        .name("Rainbow Energy")
                        .supertype(CardType.ENERGY)
                        .basicEnergy(false)
                        .types(new ArrayList<>())
                        .build()));
    }
}
