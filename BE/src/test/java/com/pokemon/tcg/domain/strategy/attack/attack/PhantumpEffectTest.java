package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

class PhantumpEffectTest {

    private PhantumpEffect effect;

    @BeforeEach
    void setUp() {
        effect = new PhantumpEffect();
    }

    @Test
    void shouldSupportAstonish() {
        assertThat(effect.getSupportedAttacks()).containsExactly("phantump|astonish");
    }

    @Test
    void astonish_shouldRemoveOneCard_fromOpponentHand() {
        AttackContext ctx = buildContext("astonish",
                List.of("xy1-1", "xy1-2", "xy1-3"));

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_2).getHand()).hasSize(2);
    }

    @Test
    void astonish_shouldAddRemovedCard_toOpponentDeck() {
        AttackContext ctx = buildContext("astonish", List.of("xy1-1"));

        effect.apply(ctx);

        PlayerState opponent = ctx.getBoardState().getStateFor(PLAYER_2);
        assertThat(opponent.getHand()).isEmpty();
        assertThat(opponent.getDeck()).contains("xy1-1");
    }

    @Test
    void astonish_totalCardCountShouldRemainSame() {
        List<String> hand = List.of("xy1-1", "xy1-2", "xy1-3");
        AttackContext ctx = buildContext("astonish", hand);
        int deckSizeBefore = ctx.getBoardState().getStateFor(PLAYER_2).getDeck().size();

        effect.apply(ctx);

        PlayerState opponent = ctx.getBoardState().getStateFor(PLAYER_2);
        int total = opponent.getHand().size() + opponent.getDeck().size();
        assertThat(total).isEqualTo(hand.size() + deckSizeBefore);
    }

    @Test
    void astonish_shouldDoNothing_whenOpponentHandIsEmpty() {
        AttackContext ctx = buildContext("astonish", List.of());

        effect.apply(ctx);

        PlayerState opponent = ctx.getBoardState().getStateFor(PLAYER_2);
        assertThat(opponent.getHand()).isEmpty();
    }

    @Test
    void astonish_shouldNotAffectAttacker() {
        AttackContext ctx = buildContext("astonish", List.of("xy1-1"));
        List<String> attackerHandBefore = new ArrayList<>(
                ctx.getBoardState().getStateFor(PLAYER_1).getHand());

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getHand())
                .isEqualTo(attackerHandBefore);
    }

    @Test
    void astonish_shouldNotAddModifiers() {
        AttackContext ctx = buildContext("astonish", List.of("xy1-1"));

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    @Test
    void unknownAttack_shouldDoNothing() {
        AttackContext ctx = buildContext("hook", List.of("xy1-1"));

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_2).getHand())
                .containsExactly("xy1-1");
    }

    private AttackContext buildContext(String attackName, List<String> opponentHand) {
        ActivePokemon phantump = ActivePokemon.builder()
                .instanceId("phantump-1")
                .cardId("xy1-54")
                .types(new ArrayList<>(List.of(EnergyType.PSYCHIC)))
                .attachedEnergyIds(new ArrayList<>(List.of("xy1-95")))
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
                .types(new ArrayList<>(List.of(EnergyType.DARKNESS)))
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();

        PlayerState attackerState = playerState(PLAYER_1, List.of(), cardIds(5));
        attackerState.setActivePokemon(phantump);

        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(3));
        defenderState.setActivePokemon(defender);
        defenderState.setHand(new ArrayList<>(opponentHand));

        BoardState state = boardState(attackerState, defenderState);

        GameAction act = GameAction.builder()
                .type(GameActionType.DECLARE_ATTACK)
                .playerId(PLAYER_1)
                .payload(Map.of("attackName", attackName))
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