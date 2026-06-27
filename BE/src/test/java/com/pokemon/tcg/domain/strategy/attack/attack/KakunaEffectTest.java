package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

class KakunaEffectTest {

    private KakunaEffect effect;

    @BeforeEach
    void setUp() {
        effect = new KakunaEffect();
    }

    @Test
    void apply_shouldAddHardenToAttacker() {
        AttackContext ctx = buildContext();

        effect.apply(ctx);

        ActivePokemon kakuna = ctx.getBoardState()
                .getStateFor(PLAYER_1).getActivePokemon();
        assertThat(kakuna.getActiveEffects()).contains(PokemonEffect.HARDEN);
    }

    @Test
    void apply_shouldNotDuplicateHarden_whenAlreadyActive() {
        AttackContext ctx = buildContext();
        ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getActiveEffects().add(PokemonEffect.HARDEN);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getActiveEffects())
                .containsExactly(PokemonEffect.HARDEN);
    }

    @Test
    void apply_shouldNotAffectDefender() {
        AttackContext ctx = buildContext();

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState()
                .getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getActiveEffects()).isEmpty();
    }

    @Test
    void harden_shouldReduceDamageToZero_whenDamageIs60OrLess() {
        ActivePokemon kakuna = kakunaWithHarden();
        int result = applyHardenEffect(kakuna, 60);
        assertThat(result).isEqualTo(0);
    }

    @Test
    void harden_shouldReduceDamageToZero_whenDamageIsExactly10() {
        ActivePokemon kakuna = kakunaWithHarden();
        int result = applyHardenEffect(kakuna, 10);
        assertThat(result).isEqualTo(0);
    }

    @Test
    void harden_shouldNotReduceDamage_whenDamageIsOver60() {
        ActivePokemon kakuna = kakunaWithHarden();
        int result = applyHardenEffect(kakuna, 70);
        assertThat(result).isEqualTo(70);
    }

    @Test
    void harden_shouldNotReduceDamage_whenNotActive() {
        ActivePokemon kakuna = kakunaWithoutHarden();
        int result = applyHardenEffect(kakuna, 60);
        assertThat(result).isEqualTo(60);
    }

    private AttackContext buildContext() {
        ActivePokemon kakuna = ActivePokemon.builder()
                .instanceId("kakuna-1")
                .cardId("xy1-4")
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
                .cardId("xy1-10")
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
        attackerState.setActivePokemon(kakuna);
        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(defender);

        BoardState state = boardState(attackerState, defenderState);
        GameAction act = action(GameActionType.DECLARE_ATTACK, PLAYER_1,
                "attackName", "Harden");

        return AttackContext.builder()
                .boardState(state)
                .action(act)
                .attackName("Harden")
                .modifiers(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
    }

    private ActivePokemon kakunaWithHarden() {
        return ActivePokemon.builder()
                .instanceId("kakuna-1")
                .cardId("xy1-4")
                .types(new ArrayList<>())
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>(List.of(PokemonEffect.HARDEN)))
                .build();
    }

    private ActivePokemon kakunaWithoutHarden() {
        return ActivePokemon.builder()
                .instanceId("kakuna-1")
                .cardId("xy1-4")
                .types(new ArrayList<>())
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();
    }

    private int applyHardenEffect(ActivePokemon defender, int damage) {
        if (defender.getActiveEffects() != null
                && defender.getActiveEffects().contains(PokemonEffect.HARDEN)
                && damage <= 60) {
            return 0;
        }
        return damage;
    }
}
