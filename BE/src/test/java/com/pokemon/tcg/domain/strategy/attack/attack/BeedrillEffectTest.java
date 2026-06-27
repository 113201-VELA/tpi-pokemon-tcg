package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.engine.CoinFlipService;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
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
class BeedrillEffectTest {

    @Mock
    private CoinFlipService coinFlipService;

    private BeedrillEffect effect;

    @BeforeEach
    void setUp() {
        effect = new BeedrillEffect(coinFlipService);
    }

    @Test
    void poisonJab_shouldPoisonDefender() {
        AttackContext ctx = buildContext("Poison Jab");

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState()
                .getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getConditions()).contains(SpecialCondition.POISONED);
    }

    @Test
    void poisonJab_shouldNotAddDamageModifiers() {
        AttackContext ctx = buildContext("Poison Jab");

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    @Test
    void poisonJab_shouldNotAffectAttacker() {
        AttackContext ctx = buildContext("Poison Jab");

        effect.apply(ctx);

        ActivePokemon attacker = ctx.getBoardState()
                .getStateFor(PLAYER_1).getActivePokemon();
        assertThat(attacker.getConditions()).isEmpty();
        assertThat(attacker.getActiveEffects()).isEmpty();
    }

    @Test
    void flashNeedle_shouldAddZeroDamage_whenAllTails() {
        when(coinFlipService.flip()).thenReturn(CoinResult.TAILS);
        AttackContext ctx = buildContext("Flash Needle");

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    @Test
    void flashNeedle_shouldAdd40Damage_whenOneHead() {
        when(coinFlipService.flip())
                .thenReturn(CoinResult.HEADS)
                .thenReturn(CoinResult.TAILS)
                .thenReturn(CoinResult.TAILS);
        AttackContext ctx = buildContext("Flash Needle");

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).hasSize(1);
        assertThat(ctx.getModifiers().get(0).amount()).isEqualTo(40);
        assertThat(ctx.getModifiers().get(0).beforeWeakness()).isTrue();
    }

    @Test
    void flashNeedle_shouldAdd80Damage_whenTwoHeads() {
        when(coinFlipService.flip())
                .thenReturn(CoinResult.HEADS)
                .thenReturn(CoinResult.HEADS)
                .thenReturn(CoinResult.TAILS);
        AttackContext ctx = buildContext("Flash Needle");

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).hasSize(1);
        assertThat(ctx.getModifiers().get(0).amount()).isEqualTo(80);
    }

    @Test
    void flashNeedle_shouldAdd120Damage_whenThreeHeads() {
        when(coinFlipService.flip()).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext("Flash Needle");

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).hasSize(1);
        assertThat(ctx.getModifiers().get(0).amount()).isEqualTo(120);
    }

    @Test
    void flashNeedle_shouldApplyInvulnerable_whenAllThreeHeads() {
        when(coinFlipService.flip()).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext("Flash Needle");

        effect.apply(ctx);

        ActivePokemon beedrill = ctx.getBoardState()
                .getStateFor(PLAYER_1).getActivePokemon();
        assertThat(beedrill.getActiveEffects()).contains(PokemonEffect.INVULNERABLE);
    }

    @Test
    void flashNeedle_shouldNotApplyInvulnerable_whenLessThanThreeHeads() {
        when(coinFlipService.flip())
                .thenReturn(CoinResult.HEADS)
                .thenReturn(CoinResult.HEADS)
                .thenReturn(CoinResult.TAILS);
        AttackContext ctx = buildContext("Flash Needle");

        effect.apply(ctx);

        ActivePokemon beedrill = ctx.getBoardState()
                .getStateFor(PLAYER_1).getActivePokemon();
        assertThat(beedrill.getActiveEffects()).doesNotContain(PokemonEffect.INVULNERABLE);
    }

    @Test
    void flashNeedle_shouldNotDuplicateInvulnerable_whenAlreadyActive() {
        when(coinFlipService.flip()).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext("Flash Needle");
        ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getActiveEffects().add(PokemonEffect.INVULNERABLE);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getActiveEffects())
                .containsExactly(PokemonEffect.INVULNERABLE);
    }

    private AttackContext buildContext(String attackName) {
        ActivePokemon beedrill = ActivePokemon.builder()
                .instanceId("beedrill-1")
                .cardId("xy1-5")
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
                .cardId("xy1-10")
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
        attackerState.setActivePokemon(beedrill);
        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(defender);

        BoardState state = boardState(attackerState, defenderState);
        GameAction act = action(GameActionType.DECLARE_ATTACK, PLAYER_1,
                "attackName", attackName);

        return AttackContext.builder()
                .boardState(state)
                .action(act)
                .attackName(attackName)
                .modifiers(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
    }
}
