package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

class PansearEffectTest {

    private PansearEffect effect;

    private static final String FIRE_ENERGY_1 = "xy1-133";
    private static final String FIRE_ENERGY_2 = "xy1-133b";

    @BeforeEach
    void setUp() {
        effect = new PansearEffect();
    }

    @Test
    void apply_shouldDiscardSpecifiedEnergy_fromPansear() {
        AttackContext ctx = buildContext(
                List.of(FIRE_ENERGY_1, FIRE_ENERGY_2), FIRE_ENERGY_1);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getAttachedEnergyIds())
                .containsExactly(FIRE_ENERGY_2);
    }

    @Test
    void apply_shouldAddDiscardedEnergy_toOwnerDiscard() {
        AttackContext ctx = buildContext(
                List.of(FIRE_ENERGY_1, FIRE_ENERGY_2), FIRE_ENERGY_1);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDiscard())
                .containsExactly(FIRE_ENERGY_1);
    }

    @Test
    void apply_shouldDoNothing_whenEnergyToDiscardIdIsNull() {
        AttackContext ctx = buildContext(List.of(FIRE_ENERGY_1), null);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getAttachedEnergyIds())
                .containsExactly(FIRE_ENERGY_1);
        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDiscard()).isEmpty();
    }

    @Test
    void apply_shouldDoNothing_whenSpecifiedEnergyNotAttached() {
        AttackContext ctx = buildContext(List.of(FIRE_ENERGY_1), FIRE_ENERGY_2);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getAttachedEnergyIds())
                .containsExactly(FIRE_ENERGY_1);
        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDiscard()).isEmpty();
    }

    @Test
    void apply_shouldOnlyDiscardOneEnergy_whenSameEnergyAttachedTwice() {
        AttackContext ctx = buildContext(
                List.of(FIRE_ENERGY_1, FIRE_ENERGY_1), FIRE_ENERGY_1);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getAttachedEnergyIds())
                .containsExactly(FIRE_ENERGY_1);
        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDiscard())
                .containsExactly(FIRE_ENERGY_1);
    }

    @Test
    void apply_shouldNotAffectDefender() {
        AttackContext ctx = buildContext(List.of(FIRE_ENERGY_1), FIRE_ENERGY_1);
        int defenderCounters = ctx.getBoardState()
                .getStateFor(PLAYER_2).getActivePokemon().getDamageCounters();

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getDamageCounters()).isEqualTo(defenderCounters);
    }

    @Test
    void apply_shouldNotAddModifiers() {
        AttackContext ctx = buildContext(List.of(FIRE_ENERGY_1), FIRE_ENERGY_1);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    private AttackContext buildContext(List<String> attachedEnergies,
                                      String energyToDiscardId) {
        ActivePokemon pansear = ActivePokemon.builder()
                .instanceId("pansear-1")
                .cardId("xy1-22")
                .types(new ArrayList<>(List.of(EnergyType.FIRE)))
                .attachedEnergyIds(new ArrayList<>(attachedEnergies))
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();

        ActivePokemon defender = ActivePokemon.builder()
                .instanceId("def-1")
                .cardId("xy1-30")
                .types(new ArrayList<>(List.of(EnergyType.WATER)))
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();

        PlayerState attackerState = playerState(PLAYER_1, List.of(), cardIds(5));
        attackerState.setActivePokemon(pansear);
        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(defender);

        BoardState state = boardState(attackerState, defenderState);

        Map<String, Object> payload = new HashMap<>();
        payload.put("attackName", "Fireworks");
        if (energyToDiscardId != null) {
            payload.put("energyToDiscardId", energyToDiscardId);
        }

        GameAction act = GameAction.builder()
                .type(GameActionType.DECLARE_ATTACK)
                .playerId(PLAYER_1)
                .payload(payload)
                .build();

        return AttackContext.builder()
                .boardState(state)
                .action(act)
                .attackName("Fireworks")
                .modifiers(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
    }
}
