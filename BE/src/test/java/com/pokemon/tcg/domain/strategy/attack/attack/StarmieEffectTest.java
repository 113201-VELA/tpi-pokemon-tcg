package com.pokemon.tcg.domain.strategy.attack.attack;

import com.pokemon.tcg.domain.model.card.Card;
import com.pokemon.tcg.domain.model.card.CardType;
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

class StarmieEffectTest {

    private CardLookupPort cardLookupPort;
    private StarmieEffect effect;

    private static final String WATER_ENERGY   = "xy1-131";
    private static final String PSYCHIC_ENERGY = "xy1-95";

    @BeforeEach
    void setUp() {
        cardLookupPort = mock(CardLookupPort.class);
        effect = new StarmieEffect(cardLookupPort);

        // Water energy — not Psychic
        Card waterCard = mock(Card.class);
        when(waterCard.getTypes()).thenReturn(List.of(EnergyType.WATER.name()));
        when(cardLookupPort.findCardById(WATER_ENERGY)).thenReturn(Optional.of(waterCard));

        // Psychic energy
        Card psychicCard = mock(Card.class);
        when(psychicCard.getTypes()).thenReturn(List.of(EnergyType.PSYCHIC.name()));
        when(cardLookupPort.findCardById(PSYCHIC_ENERGY)).thenReturn(Optional.of(psychicCard));
    }

    // ─── getSupportedAttacks ──────────────────────────────────────────────────

    @Test
    void shouldSupportBothAttacks() {
        assertThat(effect.getSupportedAttacks())
                .contains("starmie|recover", "starmie|core splash");
    }

    // ─── Recover ──────────────────────────────────────────────────────────────

    @Test
    void recover_shouldHealAllDamage_andDiscardEnergy() {
        AttackContext ctx = buildContext("recover", List.of(WATER_ENERGY), WATER_ENERGY, 5);

        effect.apply(ctx);

        ActivePokemon starmie = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        assertThat(starmie.getDamageCounters()).isEqualTo(0);
        assertThat(starmie.getAttachedEnergyIds()).isEmpty();
        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDiscard())
                .contains(WATER_ENERGY);
    }

    @Test
    void recover_shouldKeepRemainingEnergies_whenMultipleAttached() {
        AttackContext ctx = buildContext("recover",
                List.of(WATER_ENERGY, PSYCHIC_ENERGY), WATER_ENERGY, 3);

        effect.apply(ctx);

        ActivePokemon starmie = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        assertThat(starmie.getDamageCounters()).isEqualTo(0);
        assertThat(starmie.getAttachedEnergyIds()).containsExactly(PSYCHIC_ENERGY);
    }

    @Test
    void recover_shouldDoNothing_whenEnergyToDiscardIdIsNull() {
        AttackContext ctx = buildContext("recover", List.of(WATER_ENERGY), null, 4);

        effect.apply(ctx);

        ActivePokemon starmie = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        assertThat(starmie.getDamageCounters()).isEqualTo(4);
        assertThat(starmie.getAttachedEnergyIds()).containsExactly(WATER_ENERGY);
    }

    @Test
    void recover_shouldDoNothing_whenSpecifiedEnergyNotAttached() {
        AttackContext ctx = buildContext("recover", List.of(WATER_ENERGY), PSYCHIC_ENERGY, 4);

        effect.apply(ctx);

        ActivePokemon starmie = ctx.getBoardState().getStateFor(PLAYER_1).getActivePokemon();
        assertThat(starmie.getDamageCounters()).isEqualTo(4);
        assertThat(starmie.getAttachedEnergyIds()).containsExactly(WATER_ENERGY);
    }

    @Test
    void recover_shouldNotAffectOpponent() {
        AttackContext ctx = buildContext("recover", List.of(WATER_ENERGY), WATER_ENERGY, 3);
        int defenderCounters = ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getDamageCounters();

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getDamageCounters()).isEqualTo(defenderCounters);
    }

    // ─── Core Splash ──────────────────────────────────────────────────────────

    @Test
    void coreSplash_shouldAdd30Damage_whenPsychicEnergyAttached() {
        AttackContext ctx = buildContext("core splash",
                List.of(WATER_ENERGY, PSYCHIC_ENERGY), null, 0);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).hasSize(1);
        assertThat(ctx.getModifiers().get(0).amount()).isEqualTo(30);
    }

    @Test
    void coreSplash_shouldNotAddBonus_whenNoPsychicEnergyAttached() {
        AttackContext ctx = buildContext("core splash",
                List.of(WATER_ENERGY), null, 0);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    @Test
    void coreSplash_shouldNotAddBonus_whenNoEnergyAttached() {
        AttackContext ctx = buildContext("core splash", List.of(), null, 0);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    @Test
    void coreSplash_shouldAdd30Damage_whenOnlyPsychicEnergyAttached() {
        AttackContext ctx = buildContext("core splash",
                List.of(PSYCHIC_ENERGY), null, 0);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).hasSize(1);
        assertThat(ctx.getModifiers().get(0).amount()).isEqualTo(30);
    }

    // ─── unknown attack ───────────────────────────────────────────────────────

    @Test
    void unknownAttack_shouldDoNothing() {
        AttackContext ctx = buildContext("unknown", List.of(WATER_ENERGY), null, 3);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getDamageCounters()).isEqualTo(3);
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private AttackContext buildContext(String attackName,
                                       List<String> attachedEnergies,
                                       String energyToDiscardId,
                                       int initialDamageCounters) {
        ActivePokemon starmie = ActivePokemon.builder()
                .instanceId("starmie-1")
                .cardId("xy1-34")
                .types(new ArrayList<>(List.of(EnergyType.WATER)))
                .attachedEnergyIds(new ArrayList<>(attachedEnergies))
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(initialDamageCounters)
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
        attackerState.setActivePokemon(starmie);
        attackerState.setDiscard(new ArrayList<>());

        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(defender);

        BoardState state = boardState(attackerState, defenderState);

        Map<String, Object> payload = new HashMap<>();
        payload.put("attackName", attackName);
        if (energyToDiscardId != null) {
            payload.put("energyToDiscardId", energyToDiscardId);
        }

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