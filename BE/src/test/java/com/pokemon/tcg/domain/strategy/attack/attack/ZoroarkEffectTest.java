package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.engine.CoinFlipService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ZoroarkEffectTest {

    @Mock private CoinFlipService coinFlipService;

    private ZoroarkEffect effect;

    @BeforeEach
    void setUp() {
        effect = new ZoroarkEffect(coinFlipService);
    }

    @Test
    void corner_shouldApplyCantRetreat_toDefender() {
        AttackContext ctx = buildContext("Corner", List.of("xy1-132", "xy1-132"));

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState().getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getActiveEffects()).contains(PokemonEffect.CANT_RETREAT);
    }

    @Test
    void corner_shouldNotDuplicateEffect_whenAlreadyPresent() {
        AttackContext ctx = buildContext("Corner", List.of());
        ctx.getBoardState().getStateFor(PLAYER_2).getActivePokemon()
                .getActiveEffects().add(PokemonEffect.CANT_RETREAT);

        effect.apply(ctx);

        List<PokemonEffect> effects = ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getActiveEffects();
        assertThat(effects).containsOnlyOnce(PokemonEffect.CANT_RETREAT);
    }

    @Test
    void nightClaw_shouldDiscardTwoEnergies_whenTails() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.TAILS);
        AttackContext ctx = buildContext("Night Claw", List.of());
        ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon()
                .setAttachedEnergyIds(new ArrayList<>(List.of("xy1-132", "xy1-132", "xy1-133")));

        effect.apply(ctx);

        ActivePokemon attacker = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        assertThat(attacker.getAttachedEnergyIds()).hasSize(1);
        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDiscard()).hasSize(2);
    }

    @Test
    void nightClaw_shouldDiscardOnlyAvailableEnergies_whenFewerThanTwo() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.TAILS);
        AttackContext ctx = buildContext("Night Claw", List.of());
        ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon()
                .setAttachedEnergyIds(new ArrayList<>(List.of("xy1-132")));

        effect.apply(ctx);

        ActivePokemon attacker = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        assertThat(attacker.getAttachedEnergyIds()).isEmpty();
        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDiscard()).hasSize(1);
    }

    @Test
    void nightClaw_shouldNotDiscardEnergy_whenHeads() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext("Night Claw", List.of());
        ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon()
                .setAttachedEnergyIds(new ArrayList<>(List.of("xy1-132", "xy1-132")));

        effect.apply(ctx);

        ActivePokemon attacker = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        assertThat(attacker.getAttachedEnergyIds()).hasSize(2);
    }

    private AttackContext buildContext(String attackName, List<String> deck) {
        ActivePokemon zoroark = ActivePokemon.builder()
                .instanceId("zoroark-1")
                .cardId("xy1-73")
                .types(new ArrayList<>(List.of(EnergyType.DARKNESS)))
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();

        PlayerState attackerState = playerState(PLAYER_1, List.of(), new ArrayList<>(deck));
        attackerState.setActivePokemon(zoroark);

        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(ActivePokemon.builder()
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
                .build());

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