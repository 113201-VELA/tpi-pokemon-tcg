package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.Card;
import com.pokemon.tcg.domain.model.card.CardType;
import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.engine.CardLookupPort;
import com.pokemon.tcg.domain.strategy.attack.DamageModifier;
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
class GogoatEffectTest {

    @Mock
    private CardLookupPort cardLookupPort;

    private GogoatEffect effect;

    private static final String SUPPORTER_1     = "xy1-122";
    private static final String SUPPORTER_2     = "xy1-127";
    private static final String NON_SUPPORTER   = "xy1-3";

    @BeforeEach
    void setUp() {
        effect = new GogoatEffect(cardLookupPort);
    }

    @Test
    void lead_shouldSetPendingAttackSelection_withMaxCards2() {
        when(cardLookupPort.findCardById(SUPPORTER_1))
                .thenReturn(Optional.of(supporterCard(SUPPORTER_1)));
        AttackContext ctx = buildContext("Lead", List.of(SUPPORTER_1), false, 0);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().isPendingAttackSelection()).isTrue();
        assertThat(ctx.getBoardState().getPendingAttackSelectionMaxCards()).isEqualTo(2);
        assertThat(ctx.getBoardState().getPendingAttackSelectionKey())
                .isEqualTo("gogoat|lead");
    }

    @Test
    void lead_shouldIncludeBothSupporters_inPendingCards() {
        when(cardLookupPort.findCardById(SUPPORTER_1))
                .thenReturn(Optional.of(supporterCard(SUPPORTER_1)));
        when(cardLookupPort.findCardById(SUPPORTER_2))
                .thenReturn(Optional.of(supporterCard(SUPPORTER_2)));
        AttackContext ctx = buildContext("Lead",
                List.of(SUPPORTER_1, SUPPORTER_2), false, 0);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getPendingDeckSelectionCardIds())
                .containsExactlyInAnyOrder(SUPPORTER_1, SUPPORTER_2);
    }

    @Test
    void lead_shouldExcludeNonSupporters_fromPendingCards() {
        when(cardLookupPort.findCardById(SUPPORTER_1))
                .thenReturn(Optional.of(supporterCard(SUPPORTER_1)));
        when(cardLookupPort.findCardById(NON_SUPPORTER))
                .thenReturn(Optional.of(nonSupporterCard(NON_SUPPORTER)));
        AttackContext ctx = buildContext("Lead",
                List.of(SUPPORTER_1, NON_SUPPORTER), false, 0);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getPendingDeckSelectionCardIds())
                .containsExactly(SUPPORTER_1);
    }

    @Test
    void lead_shouldDoNothing_whenNoSupportersInDeck() {
        when(cardLookupPort.findCardById(NON_SUPPORTER))
                .thenReturn(Optional.of(nonSupporterCard(NON_SUPPORTER)));
        AttackContext ctx = buildContext("Lead", List.of(NON_SUPPORTER), false, 0);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().isPendingAttackSelection()).isFalse();
    }

    @Test
    void chargeDash_shouldAdd20DamageModifier_whenChargeBoostIsTrue() {
        AttackContext ctx = buildContext("Charge Dash", List.of(), true, 0);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).hasSize(1);
        DamageModifier mod = ctx.getModifiers().get(0);
        assertThat(mod.amount()).isEqualTo(20);
        assertThat(mod.beforeWeakness()).isTrue();
        assertThat(mod.source()).isEqualTo("charge-dash-boost");
    }

    @Test
    void chargeDash_shouldApply2RecoilCounters_whenChargeBoostIsTrue() {
        AttackContext ctx = buildContext("Charge Dash", List.of(), true, 0);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getDamageCounters()).isEqualTo(2);
    }

    @Test
    void chargeDash_shouldAccumulateRecoilCounters_whenGogoatAlreadyDamaged() {
        AttackContext ctx = buildContext("Charge Dash", List.of(), true, 3);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getDamageCounters()).isEqualTo(5);
    }

    @Test
    void chargeDash_shouldNotAddModifierOrRecoil_whenChargeBoostIsFalse() {
        AttackContext ctx = buildContext("Charge Dash", List.of(), false, 0);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getDamageCounters()).isEqualTo(0);
    }

    @Test
    void chargeDash_shouldNotAffectDefender() {
        AttackContext ctx = buildContext("Charge Dash", List.of(), true, 0);
        int defenderCounters = ctx.getBoardState()
                .getStateFor(PLAYER_2).getActivePokemon().getDamageCounters();

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getDamageCounters()).isEqualTo(defenderCounters);
    }

    private AttackContext buildContext(String attackName, List<String> deck,
                                      boolean chargeBoost, int gogoatCounters) {
        ActivePokemon gogoat = ActivePokemon.builder()
                .instanceId("gogoat-1")
                .cardId("xy1-19")
                .types(new ArrayList<>(List.of(EnergyType.GRASS)))
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(gogoatCounters)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();

        PlayerState attackerState = playerState(PLAYER_1, List.of(), new ArrayList<>(deck));
        attackerState.setActivePokemon(gogoat);

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

        Map<String, Object> payload = new HashMap<>();
        payload.put("attackName", attackName);
        payload.put("chargeBoost", chargeBoost);

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
