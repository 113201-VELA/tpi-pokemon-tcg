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

class RhyhornEffectTest {

    private CardLookupPort cardLookupPort;
    private RhyhornEffect effect;

    private static final String FIGHTING_ENERGY_ID = "xy1-96";
    private static final String NON_ENERGY_ID      = "xy1-1";

    @BeforeEach
    void setUp() {
        cardLookupPort = mock(CardLookupPort.class);
        effect = new RhyhornEffect(cardLookupPort);

        Card fightingEnergy = mock(Card.class);
        when(fightingEnergy.isBasicEnergy()).thenReturn(true);
        when(fightingEnergy.getTypes()).thenReturn(List.of(EnergyType.FIGHTING.name()));
        when(cardLookupPort.findCardById(FIGHTING_ENERGY_ID))
                .thenReturn(Optional.of(fightingEnergy));

        Card nonEnergy = mock(Card.class);
        when(nonEnergy.isBasicEnergy()).thenReturn(false);
        when(nonEnergy.getTypes()).thenReturn(List.of());
        when(cardLookupPort.findCardById(NON_ENERGY_ID))
                .thenReturn(Optional.of(nonEnergy));
    }

    @Test
    void shouldSupportBothAttacks() {
        assertThat(effect.getSupportedAttacks())
                .contains("rhyhorn|dig out", "rhyhorn|horn drill");
    }

    // ─── Dig Out — Fighting Energy on top ────────────────────────────────────

    @Test
    void digOut_shouldAttachFightingEnergyToRhyhorn_whenTopCardIsFightingEnergy() {
        AttackContext ctx = buildContext("dig out",
                List.of(FIGHTING_ENERGY_ID, NON_ENERGY_ID));

        effect.apply(ctx);

        ActivePokemon rhyhorn = ctx.getBoardState()
                .getStateFor(PLAYER_1).getActivePokemon();
        assertThat(rhyhorn.getAttachedEnergyIds()).contains(FIGHTING_ENERGY_ID);
    }

    @Test
    void digOut_shouldRemoveTopCardFromDeck_whenTopCardIsFightingEnergy() {
        AttackContext ctx = buildContext("dig out",
                List.of(FIGHTING_ENERGY_ID, NON_ENERGY_ID));

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDeck())
                .doesNotContain(FIGHTING_ENERGY_ID);
    }

    @Test
    void digOut_shouldNotDiscardFightingEnergy_whenAttached() {
        AttackContext ctx = buildContext("dig out",
                List.of(FIGHTING_ENERGY_ID, NON_ENERGY_ID));

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDiscard())
                .doesNotContain(FIGHTING_ENERGY_ID);
    }

    // ─── Dig Out — non-energy on top ─────────────────────────────────────────

    @Test
    void digOut_shouldDiscardTopCard_whenNotFightingEnergy() {
        AttackContext ctx = buildContext("dig out",
                List.of(NON_ENERGY_ID, FIGHTING_ENERGY_ID));

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDiscard())
                .contains(NON_ENERGY_ID);
    }

    @Test
    void digOut_shouldNotAttachNonFightingEnergy() {
        AttackContext ctx = buildContext("dig out",
                List.of(NON_ENERGY_ID, FIGHTING_ENERGY_ID));

        effect.apply(ctx);

        ActivePokemon rhyhorn = ctx.getBoardState()
                .getStateFor(PLAYER_1).getActivePokemon();
        assertThat(rhyhorn.getAttachedEnergyIds()).doesNotContain(NON_ENERGY_ID);
    }

    // ─── Dig Out — empty deck ─────────────────────────────────────────────────

    @Test
    void digOut_shouldDoNothing_whenDeckIsEmpty() {
        AttackContext ctx = buildContext("dig out", List.of());

        int energiesBefore = ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getAttachedEnergyIds().size();

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getAttachedEnergyIds()).hasSize(energiesBefore);
        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDiscard()).isEmpty();
    }

    // ─── Horn Drill ───────────────────────────────────────────────────────────

    @Test
    void hornDrill_shouldNotAddModifiers() {
        AttackContext ctx = buildContext("horn drill", List.of(NON_ENERGY_ID));

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    @Test
    void hornDrill_shouldNotModifyDeck() {
        AttackContext ctx = buildContext("horn drill",
                List.of(NON_ENERGY_ID, FIGHTING_ENERGY_ID));
        int deckSizeBefore = ctx.getBoardState().getStateFor(PLAYER_1).getDeck().size();

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDeck())
                .hasSize(deckSizeBefore);
    }

    // ─── unknown attack ───────────────────────────────────────────────────────

    @Test
    void unknownAttack_shouldDoNothing() {
        AttackContext ctx = buildContext("unknown", List.of(FIGHTING_ENERGY_ID));

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
        verifyNoInteractions(cardLookupPort);
    }

    // ─── helper ───────────────────────────────────────────────────────────────

    private AttackContext buildContext(String attackName, List<String> deck) {
        ActivePokemon rhyhorn = ActivePokemon.builder()
                .instanceId("rhyhorn-1")
                .cardId("xy1-60")
                .types(new ArrayList<>(List.of(EnergyType.FIGHTING)))
                .attachedEnergyIds(new ArrayList<>(List.of(FIGHTING_ENERGY_ID)))
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

        PlayerState attackerState = playerState(PLAYER_1, List.of(), deck);
        attackerState.setActivePokemon(rhyhorn);
        attackerState.setDiscard(new ArrayList<>());

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