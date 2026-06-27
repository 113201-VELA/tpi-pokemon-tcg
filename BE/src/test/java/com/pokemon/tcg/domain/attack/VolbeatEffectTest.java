package com.pokemon.tcg.domain.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.engine.attack.AttackContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

class VolbeatEffectTest {

    private VolbeatEffect effect;

    @BeforeEach
    void setUp() {
        effect = new VolbeatEffect();
    }

    @Test
    void luringGlow_shouldSetPendingForcedSwitch_whenOpponentHasBench() {
        BenchPokemon bench = benchWithId("bench-1", "xy1-10");
        AttackContext ctx = buildContext("Luring Glow", List.of(bench));

        effect.apply(ctx);

        assertThat(ctx.getBoardState().isPendingForcedSwitch()).isTrue();
        assertThat(ctx.getBoardState().getPendingForcedSwitchPlayerId())
                .isEqualTo(PLAYER_2);
    }

    @Test
    void luringGlow_shouldDoNothing_whenOpponentHasNoBench() {
        AttackContext ctx = buildContext("Luring Glow", List.of());

        effect.apply(ctx);

        assertThat(ctx.getBoardState().isPendingForcedSwitch()).isFalse();
    }

    @Test
    void luringGlow_shouldNotAffectAttacker() {
        BenchPokemon bench = benchWithId("bench-1", "xy1-10");
        AttackContext ctx = buildContext("Luring Glow", List.of(bench));

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getConditions()).isEmpty();
    }

    @Test
    void signalBeam_shouldConfuseDefender() {
        AttackContext ctx = buildContext("Signal Beam", List.of());

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState()
                .getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getConditions()).contains(SpecialCondition.CONFUSED);
    }

    @Test
    void signalBeam_shouldReplaceAsleep_withConfused() {
        AttackContext ctx = buildContext("Signal Beam", List.of());
        ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getConditions().add(SpecialCondition.ASLEEP);

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState()
                .getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getConditions()).contains(SpecialCondition.CONFUSED);
        assertThat(defender.getConditions()).doesNotContain(SpecialCondition.ASLEEP);
    }

    @Test
    void signalBeam_shouldReplaceParalyzed_withConfused() {
        AttackContext ctx = buildContext("Signal Beam", List.of());
        ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getConditions().add(SpecialCondition.PARALYZED);

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState()
                .getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getConditions()).contains(SpecialCondition.CONFUSED);
        assertThat(defender.getConditions()).doesNotContain(SpecialCondition.PARALYZED);
    }

    @Test
    void signalBeam_shouldNotSetPendingForcedSwitch() {
        AttackContext ctx = buildContext("Signal Beam", List.of());

        effect.apply(ctx);

        assertThat(ctx.getBoardState().isPendingForcedSwitch()).isFalse();
    }

    @Test
    void signalBeam_shouldNotAffectAttacker() {
        AttackContext ctx = buildContext("Signal Beam", List.of());

        effect.apply(ctx);

        ActivePokemon attacker = ctx.getBoardState()
                .getStateFor(PLAYER_1).getActivePokemon();
        assertThat(attacker.getConditions()).isEmpty();
    }

    private AttackContext buildContext(String attackName,
                                      List<BenchPokemon> opponentBench) {
        ActivePokemon volbeat = ActivePokemon.builder()
                .instanceId("volbeat-1")
                .cardId("xy1-8")
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
        attackerState.setActivePokemon(volbeat);

        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(defender);
        defenderState.setBench(new ArrayList<>(opponentBench));

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

    private BenchPokemon benchWithId(String instanceId, String cardId) {
        return BenchPokemon.builder()
                .instanceId(instanceId)
                .cardId(cardId)
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>(List.of(cardId)))
                .damageCounters(0)
                .enteredThisTurn(false)
                .build();
    }
}
