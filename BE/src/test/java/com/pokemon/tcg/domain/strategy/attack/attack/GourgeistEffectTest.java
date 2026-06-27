package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.engine.CardLookupPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GourgeistEffectTest {

    private CardLookupPort cardLookupPort;
    private GourgeistEffect effect;

    private static final int GOURGEIST_MAX_HP = 100;
    private static final int DEFENDER_MAX_HP  = 120;

    @BeforeEach
    void setUp() {
        cardLookupPort = mock(CardLookupPort.class);
        when(cardLookupPort.getMaxHp("xy1-57")).thenReturn(GOURGEIST_MAX_HP);
        effect = new GourgeistEffect(cardLookupPort);
    }

    // ─── getSupportedAttacks ──────────────────────────────────────────────────

    @Test
    void shouldSupportBothAttacks() {
        assertThat(effect.getSupportedAttacks())
                .contains("gourgeist|eerie voice", "gourgeist|spirit scream");
    }

    // ─── Eerie Voice ──────────────────────────────────────────────────────────

    @Test
    void eerieVoice_shouldAddTwoCountersToOpponentActive() {
        AttackContext ctx = buildContext("eerie voice", 0, 0, 0);

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState()
                .getOpponentState(PLAYER_1).getActivePokemon();
        assertThat(defender.getDamageCounters()).isEqualTo(2);
    }

    @Test
    void eerieVoice_shouldAddTwoCountersToEachBenchPokemon() {
        AttackContext ctx = buildContext("eerie voice", 0, 0, 0);
        List<BenchPokemon> bench = ctx.getBoardState()
                .getOpponentState(PLAYER_1).getBench();

        effect.apply(ctx);

        assertThat(bench).allSatisfy(bp ->
                assertThat(bp.getDamageCounters()).isEqualTo(2));
    }

    @Test
    void eerieVoice_shouldNotAffectAttacker() {
        AttackContext ctx = buildContext("eerie voice", 0, 0, 0);

        effect.apply(ctx);

        ActivePokemon attacker = ctx.getBoardState()
                .getStateFor(PLAYER_1).getActivePokemon();
        assertThat(attacker.getDamageCounters()).isEqualTo(0);
    }

    @Test
    void eerieVoice_shouldStackOnExistingCounters() {
        AttackContext ctx = buildContext("eerie voice", 0, 3, 0);

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState()
                .getOpponentState(PLAYER_1).getActivePokemon();
        assertThat(defender.getDamageCounters()).isEqualTo(5);
    }

    @Test
    void eerieVoice_shouldNotAddModifiers() {
        AttackContext ctx = buildContext("eerie voice", 0, 0, 0);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    // ─── Spirit Scream ────────────────────────────────────────────────────────

    @Test
    void spiritScream_shouldReduceDefenderToTenHp() {
        // Defender has 120 HP and 0 counters → needs 11 counters to reach 10 HP
        AttackContext ctx = buildContext("spirit scream", 0, 0, DEFENDER_MAX_HP);

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState()
                .getOpponentState(PLAYER_1).getActivePokemon();
        int remainingHp = DEFENDER_MAX_HP - defender.getDamageCounters() * 10;
        assertThat(remainingHp).isEqualTo(10);
    }

    @Test
    void spiritScream_shouldReduceAttackerToTenHp() {
        // Gourgeist has 100 HP and 0 counters → needs 9 counters to reach 10 HP
        AttackContext ctx = buildContext("spirit scream", 0, 0, DEFENDER_MAX_HP);

        effect.apply(ctx);

        ActivePokemon attacker = ctx.getBoardState()
                .getStateFor(PLAYER_1).getActivePokemon();
        int remainingHp = GOURGEIST_MAX_HP - attacker.getDamageCounters() * 10;
        assertThat(remainingHp).isEqualTo(10);
    }

    @Test
    void spiritScream_shouldNotAddMoreCountersIfAlreadyAtTenHp() {
        // Gourgeist already has 9 counters (90 damage) → 10 HP remaining
        AttackContext ctx = buildContext("spirit scream", 9, 0, DEFENDER_MAX_HP);

        effect.apply(ctx);

        ActivePokemon attacker = ctx.getBoardState()
                .getStateFor(PLAYER_1).getActivePokemon();
        assertThat(attacker.getDamageCounters()).isEqualTo(9);
    }

    @Test
    void spiritScream_shouldNotAddModifiers() {
        AttackContext ctx = buildContext("spirit scream", 0, 0, DEFENDER_MAX_HP);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    // ─── unknown attack ───────────────────────────────────────────────────────

    @Test
    void unknownAttack_shouldDoNothing() {
        AttackContext ctx = buildContext("unknown", 0, 0, DEFENDER_MAX_HP);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
        assertThat(ctx.getBoardState().getOpponentState(PLAYER_1)
                .getActivePokemon().getDamageCounters()).isEqualTo(0);
    }

    // ─── helper ───────────────────────────────────────────────────────────────

    private AttackContext buildContext(String attackName,
                                       int attackerCounters,
                                       int defenderCounters,
                                       int defenderMaxHp) {
        ActivePokemon gourgeist = ActivePokemon.builder()
                .instanceId("gourgeist-1")
                .cardId("xy1-57")
                .types(new ArrayList<>(List.of(EnergyType.PSYCHIC)))
                .attachedEnergyIds(new ArrayList<>(List.of("xy1-95", "xy1-95")))
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(attackerCounters)
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
                .damageCounters(defenderCounters)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();

        BenchPokemon benchPokemon = BenchPokemon.builder()
                .instanceId("bench-1")
                .cardId("xy1-2")
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>())
                .damageCounters(0)
                .build();

        PlayerState attackerState = playerState(PLAYER_1, List.of(), cardIds(5));
        attackerState.setActivePokemon(gourgeist);

        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(defender);
        defenderState.setBench(new ArrayList<>(List.of(benchPokemon)));

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
                .defenderMaxHp(defenderMaxHp)
                .modifiers(new ArrayList<>())
                .events(new ArrayList<>())
                .build();
    }
}