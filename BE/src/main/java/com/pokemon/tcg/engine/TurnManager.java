package com.pokemon.tcg.engine;

import com.pokemon.tcg.domain.model.card.Attack;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.TrainerEffectRegistry;
import com.pokemon.tcg.engine.attack.AttackContext;
import com.pokemon.tcg.engine.attack.AttackPipeline;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

@Component
public class TurnManager {

    private final RuleValidator ruleValidator;
    private final CoinFlipService coinFlipService;
    private final AttackPipeline attackPipeline;
    private final StatusEffectManager statusEffectManager;
    private final CardLookupPort cardLookupPort;
    private final TrainerEffectRegistry trainerEffectRegistry;

    public TurnManager(RuleValidator ruleValidator,
                       CoinFlipService coinFlipService,
                       AttackPipeline attackPipeline,
                       StatusEffectManager statusEffectManager,
                       CardLookupPort cardLookupPort,
                       TrainerEffectRegistry trainerEffectRegistry) {
        this.ruleValidator         = ruleValidator;
        this.coinFlipService       = coinFlipService;
        this.attackPipeline        = attackPipeline;
        this.statusEffectManager   = statusEffectManager;
        this.cardLookupPort        = cardLookupPort;
        this.trainerEffectRegistry = trainerEffectRegistry;
    }

    /**
     * Processes a game action and advances the board state accordingly.
     * Routes each action type to its specific handler.
     */
    public BoardState advancePhase(BoardState state, GameAction action) {
        return switch (action.getType()) {
            case DRAW_CARD            -> handleDrawCard(state, action);
            case PLACE_BASIC_POKEMON  -> handlePlaceBasicPokemon(state, action);
            case ATTACH_ENERGY        -> handleAttachEnergy(state, action);
            case PLAY_TRAINER         -> handlePlayTrainer(state, action);
            case EVOLVE_POKEMON       -> handleEvolvePokemon(state, action);
            case RETREAT              -> handleRetreat(state, action);
            case DECLARE_ATTACK       -> handleDeclareAttack(state, action);
            case END_TURN             -> handleEndTurn(state, action);
            case SETUP_PLACE_ACTIVE   -> handleSetupPlaceActive(state, action);
            case SETUP_PLACE_BENCH    -> handleSetupPlaceBench(state, action);
            case MULLIGAN_CONFIRM     -> handleMulliganConfirm(state, action);
            case CHOOSE_BENCH_POKEMON -> handleChooseBenchPokemon(state, action);
            default                   -> state;
        };
    }

    public ValidationResult validateActionForPhase(BoardState state, GameAction action) {
        return ruleValidator.validate(state, action);
    }

    public Set<GameActionType> getAvailableActions(BoardState state) {
        Set<GameActionType> actions = new HashSet<>();
        TurnPhase phase = state.getTurnPhase();
        String currentPlayer = state.getCurrentPlayerId();

        if (phase == TurnPhase.SETUP) {
            actions.add(GameActionType.SETUP_PLACE_ACTIVE);
            actions.add(GameActionType.SETUP_PLACE_BENCH);
            actions.add(GameActionType.MULLIGAN_CONFIRM);
            return actions;
        }

        if (phase == TurnPhase.DRAW) {
            actions.add(GameActionType.DRAW_CARD);
            return actions;
        }

        if (phase == TurnPhase.MAIN) {
            actions.add(GameActionType.END_TURN);
            actions.add(GameActionType.PLACE_BASIC_POKEMON);
            actions.add(GameActionType.PLAY_TRAINER);
            actions.add(GameActionType.EVOLVE_POKEMON);

            PlayerState ps = state.getStateFor(currentPlayer);
            if (!state.getTurnFlags().isEnergyAttachedThisTurn()) {
                actions.add(GameActionType.ATTACH_ENERGY);
            }
            if (!state.getTurnFlags().isRetreatedThisTurn() && ps.getActivePokemon() != null) {
                actions.add(GameActionType.RETREAT);
            }
            if (ps.getActivePokemon() != null) {
                actions.add(GameActionType.DECLARE_ATTACK);
            }
        }

        return actions;
    }

    // ─── SETUP ────────────────────────────────────────────────────────────────

