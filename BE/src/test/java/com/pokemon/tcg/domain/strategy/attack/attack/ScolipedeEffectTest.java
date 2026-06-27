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

class ScolipedeEffectTest {

    private CoinFlipService coinFlipService;
    private ScolipedeEffect effect;

    @BeforeEach
    void setUp() {
        coinFlipService = mock(CoinFlipService.class);
        effect = new ScolipedeEffect(coinFlipService);
    }

    @Test
    void shouldSupportBothAttacks() {
        assertThat(effect.getSupportedAttacks())
                .contains("scolipede|random peck", "scolipede|poison ring");
    }

    // ─── Random Peck ──────────────────────────────────────────────────────────

    @Test
    void randomPeck_shouldAdd40Damage_whenBothHeads() {
        when(coinFlipService.flip()).thenReturn(CoinResult.HEADS, CoinResult.HEADS);
        AttackContext ctx = buildContext("random peck");

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).hasSize(1);
        assertThat(ctx.getModifiers().get(0).amount()).isEqualTo(40);
    }

    @Test
    void randomPeck_shouldAdd20Damage_whenOneHead() {
        when(coinFlipService.flip()).thenReturn(CoinResult.HEADS, CoinResult.TAILS);
        AttackContext ctx = buildContext("random peck");

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).hasSize(1);
        assertThat(ctx.getModifiers().get(0).amount()).isEqualTo(20);
    }

    @Test
    void randomPeck_shouldAdd0Damage_whenBothTails() {
        when(coinFlipService.flip()).thenReturn(CoinResult.TAILS, CoinResult.TAILS);
        AttackContext ctx = buildContext("random peck");

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    @Test
    void randomPeck_shouldFlipExactly2Coins() {
        when(coinFlipService.flip()).thenReturn(CoinResult.TAILS);
        AttackContext ctx = buildContext("random peck");

        effect.apply(ctx);

        verify(coinFlipService, times(2)).flip();
    }

    // ─── Poison Ring ──────────────────────────────────────────────────────────

    @Test
    void poisonRing_shouldPoisonDefender() {
        AttackContext ctx = buildContext("poison ring");

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getConditions())
                .contains(SpecialCondition.POISONED);
    }

    @Test
    void poisonRing_shouldAddCantRetreat_toDefender() {
        AttackContext ctx = buildContext("poison ring");

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getActiveEffects())
                .contains(PokemonEffect.CANT_RETREAT);
    }

    @Test
    void poisonRing_shouldNotDuplicateCantRetreat_whenAlreadyPresent() {
        AttackContext ctx = buildContext("poison ring");
        ctx.getBoardState().getStateFor(PLAYER_2).getActivePokemon()
                .setActiveEffects(new ArrayList<>(List.of(PokemonEffect.CANT_RETREAT)));

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getActiveEffects()).hasSize(1);
    }

    @Test
    void poisonRing_shouldNotAffectAttacker() {
        AttackContext ctx = buildContext("poison ring");

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getConditions()).isEmpty();
        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getActiveEffects()).isEmpty();
    }

    @Test
    void poisonRing_shouldNotAddModifiers() {
        AttackContext ctx = buildContext("poison ring");

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    // ─── unknown attack ───────────────────────────────────────────────────────

    @Test
    void unknownAttack_shouldDoNothing() {
        AttackContext ctx = buildContext("unknown");

        effect.apply(ctx);

        verifyNoInteractions(coinFlipService);
        assertThat(ctx.getModifiers()).isEmpty();
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private AttackContext buildContext(String attackName) {
        ActivePokemon scolipede = ActivePokemon.builder()
                .instanceId("scolipede-1")
                .cardId("xy1-53")
                .types(new ArrayList<>(List.of(EnergyType.PSYCHIC)))
                .attachedEnergyIds(new ArrayList<>(
                        List.of("xy1-95", "xy1-95", "xy1-95", "xy1-95")))
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
        attackerState.setActivePokemon(scolipede);
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