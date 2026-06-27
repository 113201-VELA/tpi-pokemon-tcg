package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

class FletchlingEffectTest {

    private FletchlingEffect effect;

    @BeforeEach
    void setUp() {
        effect = new FletchlingEffect();
    }

    @Test
    void getSupportedAttacks_shouldReturn_fletchlingMeFirst() {
        assertThat(effect.getSupportedAttacks())
                .containsExactly("fletchling|me first");
    }

    @Test
    void apply_shouldDraw1Card_whenDeckHasCards() {
        AttackContext ctx = buildContext(cardIds(5));

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getHand()).hasSize(1);
        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDeck()).hasSize(4);
    }

    @Test
    void apply_shouldDrawTopCard_fromDeck() {
        List<String> deck = new ArrayList<>(List.of("xy1-1", "xy1-2", "xy1-3"));
        AttackContext ctx = buildContext(deck);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getHand())
                .containsExactly("xy1-1");
        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDeck())
                .containsExactly("xy1-2", "xy1-3");
    }

    @Test
    void apply_shouldDoNothing_whenDeckIsEmpty() {
        AttackContext ctx = buildContext(List.of());

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getHand()).isEmpty();
        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDeck()).isEmpty();
    }

    @Test
    void apply_shouldNotAffectDefender() {
        AttackContext ctx = buildContext(cardIds(5));
        int defenderCounters = ctx.getBoardState()
                .getStateFor(PLAYER_2).getActivePokemon().getDamageCounters();

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getDamageCounters()).isEqualTo(defenderCounters);
    }

    @Test
    void apply_shouldNotAddModifiers() {
        AttackContext ctx = buildContext(cardIds(5));

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    private AttackContext buildContext(List<String> deck) {
        ActivePokemon fletchling = ActivePokemon.builder()
                .instanceId("fletchling-1")
                .cardId("xy1-113")
                .types(new ArrayList<>(List.of(EnergyType.COLORLESS)))
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
                .cardId("xy1-20")
                .types(new ArrayList<>(List.of(EnergyType.FIRE)))
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
        attackerState.setActivePokemon(fletchling);
        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(defender);

        BoardState state = boardState(attackerState, defenderState);
        GameAction act = action(GameActionType.DECLARE_ATTACK, PLAYER_1,
                "attackName", "Me First");

        return AttackContext.builder()
                .boardState(state)
                .action(act)
                .attackName("Me First")
                .modifiers(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
    }
}
