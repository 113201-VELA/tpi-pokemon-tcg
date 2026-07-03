package com.pokemon.tcg.domain.strategy.ability.ability;

import com.pokemon.tcg.domain.model.card.Card;
import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.engine.CardLookupPort;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SlurpuffAbilityTest {

    private CardLookupPort  cardLookupPort;
    private SlurpuffAbility ability;

    private static final String FAIRY_ENERGY = "xy1-134";
    private static final String WATER_ENERGY = "xy1-131";

    @BeforeEach
    void setUp() {
        cardLookupPort = mock(CardLookupPort.class);
        ability = new SlurpuffAbility(cardLookupPort);

        Card fairyCard = mock(Card.class);
        when(fairyCard.getTypes()).thenReturn(List.of(EnergyType.FAIRY.name()));
        when(cardLookupPort.findCardById(FAIRY_ENERGY)).thenReturn(Optional.of(fairyCard));

        Card waterCard = mock(Card.class);
        when(waterCard.getTypes()).thenReturn(List.of(EnergyType.WATER.name()));
        when(cardLookupPort.findCardById(WATER_ENERGY)).thenReturn(Optional.of(waterCard));
    }

    @Test
    void shouldHaveCorrectIdentifier() {
        assertThat(ability.getIdentifier()).isEqualTo("slurpuff");
    }

    @Test
    void onDamageReceived_shouldClearConditions_whenDamagedWithFairyEnergyAttached() {
        AttackContext ctx = buildContext(30, List.of(FAIRY_ENERGY));
        ActivePokemon slurpuff = buildSlurpuff(List.of(FAIRY_ENERGY),
                new HashSet<>(Set.of(SpecialCondition.PARALYZED)));

        ability.onDamageReceived(ctx, slurpuff);

        assertThat(slurpuff.getConditions()).isEmpty();
    }

    @Test
    void onDamageReceived_shouldClearMultipleConditions() {
        AttackContext ctx = buildContext(30, List.of(FAIRY_ENERGY));
        ActivePokemon slurpuff = buildSlurpuff(List.of(FAIRY_ENERGY),
                new HashSet<>(Set.of(SpecialCondition.POISONED, SpecialCondition.BURNED)));

        ability.onDamageReceived(ctx, slurpuff);

        assertThat(slurpuff.getConditions()).isEmpty();
    }

    @Test
    void onDamageReceived_shouldNotClearConditions_whenNoFairyEnergyAttached() {
        AttackContext ctx = buildContext(30, List.of(WATER_ENERGY));
        ActivePokemon slurpuff = buildSlurpuff(List.of(WATER_ENERGY),
                new HashSet<>(Set.of(SpecialCondition.PARALYZED)));

        ability.onDamageReceived(ctx, slurpuff);

        assertThat(slurpuff.getConditions()).contains(SpecialCondition.PARALYZED);
    }

    @Test
    void onDamageReceived_shouldNotClearConditions_whenNoEnergyAttachedAtAll() {
        AttackContext ctx = buildContext(30, List.of());
        ActivePokemon slurpuff = buildSlurpuff(List.of(),
                new HashSet<>(Set.of(SpecialCondition.PARALYZED)));

        ability.onDamageReceived(ctx, slurpuff);

        assertThat(slurpuff.getConditions()).contains(SpecialCondition.PARALYZED);
    }

    @Test
    void onDamageReceived_shouldDoNothing_whenNoDamageDealt() {
        AttackContext ctx = buildContext(0, List.of(FAIRY_ENERGY));
        ActivePokemon slurpuff = buildSlurpuff(List.of(FAIRY_ENERGY),
                new HashSet<>(Set.of(SpecialCondition.PARALYZED)));

        ability.onDamageReceived(ctx, slurpuff);

        assertThat(slurpuff.getConditions()).contains(SpecialCondition.PARALYZED);
    }

    @Test
    void onDamageReceived_shouldDoNothing_whenDefenderIsNull() {
        AttackContext ctx = buildContext(30, List.of(FAIRY_ENERGY));

        // Should not throw
        ability.onDamageReceived(ctx, null);
    }

    @Test
    void onDamageReceived_shouldBeNoOp_whenNoConditionsPresent() {
        AttackContext ctx = buildContext(30, List.of(FAIRY_ENERGY));
        ActivePokemon slurpuff = buildSlurpuff(List.of(FAIRY_ENERGY), new HashSet<>());

        ability.onDamageReceived(ctx, slurpuff);

        assertThat(slurpuff.getConditions()).isEmpty();
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private ActivePokemon buildSlurpuff(List<String> attachedEnergies,
                                        Set<SpecialCondition> conditions) {
        return ActivePokemon.builder()
                .instanceId("slurpuff-1")
                .cardId("xy1-95")
                .types(new ArrayList<>(List.of(EnergyType.FAIRY)))
                .attachedEnergyIds(new ArrayList<>(attachedEnergies))
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(0)
                .conditions(conditions)
                .activeEffects(new ArrayList<>())
                .build();
    }

    private AttackContext buildContext(int damageToApply, List<String> attackerEnergies) {
        ActivePokemon attacker = ActivePokemon.builder()
                .instanceId("attacker-1")
                .cardId("xy1-1")
                .types(new ArrayList<>(List.of(EnergyType.METAL)))
                .attachedEnergyIds(new ArrayList<>(attackerEnergies))
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();

        PlayerState attackerState = playerState(PLAYER_2, List.of(), cardIds(5));
        attackerState.setActivePokemon(attacker);

        PlayerState defenderState = playerState(PLAYER_1, List.of(), cardIds(5));

        BoardState state = boardState(defenderState, attackerState);

        GameAction act = GameAction.builder()
                .type(GameActionType.DECLARE_ATTACK)
                .playerId(PLAYER_2)
                .payload(Map.of("attackName", "metal claw"))
                .build();

        return AttackContext.builder()
                .boardState(state)
                .action(act)
                .attackName("metal claw")
                .damageToApply(damageToApply)
                .modifiers(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
    }
}