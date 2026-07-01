package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.engine.CoinFlipService;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuilladinEffectTest {

    @Mock
    private CoinFlipService coinFlipService;

    private QuilladinEffect effect;

    @BeforeEach
    void setUp() {
        effect = new QuilladinEffect(coinFlipService);
    }

    @Test
    void scrunch_shouldApplyInvulnerable_whenHeads() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext("Scrunch", 0);

        effect.apply(ctx);

        ActivePokemon quilladin = ctx.getBoardState()
                .getStateFor(PLAYER_1).getActivePokemon();
        assertThat(quilladin.getActiveEffects()).contains(PokemonEffect.INVULNERABLE);
    }

    @Test
    void scrunch_shouldNotApplyInvulnerable_whenTails() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.TAILS);
        AttackContext ctx = buildContext("Scrunch", 0);

        effect.apply(ctx);

        ActivePokemon quilladin = ctx.getBoardState()
                .getStateFor(PLAYER_1).getActivePokemon();
        assertThat(quilladin.getActiveEffects()).doesNotContain(PokemonEffect.INVULNERABLE);
    }

    @Test
    void scrunch_shouldNotDuplicateInvulnerable_whenAlreadyActive() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext("Scrunch", 0);
        ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getActiveEffects().add(PokemonEffect.INVULNERABLE);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getActiveEffects())
                .containsExactly(PokemonEffect.INVULNERABLE);
    }

    @Test
    void scrunch_shouldNotAddDamageCounters() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext("Scrunch", 0);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getDamageCounters()).isEqualTo(0);
    }

    @Test
    void scrunch_shouldNotAffectDefender() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext("Scrunch", 0);

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState()
                .getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getActiveEffects()).isEmpty();
    }

    @Test
    void woodHammer_shouldAddOneRecoilCounter_toQuilladin() {
        AttackContext ctx = buildContext("Wood Hammer", 0);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getDamageCounters()).isEqualTo(1);
    }

    @Test
    void woodHammer_shouldAccumulateRecoilCounters_whenQuilladinAlreadyDamaged() {
        AttackContext ctx = buildContext("Wood Hammer", 3);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getDamageCounters()).isEqualTo(4);
    }

    @Test
    void woodHammer_shouldNotAffectDefender() {
        AttackContext ctx = buildContext("Wood Hammer", 0);
        ActivePokemon defender = ctx.getBoardState()
                .getStateFor(PLAYER_2).getActivePokemon();
        int initialCounters = defender.getDamageCounters();

        effect.apply(ctx);

        assertThat(defender.getDamageCounters()).isEqualTo(initialCounters);
    }

    @Test
    void woodHammer_shouldNotAddModifiers() {
        AttackContext ctx = buildContext("Wood Hammer", 0);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    private AttackContext buildContext(String attackName, int quilladinDamageCounters) {
        ActivePokemon quilladin = ActivePokemon.builder()
                .instanceId("quilladin-1")
                .cardId("xy1-13")
                .types(new ArrayList<>(List.of(EnergyType.GRASS)))
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(quilladinDamageCounters)
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
                .damageCounters(2)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();

        PlayerState attackerState = playerState(PLAYER_1, List.of(), cardIds(5));
        attackerState.setActivePokemon(quilladin);
        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(defender);

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
