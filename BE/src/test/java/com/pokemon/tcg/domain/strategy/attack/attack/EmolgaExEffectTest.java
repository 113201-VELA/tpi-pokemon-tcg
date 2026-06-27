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

class EmolgaExEffectTest {

    private CardLookupPort cardLookupPort;
    private EmolgaExEffect effect;

    private static final String LIGHTNING_ENERGY = "xy1-135";
    private static final String WATER_ENERGY     = "xy1-131";

    @BeforeEach
    void setUp() {
        cardLookupPort = mock(CardLookupPort.class);
        effect = new EmolgaExEffect(cardLookupPort);

        Card lightningCard = mock(Card.class);
        when(lightningCard.getTypes()).thenReturn(List.of(EnergyType.LIGHTNING.name()));
        when(cardLookupPort.findCardById(LIGHTNING_ENERGY))
                .thenReturn(Optional.of(lightningCard));

        Card waterCard = mock(Card.class);
        when(waterCard.getTypes()).thenReturn(List.of(EnergyType.WATER.name()));
        when(cardLookupPort.findCardById(WATER_ENERGY))
                .thenReturn(Optional.of(waterCard));
    }

    @Test
    void shouldSupportBothAttacks() {
        assertThat(effect.getSupportedAttacks())
                .contains("emolga-ex|energy glide", "emolga-ex|electron crush");
    }

    // ─── Energy Glide ─────────────────────────────────────────────────────────

    @Test
    void energyGlide_shouldAttachLightningEnergy_fromDeck() {
        AttackContext ctx = buildContext("energy glide",
                List.of(LIGHTNING_ENERGY, WATER_ENERGY), // deck
                List.of(LIGHTNING_ENERGY),               // emolga energies
                true, null, null);

        effect.apply(ctx);

        PlayerState attacker = ctx.getBoardState().getStateFor(PLAYER_1);
        // Energy attached to whoever is now active (could be bench if switched)
        int totalEnergiesInPlay = countAllEnergies(attacker);
        assertThat(totalEnergiesInPlay).isGreaterThan(1); // had 1, now has 2
        assertThat(attacker.getDeck()).doesNotContain(LIGHTNING_ENERGY);
    }

    @Test
    void energyGlide_shouldSwitchWithBench_afterAttachingEnergy() {
        AttackContext ctx = buildContext("energy glide",
                List.of(LIGHTNING_ENERGY),
                List.of(LIGHTNING_ENERGY),
                true, "bench-1", "bench-1");

        effect.apply(ctx);

        PlayerState attacker = ctx.getBoardState().getStateFor(PLAYER_1);
        // The bench Pokémon should now be active
        assertThat(attacker.getActivePokemon().getInstanceId()).isEqualTo("bench-1");
        // Emolga should be on bench
        assertThat(attacker.getBench().stream()
                .anyMatch(b -> b.getCardId().equals("xy1-46"))).isTrue();
    }

    @Test
    void energyGlide_shouldFallBackToFirstBench_whenNoReplacementSpecified() {
        AttackContext ctx = buildContext("energy glide",
                List.of(LIGHTNING_ENERGY),
                List.of(LIGHTNING_ENERGY),
                true, null, null); // no replacement specified

        effect.apply(ctx);

        PlayerState attacker = ctx.getBoardState().getStateFor(PLAYER_1);
        assertThat(attacker.getActivePokemon().getInstanceId()).isEqualTo("bench-1");
    }

    @Test
    void energyGlide_shouldNotSwitch_whenNoBench() {
        AttackContext ctx = buildContext("energy glide",
                List.of(LIGHTNING_ENERGY),
                List.of(LIGHTNING_ENERGY),
                false, null, null); // no bench

        effect.apply(ctx);

        PlayerState attacker = ctx.getBoardState().getStateFor(PLAYER_1);
        assertThat(attacker.getActivePokemon().getCardId()).isEqualTo("xy1-46");
    }

    @Test
    void energyGlide_shouldDoNothing_whenNoLightningEnergyInDeck() {
        AttackContext ctx = buildContext("energy glide",
                List.of(WATER_ENERGY), // only water
                List.of(LIGHTNING_ENERGY),
                true, null, null);

        int energiesBefore = ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getAttachedEnergyIds().size();

        effect.apply(ctx);

        int energiesAfter = ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getAttachedEnergyIds().size();
        assertThat(energiesAfter).isEqualTo(energiesBefore);
    }

    // ─── Electron Crush ───────────────────────────────────────────────────────

