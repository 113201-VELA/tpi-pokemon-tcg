package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

class SimipourEffectTest {

    private SimipourEffect effect;

    private static final String CARD_IN_DISCARD = "xy1-131";

    @BeforeEach
    void setUp() {
        effect = new SimipourEffect();
    }

    @Test
    void shouldSupportRecycle() {
        assertThat(effect.getSupportedAttacks())
                .containsExactly("simipour|recycle");
    }

    @Test
    void recycle_shouldMoveCardFromDiscard_toTopOfDeck() {
        AttackContext ctx = buildContext("recycle", List.of(CARD_IN_DISCARD), CARD_IN_DISCARD);

        effect.apply(ctx);

        PlayerState attacker = ctx.getBoardState().getStateFor(PLAYER_1);
        assertThat(attacker.getDiscard()).doesNotContain(CARD_IN_DISCARD);
        assertThat(attacker.getDeck().get(0)).isEqualTo(CARD_IN_DISCARD);
    }

    @Test
    void recycle_shouldPlaceCard_atTopOfDeck_notBottom() {
        AttackContext ctx = buildContext("recycle",
                List.of(CARD_IN_DISCARD), CARD_IN_DISCARD);

        effect.apply(ctx);

        PlayerState attacker = ctx.getBoardState().getStateFor(PLAYER_1);
        assertThat(attacker.getDeck().get(0)).isEqualTo(CARD_IN_DISCARD);
    }

    @Test
    void recycle_shouldDoNothing_whenCardIdIsNull() {
        AttackContext ctx = buildContext("recycle", List.of(CARD_IN_DISCARD), null);

        effect.apply(ctx);

        PlayerState attacker = ctx.getBoardState().getStateFor(PLAYER_1);
        assertThat(attacker.getDiscard()).contains(CARD_IN_DISCARD);
    }

    @Test
    void recycle_shouldDoNothing_whenCardNotInDiscard() {
        AttackContext ctx = buildContext("recycle", List.of(), CARD_IN_DISCARD);

        effect.apply(ctx);

        PlayerState attacker = ctx.getBoardState().getStateFor(PLAYER_1);
        assertThat(attacker.getDeck()).doesNotContain(CARD_IN_DISCARD);
    }

    @Test
    void recycle_shouldNotAffectOpponent() {
        AttackContext ctx = buildContext("recycle", List.of(CARD_IN_DISCARD), CARD_IN_DISCARD);
        List<String> opponentDeckBefore = new ArrayList<>(
                ctx.getBoardState().getStateFor(PLAYER_2).getDeck());

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_2).getDeck())
                .isEqualTo(opponentDeckBefore);
    }

    @Test
    void unknownAttack_shouldDoNothing() {
        AttackContext ctx = buildContext("surf", List.of(CARD_IN_DISCARD), CARD_IN_DISCARD);

        effect.apply(ctx);

        PlayerState attacker = ctx.getBoardState().getStateFor(PLAYER_1);
        assertThat(attacker.getDiscard()).contains(CARD_IN_DISCARD);
    }

    private AttackContext buildContext(String attackName,
                                       List<String> discard,
                                       String cardToRecycle) {
        ActivePokemon simipour = ActivePokemon.builder()
                .instanceId("simipour-1")
                .cardId("xy1-38")
                .types(new ArrayList<>(List.of(EnergyType.WATER)))
                .attachedEnergyIds(new ArrayList<>(
                        List.of("xy1-131", "xy1-131", "xy1-131")))
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();

        ActivePokemon defender = ActivePokemon.builder()
                .instanceId("def-1")
                .cardId("xy1-1")
                .types(new ArrayList<>(List.of(EnergyType.GRASS)))
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();

        PlayerState attackerState = playerState(PLAYER_1, List.of(), cardIds(5));
        attackerState.setActivePokemon(simipour);
        attackerState.setDiscard(new ArrayList<>(discard));

        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(defender);

        BoardState state = boardState(attackerState, defenderState);

        Map<String, Object> payload = new HashMap<>();
        payload.put("attackName", attackName);
        if (cardToRecycle != null) {
            payload.put("cardId", cardToRecycle);
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
}