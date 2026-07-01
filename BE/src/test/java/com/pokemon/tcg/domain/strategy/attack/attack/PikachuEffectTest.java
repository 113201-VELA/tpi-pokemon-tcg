package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.engine.CoinFlipService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PikachuEffectTest {

    private CoinFlipService coinFlipService;
    private PikachuEffect effect;

    @BeforeEach
    void setUp() {
        coinFlipService = mock(CoinFlipService.class);
        effect = new PikachuEffect(coinFlipService);
    }

    @Test
    void shouldSupportBothAttacks() {
        assertThat(effect.getSupportedAttacks())
                .contains("pikachu|nuzzle", "pikachu|quick attack");
    }

    // ─── Nuzzle ───────────────────────────────────────────────────────────────

    @Test
    void nuzzle_onHeads_shouldParalyzeDefender() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext("nuzzle", new HashSet<>());

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getConditions())
                .contains(SpecialCondition.PARALYZED);
    }

    @Test
    void nuzzle_onHeads_shouldReplaceAsleep() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext("nuzzle",
                new HashSet<>(Set.of(SpecialCondition.ASLEEP)));

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getConditions())
                .contains(SpecialCondition.PARALYZED)
                .doesNotContain(SpecialCondition.ASLEEP);
    }

    @Test
    void nuzzle_onTails_shouldNotParalyzeDefender() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.TAILS);
        AttackContext ctx = buildContext("nuzzle", new HashSet<>());

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getConditions())
                .doesNotContain(SpecialCondition.PARALYZED);
    }

    @Test
    void nuzzle_shouldNotAffectAttacker() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext("nuzzle", new HashSet<>());

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getConditions()).isEmpty();
    }

    // ─── Quick Attack ─────────────────────────────────────────────────────────

    @Test
    void quickAttack_onHeads_shouldAdd10Damage() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext("quick attack", new HashSet<>());

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).hasSize(1);
        assertThat(ctx.getModifiers().get(0).amount()).isEqualTo(10);
    }

    @Test
    void quickAttack_onTails_shouldNotAddDamage() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.TAILS);
        AttackContext ctx = buildContext("quick attack", new HashSet<>());

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    @Test
    void quickAttack_shouldNotAffectOpponentConditions() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.HEADS);
        AttackContext ctx = buildContext("quick attack", new HashSet<>());

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getConditions()).isEmpty();
    }

    // ─── unknown attack ───────────────────────────────────────────────────────

    @Test
    void unknownAttack_shouldDoNothing() {
        AttackContext ctx = buildContext("unknown", new HashSet<>());

        effect.apply(ctx);

        verifyNoInteractions(coinFlipService);
        assertThat(ctx.getModifiers()).isEmpty();
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private AttackContext buildContext(String attackName,
                                       Set<SpecialCondition> defenderConditions) {
        ActivePokemon pikachu = ActivePokemon.builder()
                .instanceId("pikachu-1")
                .cardId("xy1-42")
                .types(new ArrayList<>(List.of(EnergyType.LIGHTNING)))
                .attachedEnergyIds(new ArrayList<>(List.of("xy1-135", "xy1-135")))
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
                .conditions(new HashSet<>(defenderConditions))
                .activeEffects(new ArrayList<>())
                .build();

        PlayerState attackerState = playerState(PLAYER_1, List.of(), cardIds(5));
        attackerState.setActivePokemon(pikachu);
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