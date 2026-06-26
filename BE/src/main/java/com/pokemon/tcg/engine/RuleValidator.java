package com.pokemon.tcg.engine;

import com.pokemon.tcg.domain.model.card.CardType;
import com.pokemon.tcg.domain.model.game.*;
import org.springframework.stereotype.Component;

/**
 * Validates the legality of every game action before the engine executes it.
 * Each validation method checks only the rules relevant to its action type.
 *
 * <p>All validations follow the same contract:
 * return {@link ValidationResult#ok()} if the action is legal,
 * or {@link ValidationResult#fail(String)} with a descriptive message otherwise.
 *
 * <p>Rules enforced here are independent of card effects — they cover
 * structural game rules (turn order, phase restrictions, once-per-turn limits,
 * evolution timing, special conditions blocking actions).
 */
@Component
public class RuleValidator {

    private final CardLookupPort cardLookupPort;

    public RuleValidator(CardLookupPort cardLookupPort) {
        this.cardLookupPort = cardLookupPort;
    }

    public ValidationResult validate(BoardState state, GameAction action) {

        // During bonusDrawPending, only ACCEPT_MULLIGAN_BONUS is allowed
        // and any player with pending bonus can act regardless of currentPlayerId
        if (state.isBonusDrawPending()) {
            if (action.getType() != GameActionType.ACCEPT_MULLIGAN_BONUS) {
                return ValidationResult.fail(
                        "Mulligan bonus draws must be resolved before continuing.");
            }
            return validateAcceptMulliganBonus(state, action);
        }

        // During SETUP both players can act simultaneously (mulligan, place active/bench)
        // Turn order is not enforced during setup phase
        boolean isSetupAction = action.getType() == GameActionType.MULLIGAN_CONFIRM
                || action.getType() == GameActionType.SETUP_PLACE_ACTIVE
                || action.getType() == GameActionType.SETUP_PLACE_BENCH;

        // Only the current player may act.
        if (!isSetupAction && !state.getCurrentPlayerId().equals(action.getPlayerId())) {
            return ValidationResult.fail("It is not your turn.");
        }

        return switch (action.getType()) {
            // ── Setup phase ──────────────────────────────────────────
            case MULLIGAN_CONFIRM      -> validateMulliganConfirm(state, action);
            case SETUP_PLACE_ACTIVE    -> validateSetupPlaceActive(state, action);
            case SETUP_PLACE_BENCH     -> validateSetupPlaceBench(state, action);
            case ACCEPT_MULLIGAN_BONUS -> validateAcceptMulliganBonus(state, action);
            // ── Draw phase ───────────────────────────────────────────
            case DRAW_CARD             -> validateDrawCard(state, action);
            // ── Main phase ───────────────────────────────────────────
            case PLACE_BASIC_POKEMON   -> validatePlaceBasicPokemon(state, action);
            case EVOLVE_POKEMON        -> validateEvolution(state, action);
            case ATTACH_ENERGY         -> validateAttachEnergy(state, action);
            case PLAY_TRAINER          -> validatePlayTrainer(state, action);
            case RETREAT               -> validateRetreat(state, action);
            case DECLARE_ATTACK        -> validateAttack(state, action);
            case END_TURN              -> validateEndTurn(state, action);
            // ── Post-KO ──────────────────────────────────────────────
            case CHOOSE_BENCH_POKEMON  -> ValidationResult.ok();
            default                    -> ValidationResult.ok();
        };
    }

    // ─── DRAW ─────────────────────────────────────────────────────────────────

    /**
     * DRAW_CARD is only legal during the DRAW phase.
     * If the deck is empty, the player loses — but that victory condition
     * is checked by VictoryConditionChecker, not here.
     */
    private ValidationResult validateDrawCard(BoardState state, GameAction action) {
        if (state.getTurnPhase() != TurnPhase.DRAW) {
            return ValidationResult.fail("You can only draw a card at the start of your turn.");
        }
        PlayerState ps = state.getStateFor(action.getPlayerId());
        if (ps.getDeck() == null || ps.getDeck().isEmpty()) {
            return ValidationResult.fail("Your deck is empty. You lose.");
        }
        return ValidationResult.ok();
    }

    // ─── SETUP ────────────────────────────────────────────────────────────────

