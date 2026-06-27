package com.pokemon.tcg.domain.strategy.trainer.stadium;

import com.pokemon.tcg.domain.model.card.Card;
import com.pokemon.tcg.domain.model.card.CardType;
import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.engine.CardLookupPort;
import com.pokemon.tcg.engine.RuleValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static com.pokemon.tcg.fixtures.TestDataBuilder.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FairyGardenEffectTest {

    @Mock
    private CardLookupPort cardLookupPort;

    private FairyGardenEffect effect;
    private RuleValidator validator;

    private static final String FAIRY_POKEMON_CARD = "xy1-60";
    private static final String FAIRY_ENERGY_CARD  = "xy1-130";
    private static final String FIRE_ENERGY_CARD   = "xy1-133";
    private static final String BENCH_INSTANCE     = "bench-inst-1";
    private static final String ACTIVE_INSTANCE    = "active-inst-1";

    @BeforeEach
    void setUp() {
        effect    = new FairyGardenEffect();
        validator = new RuleValidator(cardLookupPort);
    }

    // ── FairyGardenEffect — apply ──────────────────────────────────────────────

    @Test
    void apply_shouldSetActiveStadiumCardId() {
        PlayerState ps = playerState(PLAYER_1, List.of(), cardIds(5));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-117");

        effect.apply(state, act);

        assertThat(state.getActiveStadiumCardId()).isEqualTo("xy1-117");
    }

    @Test
    void apply_shouldDiscardPreviousStadium_whenOneWasActive() {
        PlayerState ps = playerState(PLAYER_1, List.of(), cardIds(5));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        state.setActiveStadiumCardId("xy1-126");
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-117");

        effect.apply(state, act);

        assertThat(state.getActiveStadiumCardId()).isEqualTo("xy1-117");
        assertThat(ps.getDiscard()).contains("xy1-126");
    }

    @Test
    void apply_shouldMarkStadiumPlayedThisTurn() {
        PlayerState ps = playerState(PLAYER_1, List.of(), cardIds(5));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-117");

        effect.apply(state, act);

        assertThat(state.getTurnFlags().isStadiumPlayedThisTurn()).isTrue();
    }

    @Test
    void apply_shouldEmitTrainerPlayedEvent() {
        PlayerState ps = playerState(PLAYER_1, List.of(), cardIds(5));
        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        GameAction act = action(GameActionType.PLAY_TRAINER, PLAYER_1, "cardId", "xy1-117");

        EngineResult result = effect.apply(state, act);

        assertThat(result.events()).hasSize(1);
        assertThat(result.events().get(0).getType()).isEqualTo(GameEventType.TRAINER_PLAYED);
        assertThat(result.events().get(0).getData()).containsEntry("cardId", "xy1-117");
    }

    // ── RuleValidator — Fairy Garden retreat cost suppression ──────────────────

    @Test
    void validateRetreat_shouldSucceedWithNoEnergies_whenFairyGardenActiveAndConditionsMet() {
        when(cardLookupPort.findCardById(FAIRY_ENERGY_CARD))
                .thenReturn(Optional.of(fairyBasicEnergyCard(FAIRY_ENERGY_CARD)));

        BoardState state = fairyGardenState(
                fairyActiveWithEnergy(List.of(FAIRY_ENERGY_CARD)),
                List.of());
        GameAction act = retreatAction(List.of());

        assertThat(validator.validate(state, act).isValid()).isTrue();
    }

    @Test
    void validateRetreat_shouldRequireNormalCost_whenFairyGardenActiveButNoFairyEnergy() {
        when(cardLookupPort.findCardById(FAIRY_POKEMON_CARD))
                .thenReturn(Optional.of(pokemonCard(FAIRY_POKEMON_CARD, 1)));
        when(cardLookupPort.findCardById(FIRE_ENERGY_CARD))
                .thenReturn(Optional.of(nonFairyEnergyCard(FIRE_ENERGY_CARD)));

        BoardState state = fairyGardenState(
                fairyActiveWithEnergy(List.of(FIRE_ENERGY_CARD)),
                List.of());
        GameAction act = retreatAction(List.of());

        ValidationResult result = validator.validate(state, act);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("You must discard exactly 1");
    }

    @Test
    void validateRetreat_shouldRequireNormalCost_whenFairyGardenActiveButNotFairyPokemon() {
        when(cardLookupPort.findCardById(FAIRY_POKEMON_CARD))
                .thenReturn(Optional.of(pokemonCard(FAIRY_POKEMON_CARD, 1)));

        ActivePokemon active = nonFairyActiveWithEnergy(List.of(FAIRY_ENERGY_CARD));
        BoardState state = fairyGardenState(active, List.of());
        GameAction act = retreatAction(List.of());

        ValidationResult result = validator.validate(state, act);

        assertThat(result.isValid()).isFalse();
    }

    @Test
    void validateRetreat_shouldRequireNormalCost_whenFairyGardenNotActive() {
        when(cardLookupPort.findCardById(FAIRY_POKEMON_CARD))
                .thenReturn(Optional.of(pokemonCard(FAIRY_POKEMON_CARD, 1)));

        BoardState state = noStadiumState(
                fairyActiveWithEnergy(List.of(FAIRY_ENERGY_CARD)),
                List.of());
        GameAction act = retreatAction(List.of());

        ValidationResult result = validator.validate(state, act);

        assertThat(result.isValid()).isFalse();
    }

    // ── Test helpers ───────────────────────────────────────────────────────────

    private BoardState fairyGardenState(ActivePokemon active, List<String> attachedEnergies) {
        BenchPokemon benched = BenchPokemon.builder()
                .instanceId(BENCH_INSTANCE)
                .cardId("xy1-6")
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>(List.of("xy1-6")))
                .damageCounters(0)
                .enteredThisTurn(false)
                .build();

        PlayerState ps = playerState(PLAYER_1, List.of(), cardIds(5));
        ps.setActivePokemon(active);
        ps.setBench(new ArrayList<>(List.of(benched)));

        BoardState state = boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
        state.setActiveStadiumCardId("xy1-117");
        return state;
    }

    private BoardState noStadiumState(ActivePokemon active, List<String> attachedEnergies) {
        BenchPokemon benched = BenchPokemon.builder()
                .instanceId(BENCH_INSTANCE)
                .cardId("xy1-6")
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>(List.of("xy1-6")))
                .damageCounters(0)
                .enteredThisTurn(false)
                .build();

        PlayerState ps = playerState(PLAYER_1, List.of(), cardIds(5));
        ps.setActivePokemon(active);
        ps.setBench(new ArrayList<>(List.of(benched)));

        return boardState(ps, playerState(PLAYER_2, List.of(), cardIds(5)));
    }

    private ActivePokemon fairyActiveWithEnergy(List<String> energyIds) {
        return ActivePokemon.builder()
                .instanceId(ACTIVE_INSTANCE)
                .cardId(FAIRY_POKEMON_CARD)
                .types(new ArrayList<>(List.of(EnergyType.FAIRY)))
                .attachedEnergyIds(new ArrayList<>(energyIds))
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .evolutionStack(new ArrayList<>(List.of(FAIRY_POKEMON_CARD)))
                .damageCounters(0)
                .conditions(new HashSet<>())
                .enteredThisTurn(false)
                .build();
    }

    private ActivePokemon nonFairyActiveWithEnergy(List<String> energyIds) {
        return ActivePokemon.builder()
                .instanceId(ACTIVE_INSTANCE)
                .cardId(FAIRY_POKEMON_CARD)
                .types(new ArrayList<>(List.of(EnergyType.FIRE)))
                .attachedEnergyIds(new ArrayList<>(energyIds))
                .weaknesses(new ArrayList<>())
                .resistances(new ArrayList<>())
                .evolutionStack(new ArrayList<>(List.of(FAIRY_POKEMON_CARD)))
                .damageCounters(0)
                .conditions(new HashSet<>())
                .enteredThisTurn(false)
                .build();
    }

    private GameAction retreatAction(List<String> energyCardIdsToDiscard) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("replacementInstanceId", BENCH_INSTANCE);
        payload.put("energyCardIdsToDiscard", energyCardIdsToDiscard);
        return GameAction.builder()
                .type(GameActionType.RETREAT)
                .playerId(PLAYER_1)
                .payload(payload)
                .build();
    }

    private Card fairyBasicEnergyCard(String id) {
        return Card.builder()
                .id(id)
                .supertype(CardType.ENERGY)
                .basicEnergy(true)
                .types(new ArrayList<>(List.of(EnergyType.FAIRY.name())))
                .build();
    }

    private Card nonFairyEnergyCard(String id) {
        return Card.builder()
                .id(id)
                .supertype(CardType.ENERGY)
                .basicEnergy(true)
                .types(new ArrayList<>(List.of(EnergyType.FIRE.name())))
                .build();
    }

    private Card pokemonCard(String id, int retreatCost) {
        List<String> cost = new ArrayList<>();
        for (int i = 0; i < retreatCost; i++) cost.add("COLORLESS");
        return Card.builder()
                .id(id)
                .supertype(CardType.POKEMON)
                .retreatCost(cost)
                .build();
    }
}
