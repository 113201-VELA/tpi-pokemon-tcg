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

class PanpourEffectTest {

    private CoinFlipService coinFlipService;
    private PanpourEffect effect;

    @BeforeEach
    void setUp() {
        coinFlipService = mock(CoinFlipService.class);
        effect = new PanpourEffect(coinFlipService);
    }

    @Test
    void shouldSupportWaterSplash() {
        assertThat(effect.getSupportedAttacks())
                .containsExactly("panpour|water splash");
    }

    @Test
    void waterSplash_shouldAdd20Damage_onHeads() {
        when(coinFlipService.flip()).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext("water splash");

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).hasSize(1);
        assertThat(ctx.getModifiers().get(0).amount()).isEqualTo(20);
    }

    @Test
    void waterSplash_shouldNotAddDamage_onTails() {
        when(coinFlipService.flip()).thenReturn(CoinResult.TAILS);
        AttackContext ctx = buildContext("water splash");

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    @Test
    void unknownAttack_shouldDoNothing() {
        AttackContext ctx = buildContext("wave splash");

        effect.apply(ctx);

        verifyNoInteractions(coinFlipService);
        assertThat(ctx.getModifiers()).isEmpty();
    }

    private AttackContext buildContext(String attackName) {
        ActivePokemon panpour = ActivePokemon.builder()
                .instanceId("panpour-1")
                .cardId("xy1-37")
                .types(new ArrayList<>(List.of(EnergyType.WATER)))
                .attachedEnergyIds(new ArrayList<>(List.of("xy1-131", "xy1-131")))
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
        attackerState.setActivePokemon(panpour);
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