    /**
     * During SETUP, the player must place exactly one Basic Pokémon as Active.
     * The card must be in hand and must be a Basic Pokémon.
     * Only allowed when no Active Pokémon has been placed yet.
     */
    private ValidationResult validateSetupPlaceActive(BoardState state, GameAction action) {
        if (state.getTurnPhase() != TurnPhase.SETUP) {
            return ValidationResult.fail("You can only place your Active Pokémon during setup.");
        }
        PlayerState ps = state.getStateFor(action.getPlayerId());
        if (ps.getActivePokemon() != null) {
            return ValidationResult.fail("You already have an Active Pokémon.");
        }
        String cardId = action.getPayloadString("cardId");
        return validateIsBasicPokemonInHand(ps, cardId);
    }

    /**
     * During SETUP, up to 5 Basic Pokémon may be placed on the bench.
     * The card must be in hand, must be a Basic Pokémon, and bench must not be full.
     * Only allowed after the Active Pokémon has been placed.
     */
    private ValidationResult validateSetupPlaceBench(BoardState state, GameAction action) {
        if (state.getTurnPhase() != TurnPhase.SETUP) {
            return ValidationResult.fail("You can only place Bench Pokémon during setup.");
        }
        PlayerState ps = state.getStateFor(action.getPlayerId());
        if (ps.getActivePokemon() == null) {
            return ValidationResult.fail("You must place your Active Pokémon before filling your bench.");
        }
        if (ps.getBench() != null && ps.getBench().size() >= 5) {
            return ValidationResult.fail("Your bench is full (maximum 5 Pokémon).");
        }
        String cardId = action.getPayloadString("cardId");
        return validateIsBasicPokemonInHand(ps, cardId);
    }

    /**
     * Mulligan is only valid during SETUP and only if the player
     * has no Basic Pokémon in their current hand.
     */
    private ValidationResult validateMulliganConfirm(BoardState state, GameAction action) {
        if (state.getTurnPhase() != TurnPhase.SETUP) {
            return ValidationResult.fail("Mulligan can only be declared during setup.");
        }
        PlayerState ps = state.getStateFor(action.getPlayerId());
        var card = ps.getHand() == null || ps.getHand().isEmpty();
        if (!card) {
            // Check if hand has any Basic Pokémon via card lookup
            boolean hasBasic = ps.getHand().stream().anyMatch(id -> {
                var c = cardLookupPort.findCardById(id);
                if (c.isEmpty()) return false;
                var subtypes = c.get().getSubtypes();
                return c.get().getSupertype() == com.pokemon.tcg.domain.model.card.CardType.POKEMON
                        && subtypes != null && subtypes.contains("Basic");
            });
            if (hasBasic) {
                return ValidationResult.fail(
                        "You cannot declare a mulligan when you have a Basic Pokémon in hand.");
            }
        }
        return ValidationResult.ok();
    }

    /**
     * Bonus draw acceptance is only valid during setup when bonusDrawPending is true
     * and the acting player actually has bonus draws available.
     * The requested draw count must be between 0 and the player's bonus amount.
     */
    private ValidationResult validateAcceptMulliganBonus(BoardState state, GameAction action) {
        if (!state.isBonusDrawPending()) {
            return ValidationResult.fail("There are no pending mulligan bonus draws.");
        }
        PlayerState ps = state.getStateFor(action.getPlayerId());
        if (ps.getMulliganBonusDraws() <= 0) {
            return ValidationResult.fail("You have no mulligan bonus draws available.");
        }
        Integer cardsToDraw = action.getPayloadInt("cardsToDraw");
        if (cardsToDraw == null) {
            return ValidationResult.fail(
                    "You must specify 'cardsToDraw' (0 to " + ps.getMulliganBonusDraws() + ").");
        }
        if (cardsToDraw < 0) {
            return ValidationResult.fail("Cards to draw cannot be negative.");
        }
        if (cardsToDraw > ps.getMulliganBonusDraws()) {
            return ValidationResult.fail(
                    "You can draw at most " + ps.getMulliganBonusDraws() + " bonus cards.");
        }
        return ValidationResult.ok();
    }

    // ─── MAIN PHASE ───────────────────────────────────────────────────────────

    /**
     * Basic Pokémon can be placed on the bench during the MAIN phase.
     * The card must be in hand, must be a Basic Pokémon, and bench must not be full.
     */
    private ValidationResult validatePlaceBasicPokemon(BoardState state, GameAction action) {
        if (state.getTurnPhase() != TurnPhase.MAIN) {
            return ValidationResult.fail("You can only place Pokémon during your main phase.");
        }
        PlayerState ps = state.getStateFor(action.getPlayerId());
        if (ps.getBench() != null && ps.getBench().size() >= 5) {
            return ValidationResult.fail("Your bench is full (maximum 5 Pokémon).");
        }
        String cardId = action.getPayloadString("cardId");
        return validateIsBasicPokemonInHand(ps, cardId);
    }

