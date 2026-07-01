package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.DamageModifier;
import com.pokemon.tcg.engine.CoinFlipService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GurdurrEffectTest {

    private CoinFlipService coinFlipService;
    private GurdurrEffect effect;

    @BeforeEach
    void setUp() {
        coinFlipService = mock(CoinFlipService.class);
        effect = new GurdurrEffect(coinFlipService);
    }

    @Test
    void shouldSupportBothAttacks() {
        assertThat(effect.getSupportedAttacks())
                .contains("gurdurr|pummel", "gurdurr|hammer arm");
    }

    // ─── Pummel ───────────────────────────────────────────────────────────────

    @Test
    void pummel_shouldAdd20Modifier_onHeads() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext("pummel", 5);

        effect.apply(ctx);

        int bonus = ctx.getModifiers().stream()
                .mapToInt(DamageModifier::amount).sum();
        assertThat(bonus).isEqualTo(20);
    }

    @Test
    void pummel_shouldNotAddModifier_onTails() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.TAILS);
        AttackContext ctx = buildContext("pummel", 5);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    @Test
    void pummel_shouldFlipExactlyOneCoin() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext("pummel", 5);

        effect.apply(ctx);

        verify(coinFlipService, times(1)).flip();
    }

    // ─── Hammer Arm ───────────────────────────────────────────────────────────

    @Test
    void hammerArm_shouldDiscardTopCardOfOpponentDeck() {
        AttackContext ctx = buildContext("hammer arm", 3);
        String expectedTop = ctx.getBoardState()
                .getOpponentState(PLAYER_1).getDeck().get(0);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getOpponentState(PLAYER_1).getDiscard())
                .contains(expectedTop);
    }

    @Test
    void hammerArm_shouldRemoveTopCardFromOpponentDeck() {
        AttackContext ctx = buildContext("hammer arm", 3);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getOpponentState(PLAYER_1).getDeck())
                .hasSize(2);
    }

    @Test
    void hammerArm_shouldDoNothing_whenOpponentDeckIsEmpty() {
        AttackContext ctx = buildContext("hammer arm", 0);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getOpponentState(PLAYER_1).getDeck()).isEmpty();
        assertThat(ctx.getBoardState().getOpponentState(PLAYER_1).getDiscard()).isEmpty();
    }

    @Test
    void hammerArm_shouldNotFlipCoin() {
        AttackContext ctx = buildContext("hammer arm", 3);

        effect.apply(ctx);

        verifyNoInteractions(coinFlipService);
    }

    @Test
    void hammerArm_shouldNotAddModifiers() {
        AttackContext ctx = buildContext("hammer arm", 3);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    // ─── unknown attack ───────────────────────────────────────────────────────

    @Test
    void unknownAttack_shouldDoNothing() {
        AttackContext ctx = buildContext("unknown", 3);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
        verifyNoInteractions(coinFlipService);
    }

    // ─── helper ───────────────────────────────────────────────────────────────

    private AttackContext buildContext(String attackName, int opponentDeckSize) {
        ActivePokemon gurdurr = ActivePokemon.builder()
                .instanceId("gurdurr-1")
                .cardId("xy1-66")
                .types(new ArrayList<>(List.of(EnergyType.FIGHTING)))
                .attachedEnergyIds(new ArrayList<>(
                        List.of("xy1-96", "xy1-95", "xy1-95")))
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
        attackerState.setActivePokemon(gurdurr);

        List<String> opponentDeck = opponentDeckSize > 0
                ? cardIds(opponentDeckSize) : new ArrayList<>();
        PlayerState defenderState = playerState(PLAYER_2, List.of(), opponentDeck);
        defenderState.setActivePokemon(defender);
        defenderState.setDiscard(new ArrayList<>());

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