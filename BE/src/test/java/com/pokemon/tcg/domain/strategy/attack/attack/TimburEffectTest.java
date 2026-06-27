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

class TimburEffectTest {

    private CoinFlipService coinFlipService;
    private TimburEffect effect;

    @BeforeEach
    void setUp() {
        coinFlipService = mock(CoinFlipService.class);
        effect = new TimburEffect(coinFlipService);
    }

    @Test
    void shouldSupportPummel() {
        assertThat(effect.getSupportedAttacks()).containsExactly("timburr|pummel");
    }

    @Test
    void pummel_shouldAdd20Modifier_onHeads() {
        when(coinFlipService.flip()).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext();

        effect.apply(ctx);

        int bonus = ctx.getModifiers().stream()
                .mapToInt(DamageModifier::amount).sum();
        assertThat(bonus).isEqualTo(20);
    }

    @Test
    void pummel_shouldNotAddModifier_onTails() {
        when(coinFlipService.flip()).thenReturn(CoinResult.TAILS);
        AttackContext ctx = buildContext();

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    @Test
    void pummel_shouldFlipExactlyOneCoin() {
        when(coinFlipService.flip()).thenReturn(CoinResult.TAILS);
        AttackContext ctx = buildContext();

        effect.apply(ctx);

        verify(coinFlipService, times(1)).flip();
    }

    // ─── helper ───────────────────────────────────────────────────────────────

    private AttackContext buildContext() {
        ActivePokemon timburr = ActivePokemon.builder()
                .instanceId("timburr-1")
                .cardId("xy1-65")
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
        attackerState.setActivePokemon(timburr);

        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(defender);

        BoardState state = boardState(attackerState, defenderState);

        GameAction act = GameAction.builder()
                .type(GameActionType.DECLARE_ATTACK)
                .playerId(PLAYER_1)
                .payload(Map.of("attackName", "pummel"))
                .build();

        return AttackContext.builder()
                .boardState(state)
                .action(act)
                .attackName("pummel")
                .modifiers(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
    }
}