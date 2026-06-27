package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;

class LunatoneEffectTest {

    private LunatoneEffect effect;

    @BeforeEach
    void setUp() {
        effect = new LunatoneEffect();
    }

    @Test
    void shouldSupportBothAttacks() {
        assertThat(effect.getSupportedAttacks())
                .contains("lunatone|double draw", "lunatone|moonblast");
    }

    // ─── Double Draw ──────────────────────────────────────────────────────────

    @Test
    void doubleDraw_shouldAddTwoCardsToHand() {
        AttackContext ctx = buildContext("double draw", 5);
        int handBefore = ctx.getBoardState().getStateFor(PLAYER_1).getHand().size();

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getHand())
                .hasSize(handBefore + 2);
    }

    @Test
    void doubleDraw_shouldRemoveTwoCardsFromDeck() {
        AttackContext ctx = buildContext("double draw", 5);
        int deckBefore = ctx.getBoardState().getStateFor(PLAYER_1).getDeck().size();

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDeck())
                .hasSize(deckBefore - 2);
    }

    @Test
    void doubleDraw_shouldDrawOnlyAvailableCards_whenDeckHasOne() {
        AttackContext ctx = buildContext("double draw", 1);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDeck()).isEmpty();
        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getHand()).hasSize(1);
    }

    @Test
    void doubleDraw_shouldDoNothing_whenDeckIsEmpty() {
        AttackContext ctx = buildContext("double draw", 0);

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getHand()).isEmpty();
    }

    @Test
    void doubleDraw_shouldNotAddModifiers() {
        AttackContext ctx = buildContext("double draw", 5);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    // ─── Moonblast ────────────────────────────────────────────────────────────

    @Test
    void moonblast_shouldAddDamageReduced20ToLunatone() {
        AttackContext ctx = buildContext("moonblast", 5);

        effect.apply(ctx);

        ActivePokemon lunatone = ctx.getBoardState()
                .getStateFor(PLAYER_1).getActivePokemon();
        assertThat(lunatone.getActiveEffects()).contains(PokemonEffect.DAMAGE_REDUCED_20);
    }

    @Test
    void moonblast_shouldNotDuplicateEffect_whenAlreadyPresent() {
        AttackContext ctx = buildContext("moonblast", 5);
        ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon()
                .setActiveEffects(new ArrayList<>(List.of(PokemonEffect.DAMAGE_REDUCED_20)));

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getActiveEffects())
                .hasSize(1);
    }

    @Test
    void moonblast_shouldNotAddModifiers() {
        AttackContext ctx = buildContext("moonblast", 5);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    @Test
    void moonblast_shouldNotAffectOpponent() {
        AttackContext ctx = buildContext("moonblast", 5);

        effect.apply(ctx);

        ActivePokemon defender = ctx.getBoardState()
                .getOpponentState(PLAYER_1).getActivePokemon();
        assertThat(defender.getActiveEffects()).doesNotContain(PokemonEffect.DAMAGE_REDUCED_20);
    }

    // ─── unknown attack ───────────────────────────────────────────────────────

    @Test
    void unknownAttack_shouldDoNothing() {
        AttackContext ctx = buildContext("unknown", 5);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getActiveEffects()).isEmpty();
    }

    // ─── helper ───────────────────────────────────────────────────────────────

    private AttackContext buildContext(String attackName, int deckSize) {
        ActivePokemon lunatone = ActivePokemon.builder()
                .instanceId("lunatone-1")
                .cardId("xy1-63")
                .types(new ArrayList<>(List.of(EnergyType.FIGHTING)))
                .attachedEnergyIds(new ArrayList<>(List.of("xy1-96", "xy1-95")))
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

        List<String> deck = deckSize > 0 ? cardIds(deckSize) : new ArrayList<>();
        PlayerState attackerState = playerState(PLAYER_1, new ArrayList<>(), deck);
        attackerState.setActivePokemon(lunatone);

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