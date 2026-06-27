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

class KrookodileEffectTest {

    private CoinFlipService coinFlipService;
    private KrookodileEffect effect;

    @BeforeEach
    void setUp() {
        coinFlipService = mock(CoinFlipService.class);
        effect = new KrookodileEffect(coinFlipService);
    }

    @Test
    void shouldSupportBothAttacks() {
        assertThat(effect.getSupportedAttacks())
                .contains("krookodile|bother", "krookodile|knock back");
    }

    // ─── Bother — heads ───────────────────────────────────────────────────────

    @Test
    void bother_shouldAddNoSupporterToDefender_onHeads() {
        when(coinFlipService.flip()).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext("bother", true);

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState()
                .getOpponentState(PLAYER_1).getActivePokemon();
        assertThat(defender.getActiveEffects()).contains(PokemonEffect.NO_SUPPORTER);
    }

    @Test
    void bother_shouldNotDuplicateNoSupporter_whenAlreadyPresent() {
        when(coinFlipService.flip()).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext("bother", true);
        ctx.getBoardState().getOpponentState(PLAYER_1).getActivePokemon()
                .setActiveEffects(new ArrayList<>(List.of(PokemonEffect.NO_SUPPORTER)));

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getOpponentState(PLAYER_1)
                .getActivePokemon().getActiveEffects()).hasSize(1);
    }

    @Test
    void bother_shouldNotAffectAttacker_onHeads() {
        when(coinFlipService.flip()).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext("bother", true);

        effect.apply(ctx);

        ActivePokemon attacker = ctx.getBoardState()
                .getStateFor(PLAYER_1).getActivePokemon();
        assertThat(attacker.getActiveEffects()).doesNotContain(PokemonEffect.NO_SUPPORTER);
    }

    // ─── Bother — tails ───────────────────────────────────────────────────────

    @Test
    void bother_shouldNotAddNoSupporter_onTails() {
        when(coinFlipService.flip()).thenReturn(CoinResult.TAILS);
        AttackContext ctx = buildContext("bother", true);

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState()
                .getOpponentState(PLAYER_1).getActivePokemon();
        assertThat(defender.getActiveEffects()).doesNotContain(PokemonEffect.NO_SUPPORTER);
    }

    @Test
    void bother_shouldNotAddModifiers() {
        when(coinFlipService.flip()).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext("bother", true);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    // ─── Knock Back ───────────────────────────────────────────────────────────

    @Test
    void knockBack_shouldSetForcedSwitchForOpponent_whenOpponentHasBench() {
        AttackContext ctx = buildContext("knock back", true);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getPendingForcedSwitchPlayerId())
                .isEqualTo(PLAYER_2);
    }

    @Test
    void knockBack_shouldNotSetForcedSwitch_whenOpponentHasNoBench() {
        AttackContext ctx = buildContext("knock back", false);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getPendingForcedSwitchPlayerId()).isNull();
    }

    @Test
    void knockBack_shouldNotFlipCoin() {
        AttackContext ctx = buildContext("knock back", true);

        effect.apply(ctx);

        verifyNoInteractions(coinFlipService);
    }

    @Test
    void knockBack_shouldNotAddModifiers() {
        AttackContext ctx = buildContext("knock back", true);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    // ─── unknown attack ───────────────────────────────────────────────────────

    @Test
    void unknownAttack_shouldDoNothing() {
        AttackContext ctx = buildContext("unknown", true);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
        verifyNoInteractions(coinFlipService);
        assertThat(ctx.getBoardState().getPendingForcedSwitchPlayerId()).isNull();
    }

    // ─── helper ───────────────────────────────────────────────────────────────

    private AttackContext buildContext(String attackName, boolean opponentHasBench) {
        ActivePokemon krookodile = ActivePokemon.builder()
                .instanceId("krookodile-1")
                .cardId("xy1-71")
                .types(new ArrayList<>(List.of(EnergyType.DARKNESS)))
                .attachedEnergyIds(new ArrayList<>(
                        List.of("xy1-97", "xy1-97", "xy1-95", "xy1-95")))
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
        attackerState.setActivePokemon(krookodile);

        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(defender);

        if (opponentHasBench) {
            defenderState.setBench(new ArrayList<>(List.of(
                    BenchPokemon.builder()
                            .instanceId("bench-opp-1")
                            .cardId("xy1-2")
                            .attachedEnergyIds(new ArrayList<>())
                            .evolutionStack(new ArrayList<>())
                            .damageCounters(0)
                            .build())));
        } else {
            defenderState.setBench(new ArrayList<>());
        }

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