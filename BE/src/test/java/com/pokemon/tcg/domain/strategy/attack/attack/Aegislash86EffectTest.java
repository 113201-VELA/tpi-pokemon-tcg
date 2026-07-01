package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class Aegislash86EffectTest {

    private Aegislash86Effect effect;

    @BeforeEach
    void setUp() {
        effect = new Aegislash86Effect();
    }

    @Test
    void kingsShield_shouldApplyInvulnerable_toSelf() {
        AttackContext ctx = buildContext("King's Shield");

        effect.apply(ctx);

        ActivePokemon aegislash = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        assertThat(aegislash.getActiveEffects()).contains(PokemonEffect.INVULNERABLE);
    }

    @Test
    void kingsShield_shouldSetBlockedAttackName() {
        AttackContext ctx = buildContext("King's Shield");

        effect.apply(ctx);

        ActivePokemon aegislash = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        assertThat(aegislash.getBlockedAttackName()).isEqualTo("king's shield");
    }

    @Test
    void kingsShield_shouldNotDuplicateInvulnerable_whenAlreadyPresent() {
        AttackContext ctx = buildContext("King's Shield");
        ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon()
                .getActiveEffects().add(PokemonEffect.INVULNERABLE);

        effect.apply(ctx);

        List<PokemonEffect> effects = ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getActiveEffects();
        assertThat(effects).containsOnlyOnce(PokemonEffect.INVULNERABLE);
    }

    @Test
    void otherAttack_shouldDoNothing() {
        AttackContext ctx = buildContext("Some Other Attack");

        effect.apply(ctx);

        ActivePokemon aegislash = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        assertThat(aegislash.getActiveEffects()).isEmpty();
        assertThat(aegislash.getBlockedAttackName()).isNull();
    }

    private AttackContext buildContext(String attackName) {
        ActivePokemon aegislash = ActivePokemon.builder()
                .instanceId("aegislash-1")
                .cardId("xy1-86")
                .types(new ArrayList<>(List.of(EnergyType.METAL)))
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();

        PlayerState attackerState = playerState(PLAYER_1, List.of(), new ArrayList<>());
        attackerState.setActivePokemon(aegislash);

        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(ActivePokemon.builder()
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
                .build());

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
}