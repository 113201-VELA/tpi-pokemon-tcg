package com.pokemon.tcg.domain.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.engine.DamageModifier;
import com.pokemon.tcg.engine.attack.AttackContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

class WeedleEffectTest {

    private WeedleEffect effect;

    @BeforeEach
    void setUp() {
        effect = new WeedleEffect();
    }

    @Test
    void apply_shouldAddGrassBonus_whenDefenderIsGrassType() {
        AttackContext ctx = contextWithDefenderType(EnergyType.GRASS);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).hasSize(1);
        DamageModifier mod = ctx.getModifiers().get(0);
        assertThat(mod.amount()).isEqualTo(20);
        assertThat(mod.beforeWeakness()).isTrue();
        assertThat(mod.source()).isEqualTo("weedle-grass-bonus");
    }

    @Test
    void apply_shouldNotAddBonus_whenDefenderIsFireType() {
        AttackContext ctx = contextWithDefenderType(EnergyType.FIRE);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    @Test
    void apply_shouldNotAddBonus_whenDefenderIsWaterType() {
        AttackContext ctx = contextWithDefenderType(EnergyType.WATER);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    @Test
    void apply_shouldNotAddBonus_whenDefenderHasNoTypes() {
        AttackContext ctx = contextWithDefenderType(null);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    @Test
    void apply_shouldNotAddBonus_whenDefenderIsNull() {
        PlayerState attackerState = playerState(PLAYER_1, List.of(), cardIds(5));
        attackerState.setActivePokemon(attackerWithType(EnergyType.COLORLESS));
        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        BoardState state = boardState(attackerState, defenderState);
        GameAction act = action(GameActionType.DECLARE_ATTACK, PLAYER_1,
                "attackName", "Poison Sting");
        AttackContext ctx = AttackContext.builder()
                .boardState(state)
                .action(act)
                .attackName("Poison Sting")
                .modifiers(new ArrayList<>())
                .events(new ArrayList<>())
                .build();

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    @Test
    void apply_shouldPreserveExistingModifiers_whenAddingGrassBonus() {
        AttackContext ctx = contextWithDefenderType(EnergyType.GRASS);
        ctx.getModifiers().add(new DamageModifier("existing", 10, true));

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).hasSize(2);
    }

    private AttackContext contextWithDefenderType(EnergyType defenderType) {
        PlayerState attackerState = playerState(PLAYER_1, List.of(), cardIds(5));
        attackerState.setActivePokemon(attackerWithType(EnergyType.COLORLESS));

        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        if (defenderType != null) {
            defenderState.setActivePokemon(defenderWithType(defenderType));
        }

        BoardState state = boardState(attackerState, defenderState);
        GameAction act = action(GameActionType.DECLARE_ATTACK, PLAYER_1,
                "attackName", "Poison Sting");

        return AttackContext.builder()
                .boardState(state)
                .action(act)
                .attackName("Poison Sting")
                .modifiers(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
    }

    private ActivePokemon attackerWithType(EnergyType type) {
        return ActivePokemon.builder()
                .instanceId("att-1")
                .cardId("xy1-3")
                .types(new ArrayList<>(List.of(type)))
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .build();
    }

    private ActivePokemon defenderWithType(EnergyType type) {
        return ActivePokemon.builder()
                .instanceId("def-1")
                .cardId("xy1-10")
                .types(new ArrayList<>(List.of(type)))
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .build();
    }
}