    @Test
    void electronCrush_shouldAdd30Damage_whenEnergyDiscarded() {
        AttackContext ctx = buildContext("electron crush",
                List.of(),
                List.of(LIGHTNING_ENERGY, LIGHTNING_ENERGY, WATER_ENERGY),
                false, null, LIGHTNING_ENERGY);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).hasSize(1);
        assertThat(ctx.getModifiers().get(0).amount()).isEqualTo(30);
        assertThat(ctx.getBoardState().getStateFor(PLAYER_1)
                .getActivePokemon().getAttachedEnergyIds())
                .hasSize(2); // one discarded
        assertThat(ctx.getBoardState().getStateFor(PLAYER_1).getDiscard())
                .contains(LIGHTNING_ENERGY);
    }

    @Test
    void electronCrush_shouldNotAddBonus_whenNoEnergySpecified() {
        AttackContext ctx = buildContext("electron crush",
                List.of(),
                List.of(LIGHTNING_ENERGY),
                false, null, null); // no energyToDiscardId

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    @Test
    void electronCrush_shouldNotAddBonus_whenSpecifiedEnergyNotAttached() {
        AttackContext ctx = buildContext("electron crush",
                List.of(),
                List.of(LIGHTNING_ENERGY),
                false, null, WATER_ENERGY); // water not attached

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    @Test
    void electronCrush_shouldNotAffectOpponent() {
        AttackContext ctx = buildContext("electron crush",
                List.of(),
                List.of(LIGHTNING_ENERGY, LIGHTNING_ENERGY, LIGHTNING_ENERGY),
                false, null, LIGHTNING_ENERGY);
        int defenderCounters = ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getDamageCounters();

        effect.apply(ctx);

        assertThat(ctx.getBoardState().getStateFor(PLAYER_2)
                .getActivePokemon().getDamageCounters()).isEqualTo(defenderCounters);
    }

    // ─── unknown attack ───────────────────────────────────────────────────────

    @Test
    void unknownAttack_shouldDoNothing() {
        AttackContext ctx = buildContext("unknown", List.of(), List.of(), false, null, null);

        effect.apply(ctx);

        assertThat(ctx.getModifiers()).isEmpty();
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private int countAllEnergies(PlayerState ps) {
        int count = 0;
        if (ps.getActivePokemon() != null
                && ps.getActivePokemon().getAttachedEnergyIds() != null) {
            count += ps.getActivePokemon().getAttachedEnergyIds().size();
        }
        if (ps.getBench() != null) {
            count += ps.getBench().stream()
                    .mapToInt(b -> b.getAttachedEnergyIds() != null
                            ? b.getAttachedEnergyIds().size() : 0)
                    .sum();
        }
        return count;
    }

    private AttackContext buildContext(String attackName,
                                       List<String> deck,
                                       List<String> emolgaEnergies,
                                       boolean hasBench,
                                       String benchInstanceId,
                                       String energyToDiscardId) {
        ActivePokemon emolga = ActivePokemon.builder()
                .instanceId("emolga-ex-1")
                .cardId("xy1-46")
                .types(new ArrayList<>(List.of(EnergyType.LIGHTNING)))
                .attachedEnergyIds(new ArrayList<>(emolgaEnergies))
                .evolutionStack(new ArrayList<>())
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .damageCounters(0)
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .build();

        PlayerState attackerState = playerState(PLAYER_1, List.of(), cardIds(5));
        attackerState.setActivePokemon(emolga);
        attackerState.setDeck(new ArrayList<>(deck));
        attackerState.setDiscard(new ArrayList<>());

        if (hasBench) {
            String instanceId = benchInstanceId != null ? benchInstanceId : "bench-1";
            BenchPokemon bench = BenchPokemon.builder()
                    .instanceId(instanceId)
                    .cardId("xy1-2")
                    .attachedEnergyIds(new ArrayList<>())
                    .evolutionStack(new ArrayList<>())
                    .damageCounters(0)
                    .build();
            attackerState.setBench(new ArrayList<>(List.of(bench)));
        }

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

        PlayerState defenderState = playerState(PLAYER_2, List.of(), cardIds(5));
        defenderState.setActivePokemon(defender);

        BoardState state = boardState(attackerState, defenderState);

        Map<String, Object> payload = new HashMap<>();
        payload.put("attackName", attackName);
        if (benchInstanceId != null) payload.put("replacementInstanceId", benchInstanceId);
        if (energyToDiscardId != null) payload.put("energyToDiscardId", energyToDiscardId);

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