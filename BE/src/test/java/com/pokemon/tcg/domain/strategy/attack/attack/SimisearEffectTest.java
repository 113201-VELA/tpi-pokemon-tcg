package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.Card;
import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.engine.CardLookupPort;
import com.pokemon.tcg.engine.StatusEffectManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SimisearEffectTest {

    private CardLookupPort      cardLookupPort;
    private StatusEffectManager statusEffectManager;
    private SimisearEffect      effect;

    private static final String FIRE_ENERGY_1 = "xy1-133";
    private static final String FIRE_ENERGY_2 = "xy1-138";
    private static final String OTHER_ENERGY  = "xy1-131";

    @BeforeEach
    void setUp() {
        cardLookupPort = mock(CardLookupPort.class);
        statusEffectManager = mock(StatusEffectManager.class);
        effect = new SimisearEffect(cardLookupPort, statusEffectManager);

        Card fireCard1 = mock(Card.class);
        when(fireCard1.getTypes()).thenReturn(List.of(EnergyType.FIRE.name()));
        when(cardLookupPort.findCardById(FIRE_ENERGY_1)).thenReturn(Optional.of(fireCard1));

        Card fireCard2 = mock(Card.class);
        when(fireCard2.getTypes()).thenReturn(List.of(EnergyType.FIRE.name()));
        when(cardLookupPort.findCardById(FIRE_ENERGY_2)).thenReturn(Optional.of(fireCard2));

        Card otherCard = mock(Card.class);
        when(otherCard.getTypes()).thenReturn(List.of(EnergyType.WATER.name()));
        when(cardLookupPort.findCardById(OTHER_ENERGY)).thenReturn(Optional.of(otherCard));

        doAnswer(invocation -> {
            ActivePokemon pokemon = invocation.getArgument(0);
            SpecialCondition condition = invocation.getArgument(1);
            Set<SpecialCondition> conditions = new HashSet<>(
                    pokemon.getConditions() != null ? pokemon.getConditions() : new HashSet<>());
            if (condition == SpecialCondition.ASLEEP
                    || condition == SpecialCondition.CONFUSED
                    || condition == SpecialCondition.PARALYZED) {
                conditions.remove(SpecialCondition.ASLEEP);
                conditions.remove(SpecialCondition.CONFUSED);
                conditions.remove(SpecialCondition.PARALYZED);
            }
            conditions.add(condition);
            pokemon.setConditions(conditions);
            return null;
        }).when(statusEffectManager).applyCondition(any(), any());
    }

    @Test
    void shouldSupportBothAttacks() {
        assertThat(effect.getSupportedAttacks())
                .containsExactly("simisear|yawn", "simisear|flamethrower");
    }

    // ─── Yawn ─────────────────────────────────────────────────────────────────

    @Test
    void yawn_shouldPutOpponentActiveToSleep() {
        AttackContext ctx = buildContext("yawn", List.of(), null, new HashSet<>());

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState().getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getConditions()).contains(SpecialCondition.ASLEEP);
    }

    @Test
    void yawn_shouldReplaceConfusedAndParalyzed() {
        AttackContext ctx = buildContext("yawn", List.of(), null,
                new HashSet<>(Set.of(SpecialCondition.CONFUSED)));

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState().getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getConditions())
                .contains(SpecialCondition.ASLEEP)
                .doesNotContain(SpecialCondition.CONFUSED);
    }

    // ─── Flamethrower ─────────────────────────────────────────────────────────

    @Test
    void flamethrower_shouldDiscardRequestedFireEnergy_whenSpecified() {
        AttackContext ctx = buildContext("flamethrower",
                List.of(FIRE_ENERGY_1, FIRE_ENERGY_2), FIRE_ENERGY_2, new HashSet<>());

        effect.apply(ctx);

        ActivePokemon simisear = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        assertThat(simisear.getAttachedEnergyIds()).containsExactly(FIRE_ENERGY_1);
        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDiscard()).contains(FIRE_ENERGY_2);
    }

    @Test
    void flamethrower_shouldDiscardFirstFireEnergy_whenNoneSpecified() {
        // This is the bug fix: the discard is mandatory, must not be skippable
        AttackContext ctx = buildContext("flamethrower",
                List.of(FIRE_ENERGY_1, FIRE_ENERGY_2), null, new HashSet<>());

        effect.apply(ctx);

        ActivePokemon simisear = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        assertThat(simisear.getAttachedEnergyIds()).containsExactly(FIRE_ENERGY_2);
        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDiscard()).contains(FIRE_ENERGY_1);
    }

    @Test
    void flamethrower_shouldFallBackToFirstFireEnergy_whenRequestedIsNotFire() {
        AttackContext ctx = buildContext("flamethrower",
                List.of(FIRE_ENERGY_1, OTHER_ENERGY), OTHER_ENERGY, new HashSet<>());

        effect.apply(ctx);

        ActivePokemon simisear = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        assertThat(simisear.getAttachedEnergyIds()).containsExactly(OTHER_ENERGY);
        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDiscard()).contains(FIRE_ENERGY_1);
    }

    @Test
    void flamethrower_shouldFallBackToFirstFireEnergy_whenRequestedNotAttached() {
        AttackContext ctx = buildContext("flamethrower",
                List.of(FIRE_ENERGY_1), "xy1-999", new HashSet<>());

        effect.apply(ctx);

        ActivePokemon simisear = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        assertThat(simisear.getAttachedEnergyIds()).isEmpty();
        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDiscard()).contains(FIRE_ENERGY_1);
    }

    @Test
    void flamethrower_shouldDoNothing_whenNoEnergyAttached() {
        AttackContext ctx = buildContext("flamethrower", List.of(), null, new HashSet<>());

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDiscard()).isEmpty();
    }

    @Test
    void flamethrower_shouldDoNothing_whenOnlyNonFireEnergyAttached() {
        AttackContext ctx = buildContext("flamethrower", List.of(OTHER_ENERGY), null, new HashSet<>());

        effect.apply(ctx);

        ActivePokemon simisear = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        assertThat(simisear.getAttachedEnergyIds()).containsExactly(OTHER_ENERGY);
        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDiscard()).isEmpty();
    }

    @Test
    void flamethrower_shouldNotAffectDefender() {
        AttackContext ctx = buildContext("flamethrower", List.of(FIRE_ENERGY_1), null, new HashSet<>());

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState().getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getConditions()).isEmpty();
    }

    // ─── unknown attack ───────────────────────────────────────────────────────

    @Test
    void unknownAttack_shouldDoNothing() {
        AttackContext ctx = buildContext("unknown", List.of(FIRE_ENERGY_1), null, new HashSet<>());

        effect.apply(ctx);

        ActivePokemon simisear = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        assertThat(simisear.getAttachedEnergyIds()).containsExactly(FIRE_ENERGY_1);
        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDiscard()).isEmpty();
    }

    // ─── helper ───────────────────────────────────────────────────────────────

    private AttackContext buildContext(String attackName,
                                       List<String> simisearEnergies,
                                       String requestedEnergyId,
                                       Set<SpecialCondition> defenderConditions) {
        ActivePokemon simisear = ActivePokemon.builder()
                .instanceId("simisear-1")
                .cardId("xy1-23")
                .types(new ArrayList<>(List.of(EnergyType.FIRE)))
                .attachedEnergyIds(new ArrayList<>(simisearEnergies))
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
                .conditions(defenderConditions)
                .activeEffects(new ArrayList<>())
                .build();

        PlayerState attackerState = playerState(PLAYER_1, List.of(), cardIds(5));
        attackerState.setActivePokemon(simisear);
        attackerState.setDiscard(new ArrayList<>());

        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(defender);

        BoardState state = boardState(attackerState, defenderState);

        Map<String, Object> payload = new HashMap<>();
        payload.put("attackName", attackName);
        if (requestedEnergyId != null) payload.put("energyToDiscardId", requestedEnergyId);

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
}