package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.Card;
import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.domain.strategy.attack.DamageModifier;
import com.pokemon.tcg.engine.CardLookupPort;
import com.pokemon.tcg.engine.CoinFlipService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RhyperiorEffectTest {

    private CoinFlipService coinFlipService;
    private CardLookupPort  cardLookupPort;
    private RhyperiorEffect effect;

    private static final String FIGHTING_ENERGY_ID = "xy1-96";
    private static final String OTHER_ENERGY_ID    = "xy1-95";

    @BeforeEach
    void setUp() {
        coinFlipService = mock(CoinFlipService.class);
        cardLookupPort  = mock(CardLookupPort.class);

        Card fightingEnergy = mock(Card.class);
        when(fightingEnergy.isBasicEnergy()).thenReturn(true);
        when(fightingEnergy.getTypes()).thenReturn(List.of(EnergyType.FIGHTING.name()));
        when(cardLookupPort.findCardById(FIGHTING_ENERGY_ID))
                .thenReturn(Optional.of(fightingEnergy));

        Card otherEnergy = mock(Card.class);
        when(otherEnergy.isBasicEnergy()).thenReturn(true);
        when(otherEnergy.getTypes()).thenReturn(List.of(EnergyType.COLORLESS.name()));
        when(cardLookupPort.findCardById(OTHER_ENERGY_ID))
                .thenReturn(Optional.of(otherEnergy));

        effect = new RhyperiorEffect(coinFlipService, cardLookupPort);
    }

    @Test
    void shouldSupportBothAttacks() {
        assertThat(effect.getSupportedAttacks())
                .contains("rhyperior|rock blast", "rhyperior|rock wrecker");
    }

    // ─── Rock Blast ───────────────────────────────────────────────────────────

    @Test
    void rockBlast_shouldAdd50PerHead_withTwoFightingEnergiesAndTwoHeads() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.HEADS, CoinResult.HEADS);
        AttackContext ctx = buildContext("rock blast",
                List.of(FIGHTING_ENERGY_ID, FIGHTING_ENERGY_ID));

        effect.apply(ctx);

        int total = ctx.getModifiers().stream()
                .mapToInt(DamageModifier::amount).sum();
        assertThat(total).isEqualTo(100);
    }

    @Test
    void rockBlast_shouldAdd50_withOneHeadOutOfTwo() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.HEADS, CoinResult.TAILS);
        AttackContext ctx = buildContext("rock blast",
                List.of(FIGHTING_ENERGY_ID, FIGHTING_ENERGY_ID));

        effect.apply(ctx);

        int total = ctx.getModifiers().stream()
                .mapToInt(DamageModifier::amount).sum();
        assertThat(total).isEqualTo(50);
    }

    @Test
    void rockBlast_shouldAddNoModifier_whenAllTails() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.TAILS, CoinResult.TAILS);
        AttackContext ctx = buildContext("rock blast",
                List.of(FIGHTING_ENERGY_ID, FIGHTING_ENERGY_ID));

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    @Test
    void rockBlast_shouldOnlyCountFightingEnergies() {
        when(coinFlipService.flipAndEmit(any(AttackContext.class), anyString())).thenReturn(CoinResult.HEADS);
        // One Fighting + one Colorless — only one coin flip
        AttackContext ctx = buildContext("rock blast",
                List.of(FIGHTING_ENERGY_ID, OTHER_ENERGY_ID));

        effect.apply(ctx);

        verify(coinFlipService, times(1)).flip();
        int total = ctx.getModifiers().stream()
                .mapToInt(DamageModifier::amount).sum();
        assertThat(total).isEqualTo(50);
    }

    @Test
    void rockBlast_shouldDoNothing_whenNoFightingEnergies() {
        AttackContext ctx = buildContext("rock blast",
                List.of(OTHER_ENERGY_ID, OTHER_ENERGY_ID));

        effect.apply(ctx);

        verifyNoInteractions(coinFlipService);
        assertThat(ctx.getModifiers()).isEmpty();
    }

    // ─── Rock Wrecker ─────────────────────────────────────────────────────────

    @Test
    void rockWrecker_shouldSetIgnoreDefenderEffects() {
        AttackContext ctx = buildContext("rock wrecker",
                List.of(FIGHTING_ENERGY_ID, FIGHTING_ENERGY_ID,
                        OTHER_ENERGY_ID, OTHER_ENERGY_ID));

        effect.apply(ctx);

        assertThat(ctx.isIgnoreDefenderEffects()).isTrue();
    }

    @Test
    void rockWrecker_shouldBlockRhyperiorFromAttackingNextTurn() {
        AttackContext ctx = buildContext("rock wrecker",
                List.of(FIGHTING_ENERGY_ID, FIGHTING_ENERGY_ID,
                        OTHER_ENERGY_ID, OTHER_ENERGY_ID));

        effect.apply(ctx);

        ActivePokemon rhyperior = ctx.getBoardState()
                .getStateFor(PLAYER_1).getActivePokemon();
        assertThat(rhyperior.getBlockedAttackName()).isEqualTo("rock wrecker");
    }

    @Test
    void rockWrecker_shouldNotFlipCoins() {
        AttackContext ctx = buildContext("rock wrecker",
                List.of(FIGHTING_ENERGY_ID, FIGHTING_ENERGY_ID,
                        OTHER_ENERGY_ID, OTHER_ENERGY_ID));

        effect.apply(ctx);

        verifyNoInteractions(coinFlipService);
    }

    @Test
    void rockWrecker_shouldNotAddModifiers() {
        AttackContext ctx = buildContext("rock wrecker",
                List.of(FIGHTING_ENERGY_ID, FIGHTING_ENERGY_ID,
                        OTHER_ENERGY_ID, OTHER_ENERGY_ID));

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    // ─── unknown attack ───────────────────────────────────────────────────────

    @Test
    void unknownAttack_shouldDoNothing() {
        AttackContext ctx = buildContext("unknown",
                List.of(FIGHTING_ENERGY_ID));

        effect.apply(ctx);

        verifyNoInteractions(coinFlipService);
        assertThat(ctx.getModifiers()).isEmpty();
        assertThat(ctx.isIgnoreDefenderEffects()).isFalse();
    }

    // ─── helper ───────────────────────────────────────────────────────────────

    private AttackContext buildContext(String attackName, List<String> attachedEnergies) {
        ActivePokemon rhyperior = ActivePokemon.builder()
                .instanceId("rhyperior-1")
                .cardId("xy1-62")
                .types(new ArrayList<>(List.of(EnergyType.FIGHTING)))
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
        attackerState.setActivePokemon(rhyperior);

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