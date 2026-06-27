package com.pokemon.tcg.domain.strategy.ability.ability;

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

class VoltorbDestinyBurstAbilityTest {

    private CoinFlipService coinFlipService;
    private VoltorbAbility ability;

    @BeforeEach
    void setUp() {
        coinFlipService = mock(CoinFlipService.class);
        ability = new VoltorbAbility(coinFlipService);
    }

    @Test
    void shouldHaveCorrectIdentifier() {
        assertThat(ability.getIdentifier()).isEqualTo("voltorb");
    }

    @Test
    void destinyBurst_onHeads_shouldAdd5CountersToAttacker_whenKnockedOut() {
        when(coinFlipService.flip()).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext(true, 10); // KO'd, damage > 0

        ActivePokemon voltorb = buildVoltorb();
        ability.onDamageReceived(ctx, voltorb);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getDamageCounters()).isEqualTo(5);
    }

    @Test
    void destinyBurst_onTails_shouldNotDamageAttacker() {
        when(coinFlipService.flip()).thenReturn(CoinResult.TAILS);
        AttackContext ctx = buildContext(true, 10);

        ActivePokemon voltorb = buildVoltorb();
        ability.onDamageReceived(ctx, voltorb);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getDamageCounters()).isEqualTo(0);
    }

    @Test
    void destinyBurst_shouldNotTrigger_whenVoltorbNotKnockedOut() {
        when(coinFlipService.flip()).thenReturn(CoinResult.HEADS);
        // Defender still alive (active not null)
        AttackContext ctx = buildContext(false, 10);

        ActivePokemon voltorb = buildVoltorb();
        ability.onDamageReceived(ctx, voltorb);

        verifyNoInteractions(coinFlipService);
        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getDamageCounters()).isEqualTo(0);
    }

    @Test
    void destinyBurst_shouldNotTrigger_whenDamageIsZero() {
        when(coinFlipService.flip()).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext(true, 0); // no damage applied

        ActivePokemon voltorb = buildVoltorb();
        ability.onDamageReceived(ctx, voltorb);

        verifyNoInteractions(coinFlipService);
        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getDamageCounters()).isEqualTo(0);
    }

    @Test
    void destinyBurst_shouldAccumulateOnExistingDamage() {
        when(coinFlipService.flip()).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext(true, 10);
        ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().setDamageCounters(3);

        ActivePokemon voltorb = buildVoltorb();
        ability.onDamageReceived(ctx, voltorb);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getDamageCounters()).isEqualTo(8);
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private ActivePokemon buildVoltorb() {
        return ActivePokemon.builder()
                .instanceId("voltorb-1")
                .cardId("xy1-44")
                .types(new ArrayList<>(List.of(EnergyType.LIGHTNING)))
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();
    }

    /**
     * Builds context where:
     * - PLAYER_1 is the attacker
     * - PLAYER_2 is the defender (Voltorb)
     * - If knocked is true, defender's active is set to null (simulating KO)
     */
    private AttackContext buildContext(boolean knocked, int damageToApply) {
        ActivePokemon attacker = ActivePokemon.builder()
                .instanceId("attacker-1")
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
        attackerState.setActivePokemon(attacker);

        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        if (knocked) {
            defenderState.setActivePokemon(null); // simulates KO
        } else {
            defenderState.setActivePokemon(buildVoltorb());
        }

        BoardState state = boardState(attackerState, defenderState);

        GameAction act = GameAction.builder()
                .type(GameActionType.DECLARE_ATTACK)
                .playerId(PLAYER_1)
                .payload(Map.of("attackName", "Rollout"))
                .build();

        return AttackContext.builder()
                .boardState(state)
                .action(act)
                .attackName("rollout")
                .damageToApply(damageToApply)
                .modifiers(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
    }
}