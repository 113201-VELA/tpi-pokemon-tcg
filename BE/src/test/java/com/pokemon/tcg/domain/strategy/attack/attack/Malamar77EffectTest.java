package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

class Malamar77EffectTest {

    private final Malamar77Effect effect = new Malamar77Effect();

    private ActivePokemon buildPokemon(String instanceId) {
        return ActivePokemon.builder()
                .instanceId(instanceId)
                .cardId("xy1-77")
                .types(new ArrayList<>(List.of(EnergyType.DARKNESS)))
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>(List.of("xy1-74", "xy1-77")))
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();
    }

    private AttackContext buildContext(String attackName) {
        ActivePokemon attacker = buildPokemon("attacker-instance");
        ActivePokemon defender = buildPokemon("defender-instance");

        PlayerState attackerState = playerState(PLAYER_1, List.of(), cardIds(5));
        attackerState.setActivePokemon(attacker);

        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(defender);

        BoardState state = boardState(attackerState, defenderState);

        Map<String, Object> payload = new HashMap<>();
        if (attackName != null) {
            payload.put("attackName", attackName);
        }

        GameAction act = GameAction.builder()
                .type(GameActionType.DECLARE_ATTACK)
                .playerId(PLAYER_1)
                .payload(payload)
                .build();

        return AttackContext.builder()
                .boardState(state)
                .action(act)
                .attackName(attackName)
                .modifiers(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
    }

    @Test
    void getSupportedAttacksShouldReturnBothAttacks() {
        assertThat(effect.getSupportedAttacks())
                .containsExactlyInAnyOrder("malamar|mental panic", "malamar|puncture");
    }

    @Test
    void applyShouldDoNothingForUnsupportedAttack() {
        AttackContext ctx = buildContext("unknown attack");

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState().getOpponentState(PLAYER_1).getActivePokemon();
        assertThat(defender.isPendingAttackFailChance()).isFalse();
        assertThat(ctx.isIgnoreResistance()).isFalse();
    }

    @Test
    void applyShouldHandleNullAttackName() {
        AttackContext ctx = buildContext(null);

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState().getOpponentState(PLAYER_1).getActivePokemon();
        assertThat(defender.isPendingAttackFailChance()).isFalse();
    }

    // ---------- Mental Panic ----------

    @Test
    void mentalPanicShouldSetPendingAttackFailChanceOnDefender() {
        AttackContext ctx = buildContext("mental panic");

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState().getOpponentState(PLAYER_1).getActivePokemon();
        assertThat(defender.isPendingAttackFailChance()).isTrue();
    }

    @Test
    void mentalPanicShouldNotAffectAttacker() {
        AttackContext ctx = buildContext("mental panic");

        effect.apply(ctx);

        ActivePokemon attacker = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        assertThat(attacker.isPendingAttackFailChance()).isFalse();
    }

    @Test
    void mentalPanicShouldDoNothingWhenNoDefender() {
        AttackContext ctx = buildContext("mental panic");
        ctx.getBoardState().getOpponentState(PLAYER_1).setActivePokemon(null);

        effect.apply(ctx);

        // No exception thrown — nothing to assert beyond not crashing
        assertThat(ctx.getBoardState().getOpponentState(PLAYER_1).getActivePokemon()).isNull();
    }

    // ---------- Puncture ----------

    @Test
    void punctureShouldSetIgnoreResistance() {
        AttackContext ctx = buildContext("puncture");

        effect.apply(ctx);

        assertThat(ctx.isIgnoreResistance()).isTrue();
    }

    @Test
    void punctureShouldNotSetIgnoreDefenderEffects() {
        AttackContext ctx = buildContext("puncture");

        effect.apply(ctx);

        // Puncture ignores ONLY Resistance — Weakness and activeEffects still apply
        assertThat(ctx.isIgnoreDefenderEffects()).isFalse();
    }

    @Test
    void punctureShouldNotSetPendingAttackFailChance() {
        AttackContext ctx = buildContext("puncture");

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState().getOpponentState(PLAYER_1).getActivePokemon();
        assertThat(defender.isPendingAttackFailChance()).isFalse();
    }
}