    /**
     * Evolution rules:
     * - Only during MAIN phase.
     * - The evolution card must be in hand.
     * - The target Pokémon must be in play (Active or Bench).
     * - Cannot evolve a Pokémon that entered play this turn.
     * - Cannot evolve on the very first turn of the game (turnNumber == 1).
     */
    private ValidationResult validateEvolution(BoardState state, GameAction action) {
        if (state.getTurnPhase() != TurnPhase.MAIN) {
            return ValidationResult.fail("You can only evolve Pokémon during your main phase.");
        }
        if (state.getTurnNumber() == 1) {
            return ValidationResult.fail("You cannot evolve Pokémon on the first turn of the game.");
        }
        PlayerState ps = state.getStateFor(action.getPlayerId());
        String cardId   = action.getPayloadString("cardId");
        String targetId = action.getPayloadString("targetInstanceId");

        if (cardId == null || !ps.getHand().contains(cardId)) {
            return ValidationResult.fail("The evolution card is not in your hand.");
        }

        // Find the target Pokémon (Active or Bench)
        boolean targetEnteredThisTurn = false;
        if (ps.getActivePokemon() != null &&
                ps.getActivePokemon().getInstanceId().equals(targetId)) {
            targetEnteredThisTurn = ps.getActivePokemon().isEnteredThisTurn();
        } else if (ps.getBench() != null) {
            var benchTarget = ps.getBench().stream()
                    .filter(b -> b.getInstanceId().equals(targetId))
                    .findFirst().orElse(null);
            if (benchTarget == null) {
                return ValidationResult.fail("Target Pokémon is not in play.");
            }
            targetEnteredThisTurn = benchTarget.isEnteredThisTurn();
        } else {
            return ValidationResult.fail("Target Pokémon is not in play.");
        }

        if (targetEnteredThisTurn) {
            return ValidationResult.fail(
                    "You cannot evolve a Pokémon the same turn it entered play.");
        }

        return ValidationResult.ok();
    }

    /**
     * Energy attachment rules:
     * - Only during MAIN phase.
     * - Only once per turn.
     * - The energy card must be in hand.
     */
    private ValidationResult validateAttachEnergy(BoardState state, GameAction action) {
        if (state.getTurnPhase() != TurnPhase.MAIN) {
            return ValidationResult.fail("You can only attach Energy during your main phase.");
        }
        if (state.getTurnFlags().isEnergyAttachedThisTurn()) {
            return ValidationResult.fail("You have already attached an Energy card this turn.");
        }
        PlayerState ps = state.getStateFor(action.getPlayerId());
        String cardId    = action.getPayloadString("cardId");
        String targetId  = action.getPayloadString("targetInstanceId");

        if (cardId == null || ps.getHand() == null || !ps.getHand().contains(cardId)) {
            return ValidationResult.fail("The Energy card is not in your hand.");
        }

        // Validate that the target Pokémon instance exists in play
        if (targetId == null) {
            return ValidationResult.fail("No target Pokémon specified.");
        }
        boolean targetExists = (ps.getActivePokemon() != null &&
                ps.getActivePokemon().getInstanceId().equals(targetId)) ||
                (ps.getBench() != null && ps.getBench().stream()
                        .anyMatch(b -> b.getInstanceId().equals(targetId)));

        if (!targetExists) {
            return ValidationResult.fail("Target Pokémon is not in play.");
        }

        return ValidationResult.ok();
    }

    /**
     * Trainer card rules:
     * - Only during MAIN phase.
     * - The card must be in hand.
     * - Supporter limit (1 per turn) is enforced here.
     * - Stadium limit (1 per turn) is enforced here.
     * Full Supporter/Stadium/Item subtype distinction requires card lookup.
     */
    private ValidationResult validatePlayTrainer(BoardState state, GameAction action) {
        if (state.getTurnPhase() != TurnPhase.MAIN) {
            return ValidationResult.fail("You can only play Trainer cards during your main phase.");
        }
        PlayerState ps = state.getStateFor(action.getPlayerId());
        String cardId  = action.getPayloadString("cardId");
        if (cardId == null || ps.getHand() == null || !ps.getHand().contains(cardId)) {
            return ValidationResult.fail("The Trainer card is not in your hand.");
        }

        // Check Supporter/Stadium once-per-turn limits via card lookup
        var card = cardLookupPort.findCardById(cardId);
        if (card.isPresent()) {
            var subtypes = card.get().getSubtypes();
            if (subtypes != null && subtypes.contains("Supporter")
                    && state.getTurnFlags().isSupporterPlayedThisTurn()) {
                return ValidationResult.fail("You can only play one Supporter per turn.");
            }
            if (subtypes != null && subtypes.contains("Stadium")
                    && state.getTurnFlags().isStadiumPlayedThisTurn()) {
                return ValidationResult.fail("You can only play one Stadium per turn.");
            }
        }

        return ValidationResult.ok();
    }

