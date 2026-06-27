package com.pokemon.tcg.domain.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.engine.CoinFlipService;
import com.pokemon.tcg.engine.DamageModifier;
import com.pokemon.tcg.engine.attack.AttackContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChespinEffectTest {

    @Mock
    private CoinFlipService coinFlipService;

    private ChespinEffect effect;

    @BeforeEach
    void setUp() {
        effect = new ChespinEffect(coinFlipService);
    }

    @Test
    void apply_shouldAddZeroDamage_whenAllTails() {
        when(coinFlipService.flip()).thenReturn(CoinResult.TAILS);
        AttackContext ctx = buildContext();

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    @Test
    void apply_shouldAdd10Damage_whenOneHead() {
        when(coinFlipService.flip())
                .thenReturn(CoinResult.HEADS)
                .thenReturn(CoinResult.TAILS)
                .thenReturn(CoinResult.TAILS)
                .thenReturn(CoinResult.TAILS);
        AttackContext ctx = buildContext();

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).hasSize(1);
        assertThat(ctx.getModifiers().get(0).amount()).isEqualTo(10);
        assertThat(ctx.getModifiers().get(0).beforeWeakness()).isTrue();
    }

    @Test
    void apply_shouldAdd20Damage_whenTwoHeads() {
        when(coinFlipService.flip())
                .thenReturn(CoinResult.HEADS)
                .thenReturn(CoinResult.HEADS)
                .thenReturn(CoinResult.TAILS)
                .thenReturn(CoinResult.TAILS);
        AttackContext ctx = buildContext();

        effect.apply(ctx);

        assertThat(ctx.getModifiers().get(0).amount()).isEqualTo(20);
    }

    @Test
    void apply_shouldAdd30Damage_whenThreeHeads() {
        when(coinFlipService.flip())
                .thenReturn(CoinResult.HEADS)
                .thenReturn(CoinResult.HEADS)
                .thenReturn(CoinResult.HEADS)
                .thenReturn(CoinResult.TAILS);
        AttackContext ctx = buildContext();

        effect.apply(ctx);

        assertThat(ctx.getModifiers().get(0).amount()).isEqualTo(30);
    }

    @Test
    void apply_shouldAdd40Damage_whenAllHeads() {
        when(coinFlipService.flip()).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext();

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).hasSize(1);
        assertThat(ctx.getModifiers().get(0).amount()).isEqualTo(40);
    }

    @Test
    void apply_shouldPreserveExistingModifiers() {
        when(coinFlipService.flip()).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext();
        ctx.getModifiers().add(new DamageModifier("existing", 10, true));

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).hasSize(2);
    }

    @Test
    void apply_shouldUseSourceName_pinMissileHeads() {
        when(coinFlipService.flip()).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext();

        effect.apply(ctx);

        assertThat(ctx.getModifiers().get(0).source()).isEqualTo("pin-missile-heads");
    }

    private AttackContext buildContext() {
        ActivePokemon chespin = ActivePokemon.builder()
                .instanceId("chespin-1")
                .cardId("xy1-12")
                .types(new ArrayList<>(List.of(EnergyType.GRASS)))
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

        PlayerState attackerState = playerState(PLAYER_1, List.of(), cardIds(5));
        attackerState.setActivePokemon(chespin);
        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(defender);

        BoardState state = boardState(attackerState, defenderState);
        GameAction act = action(GameActionType.DECLARE_ATTACK, PLAYER_1,
                "attackName", "Pin Missile");

        return AttackContext.builder()
                .boardState(state)
                .action(act)
                .attackName("Pin Missile")
                .modifiers(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
    }
}
