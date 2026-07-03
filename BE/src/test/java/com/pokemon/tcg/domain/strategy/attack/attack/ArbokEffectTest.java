package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.engine.StatusEffectManager;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class ArbokEffectTest {

    private StatusEffectManager statusEffectManager;
    private ArbokEffect effect;

    @BeforeEach
    void setUp() {
        statusEffectManager = mock(StatusEffectManager.class);
        effect = new ArbokEffect(statusEffectManager);
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
                .contains("arbok|gastro acid", "arbok|poison jab");
    }

    // ─── Gastro Acid ──────────────────────────────────────────────────────────

    @Test
    void gastroAcid_shouldAddNoAbilities_toDefender() {
        AttackContext ctx = buildContext("gastro acid");

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState().getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getActiveEffects()).contains(PokemonEffect.NO_ABILITIES);
    }

    @Test
    void gastroAcid_shouldNotDuplicateNoAbilities_whenAlreadyPresent() {
        AttackContext ctx = buildContext("gastro acid");
        ctx.getBoardState().getStateFor(PLAYER_2).getActivePokemon()
                .setActiveEffects(new ArrayList<>(List.of(PokemonEffect.NO_ABILITIES)));

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState().getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getActiveEffects()).hasSize(1);
    }

    @Test
    void gastroAcid_shouldNotAffectAttacker() {
        AttackContext ctx = buildContext("gastro acid");

        effect.apply(ctx);

        ActivePokemon attacker = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        assertThat(attacker.getActiveEffects()).doesNotContain(PokemonEffect.NO_ABILITIES);
    }

    @Test
    void gastroAcid_shouldNotAddModifiers() {
        AttackContext ctx = buildContext("gastro acid");

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    // ─── Poison Jab ───────────────────────────────────────────────────────────

    @Test
    void poisonJab_shouldPoisonDefender() {
        AttackContext ctx = buildContext("poison jab");

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getConditions())
                .contains(SpecialCondition.POISONED);
    }

    @Test
    void poisonJab_shouldNotAffectAttacker() {
        AttackContext ctx = buildContext("poison jab");

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getConditions()).isEmpty();
    }

    @Test
    void poisonJab_shouldNotAddModifiers() {
        AttackContext ctx = buildContext("poison jab");

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    // ─── unknown attack ───────────────────────────────────────────────────────

    @Test
    void unknownAttack_shouldDoNothing() {
        AttackContext ctx = buildContext("unknown");

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
        assertThat(ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getConditions()).isEmpty();
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private AttackContext buildContext(String attackName) {
        ActivePokemon arbok = ActivePokemon.builder()
                .instanceId("arbok-1")
                .cardId("xy1-48")
                .types(new ArrayList<>(List.of(EnergyType.PSYCHIC)))
                .attachedEnergyIds(new ArrayList<>(
                        List.of("xy1-95", "xy1-95", "xy1-95")))
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
        attackerState.setActivePokemon(arbok);
        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(defender);

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