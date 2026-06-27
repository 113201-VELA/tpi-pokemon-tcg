package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

class DiglettEffectTest {

    private DiglettEffect effect;

    @BeforeEach
    void setUp() {
        effect = new DiglettEffect();
    }

    @Test
    void shouldSupportBothAttacks() {
        assertThat(effect.getSupportedAttacks())
                .contains("diglett|mine", "diglett|mud-slap");
    }

    // ─── Mine ─────────────────────────────────────────────────────────────────

    @Test
    void mine_shouldExposeTopCardOfOpponentDeck() {
        AttackContext ctx = buildContext("mine");
        String expectedTop = ctx.getBoardState()
                .getOpponentState(PLAYER_1).getDeck().get(0);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getPendingDeckSelectionCardIds())
                .containsExactly(expectedTop);
    }

    @Test
    void mine_shouldSetPendingSelectionForAttacker() {
        AttackContext ctx = buildContext("mine");

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getPendingAttackSelectionPlayerId())
                .isEqualTo(PLAYER_1);
        assertThat(ctx.getBoardState().getPendingAttackSelectionKey())
                .isEqualTo("diglett|mine");
    }

    @Test
    void mine_shouldDoNothingIfOpponentDeckIsEmpty() {
        AttackContext ctx = buildContext("mine");
        ctx.getBoardState().getOpponentState(PLAYER_1).setDeck(new ArrayList<>());

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getPendingAttackSelectionPlayerId()).isNull();
        assertThat(ctx.getBoardState().getPendingDeckSelectionCardIds()).isNull();
    }

    @Test
    void mine_shouldNotAddModifiers() {
        AttackContext ctx = buildContext("mine");

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    // ─── Mud-Slap ─────────────────────────────────────────────────────────────

    @Test
    void mudSlap_shouldNotAddModifiers() {
        AttackContext ctx = buildContext("mud-slap");

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    @Test
    void mudSlap_shouldNotSetPendingSelection() {
        AttackContext ctx = buildContext("mud-slap");

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getPendingAttackSelectionPlayerId()).isNull();
    }

    // ─── shuffleOpponentDeck ──────────────────────────────────────────────────

    @Test
    void shuffleOpponentDeck_shouldContainSameCardsInDifferentOrder() {
        PlayerState opponent = playerState(PLAYER_2, List.of(), cardIds(10));
        List<String> original = new ArrayList<>(opponent.getDeck());

        DiglettEffect.shuffleOpponentDeck(opponent);

        assertThat(opponent.getDeck()).containsExactlyInAnyOrderElementsOf(original);
    }

    @Test
    void shuffleOpponentDeck_shouldDoNothingIfDeckIsEmpty() {
        PlayerState opponent = playerState(PLAYER_2, List.of(), List.of());

        DiglettEffect.shuffleOpponentDeck(opponent);

        assertThat(opponent.getDeck()).isEmpty();
    }

    // ─── unknown attack ───────────────────────────────────────────────────────

    @Test
    void unknownAttack_shouldDoNothing() {
        AttackContext ctx = buildContext("unknown");

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
        assertThat(ctx.getBoardState().getPendingAttackSelectionPlayerId()).isNull();
    }

    // ─── helper ───────────────────────────────────────────────────────────────

    private AttackContext buildContext(String attackName) {
        ActivePokemon diglett = ActivePokemon.builder()
                .instanceId("diglett-1")
                .cardId("xy1-58")
                .types(new ArrayList<>(List.of(EnergyType.FIGHTING)))
                .attachedEnergyIds(new ArrayList<>(List.of("xy1-96", "xy1-95")))
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
        attackerState.setActivePokemon(diglett);

        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(defender);

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