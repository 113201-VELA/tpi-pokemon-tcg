package com.pokemon.tcg.domain.attack;

import com.pokemon.tcg.domain.model.card.Card;
import com.pokemon.tcg.domain.model.card.CardType;
import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.engine.CardLookupPort;
import com.pokemon.tcg.engine.attack.AttackContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimisearEffectTest {

    @Mock
    private CardLookupPort cardLookupPort;

    private SimisearEffect effect;

    private static final String FIRE_ENERGY_ID  = "xy1-133";
    private static final String WATER_ENERGY_ID = "xy1-134";

    @BeforeEach
    void setUp() {
        effect = new SimisearEffect(cardLookupPort);
    }

    @Test
    void yawn_shouldApplyAsleep_toDefender() {
        AttackContext ctx = buildContext("Yawn", List.of(), null);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getConditions())
                .contains(SpecialCondition.ASLEEP);
    }

    @Test
    void yawn_shouldReplaceConfused_withAsleep() {
        AttackContext ctx = buildContext("Yawn", List.of(), null);
        ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getConditions().add(SpecialCondition.CONFUSED);

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState()
                .getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getConditions()).contains(SpecialCondition.ASLEEP);
        assertThat(defender.getConditions()).doesNotContain(SpecialCondition.CONFUSED);
    }

    @Test
    void yawn_shouldReplaceParalyzed_withAsleep() {
        AttackContext ctx = buildContext("Yawn", List.of(), null);
        ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getConditions().add(SpecialCondition.PARALYZED);

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState()
                .getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getConditions()).contains(SpecialCondition.ASLEEP);
        assertThat(defender.getConditions()).doesNotContain(SpecialCondition.PARALYZED);
    }

    @Test
    void yawn_shouldNotAffectAttacker() {
        AttackContext ctx = buildContext("Yawn", List.of(), null);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getConditions()).isEmpty();
    }

    @Test
    void flamethrower_shouldDiscardFireEnergy_whenSpecifiedAndAttached() {
        when(cardLookupPort.findCardById(FIRE_ENERGY_ID))
                .thenReturn(Optional.of(fireEnergyCard(FIRE_ENERGY_ID)));
        AttackContext ctx = buildContext("Flamethrower",
                List.of(FIRE_ENERGY_ID, WATER_ENERGY_ID), FIRE_ENERGY_ID);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getAttachedEnergyIds())
                .containsExactly(WATER_ENERGY_ID);
        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDiscard())
                .containsExactly(FIRE_ENERGY_ID);
    }

    @Test
    void flamethrower_shouldDoNothing_whenEnergyToDiscardIsNull() {
        AttackContext ctx = buildContext("Flamethrower",
                List.of(FIRE_ENERGY_ID), null);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getAttachedEnergyIds())
                .containsExactly(FIRE_ENERGY_ID);
        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDiscard()).isEmpty();
    }

    @Test
    void flamethrower_shouldDoNothing_whenEnergyNotAttached() {
        AttackContext ctx = buildContext("Flamethrower",
                List.of(WATER_ENERGY_ID), FIRE_ENERGY_ID);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getAttachedEnergyIds())
                .containsExactly(WATER_ENERGY_ID);
        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDiscard()).isEmpty();
    }

    @Test
    void flamethrower_shouldDoNothing_whenSpecifiedEnergyIsNotFireType() {
        when(cardLookupPort.findCardById(WATER_ENERGY_ID))
                .thenReturn(Optional.of(waterEnergyCard(WATER_ENERGY_ID)));
        AttackContext ctx = buildContext("Flamethrower",
                List.of(FIRE_ENERGY_ID, WATER_ENERGY_ID), WATER_ENERGY_ID);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getAttachedEnergyIds())
                .containsExactlyInAnyOrder(FIRE_ENERGY_ID, WATER_ENERGY_ID);
        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDiscard()).isEmpty();
    }

    @Test
    void flamethrower_shouldNotAddModifiers() {
        when(cardLookupPort.findCardById(FIRE_ENERGY_ID))
                .thenReturn(Optional.of(fireEnergyCard(FIRE_ENERGY_ID)));
        AttackContext ctx = buildContext("Flamethrower",
                List.of(FIRE_ENERGY_ID), FIRE_ENERGY_ID);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    private AttackContext buildContext(String attackName,
                                      List<String> attachedEnergies,
                                      String energyToDiscardId) {
        ActivePokemon simisear = ActivePokemon.builder()
                .instanceId("simisear-1")
                .cardId("xy1-23")
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
        attackerState.setActivePokemon(simisear);
        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(defender);

        BoardState state = boardState(attackerState, defenderState);

        Map<String, Object> payload = new HashMap<>();
        payload.put("attackName", attackName);
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
                .attackName(attackName)
                .modifiers(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
    }

    private Card fireEnergyCard(String id) {
        return Card.builder()
                .id(id)
                .supertype(CardType.ENERGY)
                .basicEnergy(true)
                .types(new ArrayList<>(List.of(EnergyType.FIRE.name())))
                .build();
    }

    private Card waterEnergyCard(String id) {
        return Card.builder()
                .id(id)
                .supertype(CardType.ENERGY)
                .basicEnergy(true)
                .types(new ArrayList<>(List.of(EnergyType.WATER.name())))
                .build();
    }
}