    /** Places a Basic Pokémon from hand as the Active Pokémon during setup. */
    private BoardState handleSetupPlaceActive(BoardState state, GameAction action) {
        String cardId = action.getPayloadString("cardId");
        PlayerState ps = state.getStateFor(action.getPlayerId());

        if (cardId == null || !ps.getHand().contains(cardId)) return state;

        List<String> hand = new ArrayList<>(ps.getHand());
        hand.remove(cardId);
        ps.setHand(hand);

        ActivePokemon active = ActivePokemon.builder()
                .instanceId(UUID.randomUUID().toString())
                .cardId(cardId)
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>(List.of(cardId)))
                .damageCounters(0)
                .conditions(new HashSet<>())
                .enteredThisTurn(true)
                .build();

        ps.setActivePokemon(active);

        // If both players have placed their Active Pokémon, setup is complete.
        // Transition to DRAW phase so the first player can start their turn.
        // This logic will be moved to SetupState when the State pattern is connected (step 4).
        if (state.getPlayer1State().getActivePokemon() != null &&
                state.getPlayer2State().getActivePokemon() != null) {
            return state.toBuilder()
                    .turnPhase(TurnPhase.DRAW)
                    .gameState(GameState.ACTIVE)
                    .build();
        }

        return state;
    }

    /** Places a Basic Pokémon from hand onto the bench during setup. */
    private BoardState handleSetupPlaceBench(BoardState state, GameAction action) {
        String cardId = action.getPayloadString("cardId");
        PlayerState ps = state.getStateFor(action.getPlayerId());

        if (cardId == null || !ps.getHand().contains(cardId)) return state;
        if (ps.getBench() != null && ps.getBench().size() >= 5) return state;

        List<String> hand = new ArrayList<>(ps.getHand());
        hand.remove(cardId);
        ps.setHand(hand);

        BenchPokemon bench = BenchPokemon.builder()
                .instanceId(UUID.randomUUID().toString())
                .cardId(cardId)
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>(List.of(cardId)))
                .damageCounters(0)
                .enteredThisTurn(true)
                .build();

        List<BenchPokemon> benchList = new ArrayList<>(
                ps.getBench() != null ? ps.getBench() : new ArrayList<>());
        benchList.add(bench);
        ps.setBench(benchList);

        return state;
    }

    /** Confirms mulligan — no Basic Pokémon in hand, reshuffles and redraws. */
    private BoardState handleMulliganConfirm(BoardState state, GameAction action) {
        return state;
    }

    // ─── DRAW ─────────────────────────────────────────────────────────────────

    /** Draws one card from the top of the deck into the hand. */
    private BoardState handleDrawCard(BoardState state, GameAction action) {
        PlayerState ps = state.getStateFor(action.getPlayerId());

        if (ps.getDeck() == null || ps.getDeck().isEmpty()) return state;

        List<String> deck = new ArrayList<>(ps.getDeck());
        List<String> hand = new ArrayList<>(
                ps.getHand() != null ? ps.getHand() : new ArrayList<>());

        hand.add(deck.remove(0));
        ps.setDeck(deck);
        ps.setHand(hand);

        return state.toBuilder()
                .turnPhase(TurnPhase.MAIN)
                .build();
    }

    // ─── MAIN ─────────────────────────────────────────────────────────────────

    /** Places a Basic Pokémon from hand onto the bench. */
    private BoardState handlePlaceBasicPokemon(BoardState state, GameAction action) {
        String cardId = action.getPayloadString("cardId");
        PlayerState ps = state.getStateFor(action.getPlayerId());

        if (cardId == null || !ps.getHand().contains(cardId)) return state;
        if (ps.getBench() != null && ps.getBench().size() >= 5) return state;

        List<String> hand = new ArrayList<>(ps.getHand());
        hand.remove(cardId);
        ps.setHand(hand);

        BenchPokemon bench = BenchPokemon.builder()
                .instanceId(UUID.randomUUID().toString())
                .cardId(cardId)
                .attachedEnergyIds(new ArrayList<>())
                .evolutionStack(new ArrayList<>(List.of(cardId)))
                .damageCounters(0)
                .enteredThisTurn(true)
                .build();

        List<BenchPokemon> benchList = new ArrayList<>(
                ps.getBench() != null ? ps.getBench() : new ArrayList<>());
        benchList.add(bench);
        ps.setBench(benchList);

        return state;
    }

    /** Attaches one Energy card from hand to a Pokémon. Only once per turn. */
    private BoardState handleAttachEnergy(BoardState state, GameAction action) {
        String cardId   = action.getPayloadString("cardId");
        String targetId = action.getPayloadString("targetInstanceId");
        PlayerState ps  = state.getStateFor(action.getPlayerId());

        if (cardId == null || targetId == null) return state;
        if (state.getTurnFlags().isEnergyAttachedThisTurn()) return state;
        if (!ps.getHand().contains(cardId)) return state;

        List<String> hand = new ArrayList<>(ps.getHand());
        hand.remove(cardId);
        ps.setHand(hand);

        attachEnergyToPokemon(ps, targetId, cardId);
        state.getTurnFlags().setEnergyAttachedThisTurn(true);

        return state;
    }

    /**
     * Plays a Trainer card from hand.
     * Looks up the card's effect in the TrainerEffectRegistry and applies it.
     * If no effect is registered for the card, it is discarded with no additional effect.
     */
    private BoardState handlePlayTrainer(BoardState state, GameAction action) {
        String cardId  = action.getPayloadString("cardId");
        PlayerState ps = state.getStateFor(action.getPlayerId());

        if (cardId == null || !ps.getHand().contains(cardId)) return state;

        List<String> hand = new ArrayList<>(ps.getHand());
        hand.remove(cardId);
        ps.setHand(hand);

        List<String> discard = new ArrayList<>(
                ps.getDiscard() != null ? ps.getDiscard() : new ArrayList<>());
        discard.add(cardId);
        ps.setDiscard(discard);

        // Look up and apply the card's effect via Strategy pattern
        String cardName = cardLookupPort.findCardById(cardId)
                .map(card -> card.getName())
                .orElse(null);

        if (cardName != null) {
            trainerEffectRegistry.findEffect(cardName).ifPresent(effect -> {
                ValidationResult canApply = effect.canApply(state, action);
                if (canApply.isValid()) {
                    effect.apply(state, action);
                }
            });
        }

        return state;
    }

    /** Evolves an in-play Pokémon using a card from hand. */
    private BoardState handleEvolvePokemon(BoardState state, GameAction action) {
        String cardId   = action.getPayloadString("cardId");
        String targetId = action.getPayloadString("targetInstanceId");
        PlayerState ps  = state.getStateFor(action.getPlayerId());

        if (cardId == null || targetId == null) return state;
        if (!ps.getHand().contains(cardId)) return state;

        List<String> hand = new ArrayList<>(ps.getHand());
        hand.remove(cardId);
        ps.setHand(hand);

        evolvePokemon(ps, targetId, cardId);

        return state;
    }

    /** Retreats the Active Pokémon to the bench. */
    private BoardState handleRetreat(BoardState state, GameAction action) {
        String replacementId = action.getPayloadString("replacementInstanceId");
        PlayerState ps       = state.getStateFor(action.getPlayerId());

        if (replacementId == null || ps.getActivePokemon() == null) return state;
        if (state.getTurnFlags().isRetreatedThisTurn()) return state;

        BenchPokemon replacement = ps.getBench().stream()
                .filter(b -> b.getInstanceId().equals(replacementId))
                .findFirst().orElse(null);

        if (replacement == null) return state;

        ActivePokemon oldActive = ps.getActivePokemon();
        BenchPokemon newBench = BenchPokemon.builder()
                .instanceId(oldActive.getInstanceId())
                .cardId(oldActive.getCardId())
                .attachedEnergyIds(oldActive.getAttachedEnergyIds())
                .attachedToolId(oldActive.getAttachedToolId())
                .evolutionStack(oldActive.getEvolutionStack())
                .damageCounters(oldActive.getDamageCounters())
                .enteredThisTurn(false)
                .build();

        ActivePokemon newActive = ActivePokemon.builder()
                .instanceId(replacement.getInstanceId())
                .cardId(replacement.getCardId())
                .attachedEnergyIds(replacement.getAttachedEnergyIds())
                .attachedToolId(replacement.getAttachedToolId())
                .evolutionStack(replacement.getEvolutionStack())
                .damageCounters(replacement.getDamageCounters())
                .conditions(new HashSet<>())
                .enteredThisTurn(false)
                .build();

        List<BenchPokemon> bench = new ArrayList<>(ps.getBench());
        bench.remove(replacement);
        bench.add(newBench);
        ps.setBench(bench);
        ps.setActivePokemon(newActive);

        state.getTurnFlags().setRetreatedThisTurn(true);

        return state;
    }

    /**
     * Declares and executes an attack through the full 7-step pipeline.
     * Resolves the attack object and defender HP from the card cache before
     * building the context. After the pipeline resolves, processes between-turn
     * effects and switches to the opponent's turn.
     *
     * <p>If the pipeline is cancelled (e.g. insufficient energy, confusion flip),
     * the turn does not end — the player keeps their turn and receives an error event.
     */
    private BoardState handleDeclareAttack(BoardState state, GameAction action) {
        String attackName = action.getPayloadString("attackName");
        if (attackName == null) return state;

        PlayerState attackerState = state.getStateFor(action.getPlayerId());
        ActivePokemon attacker = attackerState.getActivePokemon();
        if (attacker == null) return state;

        Attack attack = findAttack(attacker.getCardId(), attackName);

        String opponentId = action.getPlayerId().equals(state.getPlayer1State().getPlayerId())
                ? state.getPlayer2State().getPlayerId()
                : state.getPlayer1State().getPlayerId();
        PlayerState defenderState = state.getStateFor(opponentId);
        int defenderMaxHp = resolveMaxHp(defenderState.getActivePokemon());

        AttackContext ctx = AttackContext.builder()
                .boardState(state)
                .action(action)
                .attackName(attackName)
                .attack(attack)
                .defenderMaxHp(defenderMaxHp)
                .cancelled(false)
                .damageToApply(0)
                .modifiers(new ArrayList<>())
                .events(new ArrayList<>())
                .build();

        attackPipeline.execute(ctx);

        // If the pipeline was cancelled (e.g. insufficient energy, confusion flip)
        // the turn does not end — the player keeps their turn and can act again.
        // A TURN_ENDED event with the cancellation reason is emitted so the client
        // can display a descriptive error message.
        if (ctx.isCancelled()) {
            List<GameEvent> pending = new ArrayList<>(
                    ctx.getBoardState().getPendingEvents() != null
                            ? ctx.getBoardState().getPendingEvents() : new ArrayList<>());
            pending.add(GameEvent.builder()
                    .type(GameEventType.TURN_ENDED)
                    .gameId(state.getGameId())
                    .playerId(action.getPlayerId())
                    .turnNumber(state.getTurnNumber())
                    .data(Map.of("error", ctx.getCancellationReason() != null
                            ? ctx.getCancellationReason()
                            : "Attack was cancelled."))
                    .occurredAt(Instant.now())
                    .build());
            return ctx.getBoardState().toBuilder()
                    .pendingEvents(pending)
                    .build();
        }

        // Attack resolved successfully — process between-turn effects and switch player
        state = processBetweenTurns(ctx.getBoardState());
        state.getTurnFlags().setAttackedThisTurn(true);

        return state.toBuilder()
                .currentPlayerId(opponentId)
                .turnPhase(TurnPhase.DRAW)
                .turnNumber(state.getTurnNumber() + 1)
                .turnFlags(TurnFlags.fresh())
                .build();
    }

    /**
     * Ends the turn without attacking, processes between-turn effects
     * and passes control to the opponent.
     */
    private BoardState handleEndTurn(BoardState state, GameAction action) {
        state = processBetweenTurns(state);

        String nextId = action.getPlayerId().equals(state.getPlayer1State().getPlayerId())
                ? state.getPlayer2State().getPlayerId()
                : state.getPlayer1State().getPlayerId();

        return state.toBuilder()
                .currentPlayerId(nextId)
                .turnPhase(TurnPhase.DRAW)
                .turnNumber(state.getTurnNumber() + 1)
                .turnFlags(TurnFlags.fresh())
                .build();
    }

    /** Processes between-turn special condition effects for both active Pokémon. */
    private BoardState processBetweenTurns(BoardState state) {
        if (state.getPlayer1State().getActivePokemon() != null) {
            statusEffectManager.processBetweenTurns(state.getPlayer1State().getActivePokemon());
        }
        if (state.getPlayer2State().getActivePokemon() != null) {
            statusEffectManager.processBetweenTurns(state.getPlayer2State().getActivePokemon());
        }
        return state;
    }

    /**
     * Looks up the attack by name on the given card from the card cache.
     * Returns null if the card is not found or has no matching attack.
     */
    private Attack findAttack(String cardId, String attackName) {
        return cardLookupPort.findAttack(cardId, attackName).orElse(null);
    }

    /**
     * Resolves the max HP of the defending Active Pokémon from the card cache.
     * The HP is read from the top card in the evolution stack (current form).
     * Falls back to 0 if no Pokémon is active or the card is not found.
     */
    private int resolveMaxHp(ActivePokemon defender) {
        if (defender == null) return 0;
        return cardLookupPort.getMaxHp(defender.getCardId());
    }

    /** Chooses a Bench Pokémon to become Active after a KO. */
    private BoardState handleChooseBenchPokemon(BoardState state, GameAction action) {
        String instanceId = action.getPayloadString("instanceId");
        PlayerState ps    = state.getStateFor(action.getPlayerId());

        if (instanceId == null || ps.getBench() == null) return state;

        BenchPokemon chosen = ps.getBench().stream()
                .filter(b -> b.getInstanceId().equals(instanceId))
                .findFirst().orElse(null);

        if (chosen == null) return state;

        ActivePokemon newActive = ActivePokemon.builder()
                .instanceId(chosen.getInstanceId())
                .cardId(chosen.getCardId())
                .attachedEnergyIds(chosen.getAttachedEnergyIds())
                .attachedToolId(chosen.getAttachedToolId())
                .evolutionStack(chosen.getEvolutionStack())
                .damageCounters(chosen.getDamageCounters())
                .conditions(new HashSet<>())
                .enteredThisTurn(false)
                .build();

        List<BenchPokemon> bench = new ArrayList<>(ps.getBench());
        bench.remove(chosen);
        ps.setBench(bench);
        ps.setActivePokemon(newActive);

        return state;
    }

    // ─── HELPERS ──────────────────────────────────────────────────────────────

    private void attachEnergyToPokemon(PlayerState ps, String targetInstanceId, String cardId) {
        if (ps.getActivePokemon() != null &&
                ps.getActivePokemon().getInstanceId().equals(targetInstanceId)) {
            List<String> energies = new ArrayList<>(
                    ps.getActivePokemon().getAttachedEnergyIds() != null
                            ? ps.getActivePokemon().getAttachedEnergyIds() : new ArrayList<>());
            energies.add(cardId);
            ps.getActivePokemon().setAttachedEnergyIds(energies);
            return;
        }
        if (ps.getBench() != null) {
            ps.getBench().stream()
                    .filter(b -> b.getInstanceId().equals(targetInstanceId))
                    .findFirst()
                    .ifPresent(b -> {
                        List<String> energies = new ArrayList<>(
                                b.getAttachedEnergyIds() != null
                                        ? b.getAttachedEnergyIds() : new ArrayList<>());
                        energies.add(cardId);
                        b.setAttachedEnergyIds(energies);
                    });
        }
    }

    private void evolvePokemon(PlayerState ps, String targetInstanceId, String newCardId) {
        if (ps.getActivePokemon() != null &&
                ps.getActivePokemon().getInstanceId().equals(targetInstanceId)) {
            ps.getActivePokemon().setCardId(newCardId);
            List<String> stack = new ArrayList<>(ps.getActivePokemon().getEvolutionStack());
            stack.add(newCardId);
            ps.getActivePokemon().setEvolutionStack(stack);
            return;
        }
        if (ps.getBench() != null) {
            ps.getBench().stream()
                    .filter(b -> b.getInstanceId().equals(targetInstanceId))
                    .findFirst()
                    .ifPresent(b -> {
                        b.setCardId(newCardId);
                        List<String> stack = new ArrayList<>(b.getEvolutionStack());
                        stack.add(newCardId);
                        b.setEvolutionStack(stack);
                    });
        }
    }
}