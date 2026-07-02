package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.engine.CoinFlipService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class WhirlipedeEffectTest {

    private CoinFlipService coinFlipService;
    private WhirlipedeEffect effect;

    @BeforeEach
    void setUp() {
        coinFlipService = mock(CoinFlipService.class);
        effect = new WhirlipedeEffect(coinFlipService);
    }

    @Test
    void shouldSupportContinuousTumble() {
        assertThat(effect.getSupportedAttacks())
                .containsExactly("whirlipede|continuous tumble");
    }

    @Test
    void continuousTumble_shouldAdd90Damage_when3Heads() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString()))
                .thenReturn(CoinResult.HEADS)
                .thenReturn(CoinResult.HEADS)
                .thenReturn(CoinResult.HEADS)
                .thenReturn(CoinResult.TAILS);
        AttackContext ctx = buildContext("continuous tumble");

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).hasSize(1);
        assertThat(ctx.getModifiers().get(0).amount()).isEqualTo(90);
    }

    @Test
    void continuousTumble_shouldAdd0Damage_whenFirstFlipIsTails() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.TAILS);
        AttackContext ctx = buildContext("continuous tumble");

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    @Test
    void continuousTumble_shouldStopFlipping_afterFirstTails() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString()))
                .thenReturn(CoinResult.HEADS)
                .thenReturn(CoinResult.TAILS);
        AttackContext ctx = buildContext("continuous tumble");

        effect.apply(ctx);

        verify(coinFlipService, times(2)).flipAndEmit(any(AttackContext.class), anyString());
        assertThat(ctx.getModifiers().get(0).amount()).isEqualTo(30);
    }

    @Test
    void unknownAttack_shouldDoNothing() {
        AttackContext ctx = buildContext("unknown");

        effect.apply(ctx);

        verifyNoInteractions(coinFlipService);
        assertThat(ctx.getModifiers()).isEmpty();
    }

    private AttackContext buildContext(String attackName) {
        ActivePokemon whirlipede = ActivePokemon.builder()
                .instanceId("whirlipede-1")
                .cardId("xy1-52")
                .types(new ArrayList<>(List.of(EnergyType.PSYCHIC)))
                .attachedEnergyIds(new ArrayList<>(List.of("xy1-95", "xy1-95")))
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
        attackerState.setActivePokemon(whirlipede);
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