package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

class SimisageEffectTest {

    private SimisageEffect effect;

    @BeforeEach
    void setUp() {
        effect = new SimisageEffect();
    }

    @Test
    void apply_shouldSetBlockedAttackName_onDefender() {
        AttackContext ctx = buildContext("Tackle");

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState()
                .getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getBlockedAttackName()).isEqualTo("Tackle");
    }

    @Test
    void apply_shouldDoNothing_whenBlockedAttackNameIsNull() {
        AttackContext ctx = buildContext(null);

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState()
                .getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getBlockedAttackName()).isNull();
    }

    @Test
    void apply_shouldDoNothing_whenBlockedAttackNameIsBlank() {
        AttackContext ctx = buildContext("   ");

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState()
                .getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getBlockedAttackName()).isNull();
    }

    @Test
    void apply_shouldNotAffectAttacker() {
        AttackContext ctx = buildContext("Tackle");

        effect.apply(ctx);

        ActivePokemon attacker = ctx.getBoardState()
                .getStateFor(PLAYER_1).getActivePokemon();
        assertThat(attacker.getBlockedAttackName()).isNull();
    }

    @Test
    void apply_shouldOverwritePreviousBlockedAttack() {
        AttackContext ctx = buildContext("Tackle");
        ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().setBlockedAttackName("Scratch");

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getBlockedAttackName()).isEqualTo("Tackle");
    }

    private AttackContext buildContext(String blockedAttackName) {
        ActivePokemon simisage = ActivePokemon.builder()
                .instanceId("simisage-1")
                .cardId("xy1-11")
                .types(new ArrayList<>(List.of(EnergyType.GRASS)))
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .blockedAttackName(null)
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
                .blockedAttackName(null)
                .build();

        PlayerState attackerState = playerState(PLAYER_1, List.of(), cardIds(5));
        attackerState.setActivePokemon(simisage);
        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(defender);

        BoardState state = boardState(attackerState, defenderState);

        Map<String, Object> payload = new HashMap<>();
        payload.put("attackName", "Torment");
        if (blockedAttackName != null) {
            payload.put("blockedAttackName", blockedAttackName);
        }

        GameAction act = GameAction.builder()
                .type(GameActionType.DECLARE_ATTACK)
                .playerId(PLAYER_1)
                .payload(payload)
                .build();

        return AttackContext.builder()
                .boardState(state)
                .action(act)
                .attackName("Torment")
                .modifiers(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
    }
}