    /**
     * Retreat rules:
     * - Only during MAIN phase.
     * - Only once per turn.
     * - Active Pokémon must not be Asleep or Paralyzed.
     * - There must be at least one Pokémon on the bench to switch to.
     */
    private ValidationResult validateRetreat(BoardState state, GameAction action) {
        if (state.getTurnPhase() != TurnPhase.MAIN) {
            return ValidationResult.fail("You can only retreat during your main phase.");
        }
        if (state.getTurnFlags().isRetreatedThisTurn()) {
            return ValidationResult.fail("You have already retreated this turn.");
        }
        PlayerState ps = state.getStateFor(action.getPlayerId());
        if (ps.getBench() == null || ps.getBench().isEmpty()) {
            return ValidationResult.fail("You have no Pokémon on the bench to retreat to.");
        }
        ActivePokemon active = ps.getActivePokemon();
        if (active == null) {
            return ValidationResult.fail("You have no Active Pokémon.");
        }
        if (active.getConditions() != null) {
            if (active.getConditions().contains(SpecialCondition.ASLEEP)) {
                return ValidationResult.fail("Your Active Pokémon is Asleep and cannot retreat.");
            }
            if (active.getConditions().contains(SpecialCondition.PARALYZED)) {
                return ValidationResult.fail("Your Active Pokémon is Paralyzed and cannot retreat.");
            }
        }
        return ValidationResult.ok();
    }

    /**
     * Attack rules:
     * - Only during MAIN phase.
     * - Active Pokémon must not be Asleep or Paralyzed.
     * Energy sufficiency is checked inside EnergyCheckStep in the pipeline,
     * not here, to keep the pipeline as the single source of truth for energy logic.
     */
    private ValidationResult validateAttack(BoardState state, GameAction action) {
        if (state.getTurnPhase() != TurnPhase.MAIN) {
            return ValidationResult.fail("You can only attack during your main phase.");
        }
        PlayerState ps = state.getStateFor(action.getPlayerId());
        ActivePokemon active = ps.getActivePokemon();
        if (active == null) {
            return ValidationResult.fail("You have no Active Pokémon to attack with.");
        }
        if (active.getConditions() != null) {
            if (active.getConditions().contains(SpecialCondition.ASLEEP)) {
                return ValidationResult.fail("Your Active Pokémon is Asleep and cannot attack.");
            }
            if (active.getConditions().contains(SpecialCondition.PARALYZED)) {
                return ValidationResult.fail("Your Active Pokémon is Paralyzed and cannot attack.");
            }
        }
        return ValidationResult.ok();
    }

    /**
     * END_TURN is always legal during MAIN phase.
     */
    private ValidationResult validateEndTurn(BoardState state, GameAction action) {
        if (state.getTurnPhase() != TurnPhase.MAIN) {
            return ValidationResult.fail("You can only end your turn during the main phase.");
        }
        return ValidationResult.ok();
    }

    // ─── HELPERS ──────────────────────────────────────────────────────────────

    /**
     * Verifies that a card is in the player's hand and is a Basic Pokémon.
     * Used by setup and bench placement validations.
     */
    private ValidationResult validateIsBasicPokemonInHand(PlayerState ps, String cardId) {
        if (cardId == null || ps.getHand() == null || !ps.getHand().contains(cardId)) {
            return ValidationResult.fail("The card is not in your hand.");
        }
        var card = cardLookupPort.findCardById(cardId);
        if (card.isEmpty()) {
            return ValidationResult.fail("Card not found in the card cache.");
        }
        if (card.get().getSupertype() != CardType.POKEMON) {
            return ValidationResult.fail("You can only place Pokémon cards.");
        }
        var subtypes = card.get().getSubtypes();
        if (subtypes == null || !subtypes.contains("Basic")) {
            return ValidationResult.fail("You can only place Basic Pokémon.");
        }
        return ValidationResult.ok();
    }
}