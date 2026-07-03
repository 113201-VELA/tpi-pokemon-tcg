package com.pokemon.tcg.engine.attack.steps;

import com.pokemon.tcg.domain.model.card.Card;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.engine.CardLookupPort;
import com.pokemon.tcg.engine.VictoryConditionChecker;
import com.pokemon.tcg.domain.strategy.ability.PassiveAbilityEffect;
import com.pokemon.tcg.domain.strategy.ability.PassiveAbilityRegistry;
import com.pokemon.tcg.engine.attack.AttackChain;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.fixtures.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PostDamageEffectStepTest {

    private VictoryConditionChecker victoryChecker;
    private PassiveAbilityRegistry  passiveAbilityRegistry;
    private CardLookupPort          cardLookupPort;
    private PostDamageEffectStep    step;
    private AttackChain             chain;

    @BeforeEach
    void setUp() {
        victoryChecker         = mock(VictoryConditionChecker.class);
        passiveAbilityRegistry = mock(PassiveAbilityRegistry.class);
        cardLookupPort         = mock(CardLookupPort.class);
        step  = new PostDamageEffectStep(victoryChecker, passiveAbilityRegistry, cardLookupPort);
        chain = mock(AttackChain.class);

        // Default: no card found, no passive ability
        when(cardLookupPort.findCardById(any())).thenReturn(Optional.empty());
        when(passiveAbilityRegistry.findAbility(any())).thenReturn(Optional.empty());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /**
     * Builds a context where p1 attacks p2.
     *
     * @param defenderCounters  damage counters on defender's active Pokémon
     * @param defenderMaxHp     HP resolved before the pipeline (stored in ctx)
     * @param defenderBench     bench Pokémon for the defender (may be empty)
     * @param attackerPrizes    prize card IDs for the attacker
     */
    private AttackContext buildCtx(int defenderCounters,
                                   int defenderMaxHp,
                                   List<BenchPokemon> defenderBench,
                                   List<String> attackerPrizes) {
        ActivePokemon defender = ActivePokemon.builder()
                .instanceId(TestDataBuilder.instanceId())
                .cardId("xy1-2")
                .damageCounters(defenderCounters)
                .activeEffects(new ArrayList<>())
                .attachedEnergyIds(new ArrayList<>())
                .build();

        ActivePokemon attacker = ActivePokemon.builder()
                .instanceId(TestDataBuilder.instanceId())
                .cardId("xy1-1")
                .damageCounters(0)
                .activeEffects(new ArrayList<>())
                .build();

        PlayerState p1 = TestDataBuilder.playerState(TestDataBuilder.PLAYER_1,
                new ArrayList<>(), new ArrayList<>());
        p1.setActivePokemon(attacker);
        p1.setPrizes(new ArrayList<>(attackerPrizes));
        p1.setHand(new ArrayList<>());

        PlayerState p2 = TestDataBuilder.playerState(TestDataBuilder.PLAYER_2,
                new ArrayList<>(), new ArrayList<>());
        p2.setActivePokemon(defender);
        p2.setBench(new ArrayList<>(defenderBench));

        BoardState board = TestDataBuilder.boardState(p1, p2);
        GameAction action = TestDataBuilder.action(
                GameActionType.DECLARE_ATTACK, TestDataBuilder.PLAYER_1);

        return AttackContext.builder()
                .boardState(board)
                .action(action)
                .defenderMaxHp(defenderMaxHp)
                .damageToApply(defenderCounters * 10)
                .events(new ArrayList<>())
                .build();
    }

    private BenchPokemon benchPokemon() {
        return BenchPokemon.builder()
                .instanceId(TestDataBuilder.instanceId())
                .cardId("xy1-5")
                .damageCounters(0)
                .build();
    }

    // ── tests ─────────────────────────────────────────────────────────────────

    @Test
    void notKnockedOut_shouldCallChainWithoutKOHandling() {
        // 5 counters = 50 damage, max HP = 100 → not KO
        AttackContext ctx = buildCtx(5, 100, new ArrayList<>(), List.of("xy1-99"));

        step.execute(ctx, chain);

        verify(chain).next(ctx);
        // Defender still alive
        assertThat(ctx.getBoardState().getStateFor(TestDataBuilder.PLAYER_2)
                .getActivePokemon()).isNotNull();
        assertThat(ctx.getEvents()).noneMatch(e -> e.getType() == GameEventType.POKEMON_KNOCKED_OUT);
    }

    @Test
    void knockedOut_shouldClearDefenderActivePokemon() {
        // 10 counters = 100 damage, max HP = 100 → KO
        AttackContext ctx = buildCtx(10, 100, new ArrayList<>(), List.of("xy1-99"));

        step.execute(ctx, chain);

        assertThat(ctx.getBoardState().getStateFor(TestDataBuilder.PLAYER_2)
                .getActivePokemon()).isNull();
    }

    /**
     * UPDATED: prizes are no longer transferred automatically on KO — only a
     * pending prize-take flag is set (pendingPrizeTakePlayerId/Count). The
     * actual move from prizes to hand happens later, in
     * TurnManager.handleTakePrize, once the player sends TAKE_PRIZE choosing
     * which card(s) to take. See the comment in
     * PostDamageEffectStep.handleKnockout.
     */
    @Test
    void knockedOut_shouldSetPendingPrizeTake_withoutMovingPrizesYet() {
        AttackContext ctx = buildCtx(10, 100, new ArrayList<>(), List.of("xy1-99", "xy1-98"));

        step.execute(ctx, chain);

        assertThat(ctx.getBoardState().getPendingPrizeTakePlayerId())
                .isEqualTo(TestDataBuilder.PLAYER_1);
        assertThat(ctx.getBoardState().getPendingPrizeTakeCount()).isEqualTo(1);

        PlayerState attacker = ctx.getBoardState().getStateFor(TestDataBuilder.PLAYER_1);
        assertThat(attacker.getHand()).doesNotContain("xy1-99", "xy1-98");
        assertThat(attacker.getPrizes()).hasSize(2); // untouched
    }

    /**
     * UPDATED: only POKEMON_KNOCKED_OUT is emitted here. PRIZE_TAKEN is
     * emitted later by TurnManager.handleTakePrize once the player actually
     * picks their prize card(s) — not as part of the KO step itself.
     */
    @Test
    void knockedOut_shouldEmitKOEvent_notPrizeTakenYet() {
        AttackContext ctx = buildCtx(10, 100, new ArrayList<>(), List.of("xy1-99"));

        step.execute(ctx, chain);

        assertThat(ctx.getEvents())
                .anyMatch(e -> e.getType() == GameEventType.POKEMON_KNOCKED_OUT);
        assertThat(ctx.getEvents())
                .noneMatch(e -> e.getType() == GameEventType.PRIZE_TAKEN);
    }

    @Test
    void knockedOut_withBench_shouldSetPendingBenchChoice() {
        AttackContext ctx = buildCtx(10, 100,
                List.of(benchPokemon()), List.of("xy1-99"));

        step.execute(ctx, chain);

        assertThat(ctx.getBoardState().getPendingBenchChoicePlayerId())
                .isEqualTo(TestDataBuilder.PLAYER_2);
    }

    @Test
    void knockedOut_withoutBench_shouldNotSetPendingBenchChoice() {
        AttackContext ctx = buildCtx(10, 100, new ArrayList<>(), List.of("xy1-99"));

        step.execute(ctx, chain);

        assertThat(ctx.getBoardState().getPendingBenchChoicePlayerId()).isNull();
    }

    @Test
    void knockedOut_shouldMoveKOdCardToDefenderDiscard() {
        AttackContext ctx = buildCtx(10, 100, new ArrayList<>(), List.of("xy1-99"));

        step.execute(ctx, chain);

        PlayerState defender = ctx.getBoardState().getStateFor(TestDataBuilder.PLAYER_2);
        assertThat(defender.getDiscard()).contains("xy1-2"); // defender card ID
    }

    @Test
    void knockedOut_withAttachedEnergy_shouldMoveEnergyToDiscard() {
        // Add attached energy to defender before building ctx
        ActivePokemon defenderWithEnergy = ActivePokemon.builder()
                .instanceId(TestDataBuilder.instanceId())
                .cardId("xy1-2")
                .damageCounters(10)
                .activeEffects(new ArrayList<>())
                .attachedEnergyIds(new ArrayList<>(List.of("xy1-132", "xy1-133")))
                .build();

        PlayerState p1 = TestDataBuilder.playerState(TestDataBuilder.PLAYER_1,
                new ArrayList<>(), new ArrayList<>());
        p1.setActivePokemon(ActivePokemon.builder().instanceId(TestDataBuilder.instanceId())
                .cardId("xy1-1").damageCounters(0).activeEffects(new ArrayList<>()).build());
        p1.setPrizes(new ArrayList<>(List.of("xy1-99")));
        p1.setHand(new ArrayList<>());

        PlayerState p2 = TestDataBuilder.playerState(TestDataBuilder.PLAYER_2,
                new ArrayList<>(), new ArrayList<>());
        p2.setActivePokemon(defenderWithEnergy);
        p2.setBench(new ArrayList<>());

        BoardState board = TestDataBuilder.boardState(p1, p2);
        GameAction action = TestDataBuilder.action(
                GameActionType.DECLARE_ATTACK, TestDataBuilder.PLAYER_1);
        AttackContext ctx = AttackContext.builder()
                .boardState(board).action(action)
                .defenderMaxHp(100).damageToApply(100)
                .events(new ArrayList<>()).build();

        step.execute(ctx, chain);

        PlayerState defender = ctx.getBoardState().getStateFor(TestDataBuilder.PLAYER_2);
        assertThat(defender.getDiscard()).contains("xy1-132", "xy1-133");
    }

    @Test
    void invulnerableDefender_shouldSkipKOCheckEntirely() {
        ActivePokemon defender = ActivePokemon.builder()
                .instanceId(TestDataBuilder.instanceId())
                .cardId("xy1-2")
                .damageCounters(10)
                .activeEffects(new ArrayList<>(List.of(PokemonEffect.INVULNERABLE)))
                .attachedEnergyIds(new ArrayList<>())
                .build();

        PlayerState p1 = TestDataBuilder.playerState(TestDataBuilder.PLAYER_1,
                new ArrayList<>(), new ArrayList<>());
        p1.setActivePokemon(ActivePokemon.builder().instanceId(TestDataBuilder.instanceId())
                .cardId("xy1-1").damageCounters(0).activeEffects(new ArrayList<>()).build());
        p1.setPrizes(new ArrayList<>(List.of("xy1-99")));
        p1.setHand(new ArrayList<>());

        PlayerState p2 = TestDataBuilder.playerState(TestDataBuilder.PLAYER_2,
                new ArrayList<>(), new ArrayList<>());
        p2.setActivePokemon(defender);
        p2.setBench(new ArrayList<>());

        BoardState board = TestDataBuilder.boardState(p1, p2);
        GameAction action = TestDataBuilder.action(
                GameActionType.DECLARE_ATTACK, TestDataBuilder.PLAYER_1);
        AttackContext ctx = AttackContext.builder()
                .boardState(board).action(action)
                .defenderMaxHp(100).damageToApply(100)
                .events(new ArrayList<>()).build();

        step.execute(ctx, chain);

        // Defender should still be in play
        assertThat(ctx.getBoardState().getStateFor(TestDataBuilder.PLAYER_2)
                .getActivePokemon()).isNotNull();
        assertThat(ctx.getEvents())
                .noneMatch(e -> e.getType() == GameEventType.POKEMON_KNOCKED_OUT);
    }

    @Test
    void unresolvableMaxHp_shouldSkipKOCheck() {
        // defenderMaxHp = 0 means card not found in cache
        AttackContext ctx = buildCtx(10, 0, new ArrayList<>(), List.of("xy1-99"));

        step.execute(ctx, chain);

        assertThat(ctx.getBoardState().getStateFor(TestDataBuilder.PLAYER_2)
                .getActivePokemon()).isNotNull();
    }

    @Test
    void knockedOut_shouldTriggerPassiveAbility() {
        Card card = Card.builder().id("xy1-2").name("Chesnaught").build();
        when(cardLookupPort.findCardById("xy1-2")).thenReturn(Optional.of(card));
        PassiveAbilityEffect ability = mock(PassiveAbilityEffect.class);
        when(passiveAbilityRegistry.findAbility("Chesnaught")).thenReturn(Optional.of(ability));

        AttackContext ctx = buildCtx(10, 100, new ArrayList<>(), List.of("xy1-99"));

        step.execute(ctx, chain);

        verify(ability).onDamageReceived(eq(ctx), any(ActivePokemon.class));
    }

    @Test
    void zeroDamageToApply_shouldSkipPostEffects() {
        // damageToApply = 0 means no damage was dealt (base damage 0 case)
        PlayerState p1 = TestDataBuilder.playerState(TestDataBuilder.PLAYER_1,
                new ArrayList<>(), new ArrayList<>());
        p1.setActivePokemon(ActivePokemon.builder().instanceId(TestDataBuilder.instanceId())
                .cardId("xy1-1").damageCounters(0).activeEffects(new ArrayList<>()).build());
        p1.setPrizes(new ArrayList<>(List.of("xy1-99")));
        p1.setHand(new ArrayList<>());

        ActivePokemon defender = ActivePokemon.builder()
                .instanceId(TestDataBuilder.instanceId())
                .cardId("xy1-2").damageCounters(0)
                .activeEffects(new ArrayList<>()).attachedEnergyIds(new ArrayList<>())
                .build();
        PlayerState p2 = TestDataBuilder.playerState(TestDataBuilder.PLAYER_2,
                new ArrayList<>(), new ArrayList<>());
        p2.setActivePokemon(defender);
        p2.setBench(new ArrayList<>());

        BoardState board = TestDataBuilder.boardState(p1, p2);
        GameAction action = TestDataBuilder.action(
                GameActionType.DECLARE_ATTACK, TestDataBuilder.PLAYER_1);
        AttackContext ctx = AttackContext.builder()
                .boardState(board).action(action)
                .defenderMaxHp(100).damageToApply(0)
                .events(new ArrayList<>()).build();

        step.execute(ctx, chain);

        // No KO events since damageToApply == 0
        assertThat(ctx.getEvents())
                .noneMatch(e -> e.getType() == GameEventType.POKEMON_KNOCKED_OUT);
        verify(chain).next(ctx);
    }
}