package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.Card;
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

class Jigglypuff87EffectTest {

    private CardLookupPort   cardLookupPort;
    private Jigglypuff87Effect effect;

    private static final String DARK_ENERGY_1 = "xy1-136";
    private static final String DARK_ENERGY_2 = "xy1-137";
    private static final String OTHER_ENERGY  = "xy1-131";

    @BeforeEach
    void setUp() {
        cardLookupPort = mock(CardLookupPort.class);
        effect = new Jigglypuff87Effect(cardLookupPort);

        Card darkCard1 = mock(Card.class);
        when(darkCard1.getTypes()).thenReturn(List.of(EnergyType.DARKNESS.name()));
        when(cardLookupPort.findCardById(DARK_ENERGY_1)).thenReturn(Optional.of(darkCard1));

        Card darkCard2 = mock(Card.class);
        when(darkCard2.getTypes()).thenReturn(List.of(EnergyType.DARKNESS.name()));
        when(cardLookupPort.findCardById(DARK_ENERGY_2)).thenReturn(Optional.of(darkCard2));

        Card otherCard = mock(Card.class);
        when(otherCard.getTypes()).thenReturn(List.of(EnergyType.WATER.name()));
        when(cardLookupPort.findCardById(OTHER_ENERGY)).thenReturn(Optional.of(otherCard));
    }

    @Test
    void shouldSupportOnlyHeartfeltSong() {
        assertThat(effect.getSupportedAttacks()).containsExactly("jigglypuff|heartfelt song");
    }

    // ─── Heartfelt Song ───────────────────────────────────────────────────────

    @Test
    void heartfeltSong_shouldDiscardDarknessEnergy() {
        AttackContext ctx = buildContext("heartfelt song",
                List.of(DARK_ENERGY_1, OTHER_ENERGY), null);

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState().getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getAttachedEnergyIds()).containsExactly(OTHER_ENERGY);
        assertThat(ctx.getBoardState().getStateFor(PLAYER_2).getDiscard()).contains(DARK_ENERGY_1);
    }

    @Test
    void heartfeltSong_shouldPreferRequestedDarknessEnergy_whenSpecified() {
        AttackContext ctx = buildContext("heartfelt song",
                List.of(DARK_ENERGY_1, DARK_ENERGY_2), DARK_ENERGY_2);

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState().getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getAttachedEnergyIds()).containsExactly(DARK_ENERGY_1);
        assertThat(ctx.getBoardState().getStateFor(PLAYER_2).getDiscard()).contains(DARK_ENERGY_2);
    }

    @Test
    void heartfeltSong_shouldFallBackToFirstDarkness_whenRequestedIsNotDarkness() {
        AttackContext ctx = buildContext("heartfelt song",
                List.of(DARK_ENERGY_1, OTHER_ENERGY), OTHER_ENERGY); // not darkness

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState().getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getAttachedEnergyIds()).containsExactly(OTHER_ENERGY);
        assertThat(ctx.getBoardState().getStateFor(PLAYER_2).getDiscard()).contains(DARK_ENERGY_1);
    }

    @Test
    void heartfeltSong_shouldDoNothing_whenNoDarknessEnergyAttached() {
        AttackContext ctx = buildContext("heartfelt song",
                List.of(OTHER_ENERGY), null);

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState().getStateFor(PLAYER_2).getActivePokemon();
        assertThat(defender.getAttachedEnergyIds()).containsExactly(OTHER_ENERGY);
        assertThat(ctx.getBoardState().getStateFor(PLAYER_2).getDiscard()).isEmpty();
    }

    @Test
    void heartfeltSong_shouldDoNothing_whenDefenderHasNoEnergy() {
        AttackContext ctx = buildContext("heartfelt song", List.of(), null);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_2).getDiscard()).isEmpty();
    }

    // ─── Rollout / unknown attack ───────────────────────────────────────────

    @Test
    void rollout_shouldDoNothing() {
        AttackContext ctx = buildContext("rollout", List.of(DARK_ENERGY_1), null);

        effect.apply(ctx);

        verifyNoInteractions(cardLookupPort);
        assertThat(ctx.getBoardState().getStateFor(PLAYER_2).getDiscard()).isEmpty();
    }

    // ─── helper ───────────────────────────────────────────────────────────────

    private AttackContext buildContext(String attackName,
                                       List<String> defenderEnergies,
                                       String requestedEnergyId) {
        ActivePokemon jigglypuff = ActivePokemon.builder()
                .instanceId("jigglypuff-1")
                .cardId("xy1-87")
                .types(new ArrayList<>(List.of(EnergyType.FAIRY)))
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
                .cardId("xy1-1")
                .types(new ArrayList<>(List.of(EnergyType.GRASS)))
                .attachedEnergyIds(new ArrayList<>(defenderEnergies))
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();

        PlayerState attackerState = playerState(PLAYER_1, List.of(), cardIds(5));
        attackerState.setActivePokemon(jigglypuff);

        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(defender);
        defenderState.setDiscard(new ArrayList<>());

        BoardState state = boardState(attackerState, defenderState);

        Map<String, Object> payload = new HashMap<>();
        payload.put("attackName", attackName);
        if (requestedEnergyId != null) payload.put("energyCardId", requestedEnergyId);

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