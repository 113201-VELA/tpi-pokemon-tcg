package com.pokemon.tcg.engine;

import com.pokemon.tcg.domain.model.card.Attack;
import com.pokemon.tcg.domain.model.card.EnergyType;
import com.pokemon.tcg.domain.model.game.*;
import com.pokemon.tcg.domain.strategy.ability.ActiveAbilityRegistry;
import com.pokemon.tcg.domain.strategy.trainer.TrainerEffectRegistry;
import com.pokemon.tcg.domain.strategy.attack.AttackContext;
import com.pokemon.tcg.engine.attack.AttackPipeline;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.Comparator;
@Component
public class TurnManager {

    private final RuleValidator ruleValidator;
    private final CoinFlipService coinFlipService;
    private final AttackPipeline attackPipeline;
    private final StatusEffectManager statusEffectManager;
    private final CardLookupPort cardLookupPort;
    private final TrainerEffectRegistry trainerEffectRegistry;
    private final ActiveAbilityRegistry activeAbilityRegistry;
    private final SetupManager setupManager;

    public TurnManager(RuleValidator ruleValidator,
                       CoinFlipService coinFlipService,
                       AttackPipeline attackPipeline,
                       StatusEffectManager statusEffectManager,
                       CardLookupPort cardLookupPort,
                       TrainerEffectRegistry trainerEffectRegistry,
                       ActiveAbilityRegistry activeAbilityRegistry,
                       SetupManager setupManager) {
        this.ruleValidator         = ruleValidator;
        this.coinFlipService       = coinFlipService;
        this.attackPipeline        = attackPipeline;
        this.statusEffectManager   = statusEffectManager;
        this.cardLookupPort        = cardLookupPort;
        this.trainerEffectRegistry = trainerEffectRegistry;
        this.activeAbilityRegistry = activeAbilityRegistry;
        this.setupManager = setupManager;
    }

    /**
     * Processes a game action and advances the board state accordingly.
     * Routes each action type to its specific handler.
     */
    public BoardState advancePhase(BoardState state, GameAction action) {
        return switch (action.getType()) {
            // ── Setup phase ──────────────────────────────────────────
            case MULLIGAN_CONFIRM      -> handleMulliganConfirm(state, action);
            case SETUP_PLACE_ACTIVE    -> handleSetupPlaceActive(state, action);
            case SETUP_PLACE_BENCH     -> handleSetupPlaceBench(state, action);
            case ACCEPT_MULLIGAN_BONUS -> handleAcceptMulliganBonus(state, action);
            case CONFIRM_SETUP         -> handleConfirmSetup(state, action);
            // ── Draw phase ───────────────────────────────────────────
            case DRAW_CARD             -> handleDrawCard(state, action);
            // ── Main phase ───────────────────────────────────────────
            case PLACE_BASIC_POKEMON   -> handlePlaceBasicPokemon(state, action);
            case EVOLVE_POKEMON        -> handleEvolvePokemon(state, action);
            case ATTACH_ENERGY         -> handleAttachEnergy(state, action);
            case PLAY_TRAINER          -> handlePlayTrainer(state, action);
            case USE_ABILITY           -> handleUseAbility(state, action);
            case RETREAT               -> handleRetreat(state, action);
            case DECLARE_ATTACK        -> handleDeclareAttack(state, action);
            case END_TURN              -> handleEndTurn(state, action);
            // ── Post-KO ──────────────────────────────────────────────
            case CHOOSE_BENCH_POKEMON  -> handleChooseBenchPokemon(state, action);
            // ── Deck selection ───────────────────────────────────────
            case SELECT_FROM_DECK      -> handleSelectFromDeck(state, action);
            case CONFIRM_BONUS_PLACEMENT -> setupManager.handleConfirmBonusPlacement(state, action);
            case FORCED_SWITCH         -> handleForcedSwitch(state, action);
            case DISCARD_FROM_HAND     -> handleDiscardFromHand(state, action);
            case TAKE_PRIZE            -> handleTakePrize(state, action);
            default                    -> state;
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

        if (state.isPendingDeckSelection()) {
            actions.add(GameActionType.SELECT_FROM_DECK);
            return actions;
        }

        if (state.isPendingHandDiscard()) {
            actions.add(GameActionType.DISCARD_FROM_HAND);
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
            actions.add(GameActionType.USE_ABILITY);
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

        populatePokemonModifiers(active, cardId);
        ps.setActivePokemon(active);

        return state;
    }

    /** Places a Basic Pokémon from hand onto the bench during setup. */
    private BoardState handleSetupPlaceBench(BoardState state, GameAction action) {
        boolean isInBonusPlacement = state.getPendingBonusPlacement() != null
                && state.getPendingBonusPlacement().contains(action.getPlayerId());
        boolean isBonusDrawPending = state.isBonusDrawPending();

        if (isBonusDrawPending && !isInBonusPlacement) {
            return state;
        }

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

    /**
     * Confirms mulligan — the player has no Basic Pokémon in hand.
     * Shuffles the hand back into the deck, redraws 7, and grants
     * the opponent 1 bonus draw for each mulligan declared.
     * If the new hand still has no Basic Pokémon, the player must
     * declare mulligan again.
     */
    private BoardState handleMulliganConfirm(BoardState state, GameAction action) {
        String playerId = action.getPlayerId();
        PlayerState ps  = state.getStateFor(playerId);

        // Only allow mulligan if the hand truly has no Basic Pokémon
        if (setupManager.hasBasicPokemonInHand(ps)) {
            return state;
        }

        return setupManager.handleMulligan(state, playerId);
    }

    /**
     * Handles the player's decision to accept mulligan bonus draws.
     * The player specifies how many cards to draw (0 to mulliganBonusDraws).
     * After all players with pending bonuses have decided, transitions to DRAW.
     *
     * <p>Per the rulebook, the player with the bonus chooses how many cards
     * to draw — anywhere from 0 up to their full bonus amount.
     */
    private BoardState handleAcceptMulliganBonus(BoardState state, GameAction action) {
        String playerId  = action.getPlayerId();
        PlayerState ps   = state.getStateFor(playerId);

        // cardsToDraw defaults to 0 if not specified (player declines bonus)
        Integer cardsToDraw = action.getPayloadInt("cardsToDraw");
        int drawCount = cardsToDraw != null ? cardsToDraw : 0;

        setupManager.applyMulliganBonusDraws(ps, drawCount);

        Set<String> pendingPlacement = new HashSet<>(
                state.getPendingBonusPlacement() != null
                        ? state.getPendingBonusPlacement() : new HashSet<>());

        if (drawCount > 0) {
            pendingPlacement.add(action.getPlayerId());
            state = state.toBuilder()
                    .pendingBonusPlacement(pendingPlacement)
                    .build();
        } else {
            state = setupManager.checkBonusResolution(state, pendingPlacement);
        }

        return state;
    }

    /**
     * Marks the player's setup as confirmed.
     * The actual transition to ACTIVE is handled by SetupState after
     * checking that both players have confirmed.
     */
    private BoardState handleConfirmSetup(BoardState state, GameAction action) {
        PlayerState ps = state.getStateFor(action.getPlayerId());
        ps.setSetupConfirmed(true);
        return state;
    }

    // ─── DRAW ─────────────────────────────────────────────────────────────────

    /** Draws one card from the top of the deck into the hand. */
    private BoardState handleDrawCard(BoardState state, GameAction action) {
        PlayerState ps = state.getStateFor(action.getPlayerId());

        // The first player does not draw on turn 0 (their first turn) per rulebook
        boolean isFirstPlayerFirstTurn = state.getTurnNumber() == 0
                && action.getPlayerId().equals(state.getFirstPlayerId());

        if (!isFirstPlayerFirstTurn) {
            if (ps.getDeck() == null || ps.getDeck().isEmpty()) return state;

            List<String> deck = new ArrayList<>(ps.getDeck());
            List<String> hand = new ArrayList<>(
                    ps.getHand() != null ? ps.getHand() : new ArrayList<>());

            hand.add(deck.remove(0));
            ps.setDeck(deck);
            ps.setHand(hand);
        }

        // Reset enteredThisTurn for ALL players on every draw
        resetEnteredThisTurn(state.getPlayer1State());
        resetEnteredThisTurn(state.getPlayer2State());

        // Clear self-protective effects (HARDEN, INVULNERABLE, etc.) set by this
        // player during their previous turn — they already served their purpose
        // protecting through the opponent's turn that just ended.
        if (ps.getActivePokemon() != null) {
            clearActiveEffects(ps.getActivePokemon());
        }

        return state.toBuilder()
                .turnPhase(TurnPhase.MAIN)
                .build();
    }
    private void resetEnteredThisTurn(PlayerState ps) {
        if (ps.getActivePokemon() != null) {
            ps.getActivePokemon().setEnteredThisTurn(false);
        }
        if (ps.getBench() != null) {
            ps.getBench().forEach(b -> b.setEnteredThisTurn(false));
        }
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

        System.out.println("ATTACH hand: " + ps.getHand());
        System.out.println("ATTACH cardId: " + cardId + " targetId: " + targetId);
        System.out.println("ATTACH contains: " + ps.getHand().contains(cardId));

        List<String> hand = new ArrayList<>(ps.getHand());
        hand.remove(cardId);
        ps.setHand(hand);

        attachEnergyToPokemon(ps, targetId, cardId);
        state.getTurnFlags().setEnergyAttachedThisTurn(true);

        // Rainbow Energy places 1 damage counter on the Pokémon it is attached to
        if (isRainbowEnergy(cardId)) {
            applyRainbowEnergyDamage(ps, targetId);
        }

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

        // Pokémon Tools stay attached to the Pokémon — do not discard them
        boolean isPokemonTool = cardLookupPort.findCardById(cardId)
                .map(card -> card.getSubtypes() != null
                        && card.getSubtypes().contains("Pokémon Tool"))
                .orElse(false);

        if (!isPokemonTool) {
            List<String> discard = new ArrayList<>(
                    ps.getDiscard() != null ? ps.getDiscard() : new ArrayList<>());
            discard.add(cardId);
            ps.setDiscard(discard);
        }

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

        // Mark this Pokémon as evolved this turn to prevent double evolution
        state.getTurnFlags().markEvolved(targetId);

        // If the evolution card is a MEGA, the turn ends immediately
        boolean isMega = cardLookupPort.findCardById(cardId)
                .map(card -> card.getSubtypes() != null && card.getSubtypes().contains("MEGA"))
                .orElse(false);

        if (isMega) {
            String opponentId = action.getPlayerId().equals(state.getPlayer1State().getPlayerId())
                    ? state.getPlayer2State().getPlayerId()
                    : state.getPlayer1State().getPlayerId();

            state = processBetweenTurns(state);

            return state.toBuilder()
                    .currentPlayerId(opponentId)
                    .turnPhase(TurnPhase.DRAW)
                    .turnNumber(state.getTurnNumber() + 1)
                    .turnFlags(TurnFlags.fresh())
                    .build();
        }

        return state;
    }

    /** Retreats the Active Pokémon to the bench, discarding energies as retreat cost. */
    private BoardState handleRetreat(BoardState state, GameAction action) {
        String replacementId = action.getPayloadString("replacementInstanceId");
        PlayerState ps       = state.getStateFor(action.getPlayerId());

        if (replacementId == null || ps.getActivePokemon() == null) return state;
        if (state.getTurnFlags().isRetreatedThisTurn()) return state;

        BenchPokemon replacement = ps.getBench().stream()
                .filter(b -> b.getInstanceId().equals(replacementId))
                .findFirst().orElse(null);

        if (replacement == null) return state;

        // Discard the energies paid as retreat cost,
        // unless Fairy Garden suppresses the retreat cost for this Pokémon
        if (!isFairyGardenRetreatFree(state, ps.getActivePokemon())) {
            List<String> energiesToDiscard = getEnergiesToDiscard(action);
            discardRetreatCostEnergies(ps, energiesToDiscard);
        }

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
        populatePokemonModifiers(newActive, replacement.getCardId());
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
     *
     * <p>If a KO occurred and the defender has bench Pokémon, the turn is suspended
     * until the defender sends CHOOSE_BENCH_POKEMON. Between-turn effects and the
     * turn switch happen only after the bench choice is resolved.
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
        if (ctx.isCancelled()) {
            List<GameEvent> pending = new ArrayList<>();

            // Coin flip events goes first
            if (ctx.getEvents() != null) {
                pending.addAll(ctx.getEvents());
            }

            // Next, error event
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

        // If a KO occurred and the defender has bench Pokémon, OR a hand discard
        // is pending (e.g. Mental Trash), OR prize taking is pending, suspend the
        // turn. Between-turn effects and turn switch happen only after the pending
        // action is resolved.
        if (ctx.getBoardState().isPendingBenchChoice()
                || ctx.getBoardState().isPendingHandDiscard()
                || ctx.getBoardState().getPendingPrizeTakePlayerId() != null) {
            List<GameEvent> pending = new ArrayList<>();
            if (ctx.getEvents() != null) {
                pending.addAll(ctx.getEvents());
            }
            if (ctx.getBoardState().getPendingEvents() != null) {
                pending.addAll(ctx.getBoardState().getPendingEvents());
            }
            return ctx.getBoardState().toBuilder()
                    .pendingEvents(pending)
                    .build();
        }

        // Attack resolved with no pending bench choice — process between-turn effects
        // and switch to the opponent's turn normally.
        state = processBetweenTurns(ctx.getBoardState());
        state.getTurnFlags().setAttackedThisTurn(true);

        // Collect events from the attack pipeline (coin flips, etc.)
        List<GameEvent> attackEvents = ctx.getEvents() != null
                ? new ArrayList<>(ctx.getEvents())
                : new ArrayList<>();

        // Merge with any existing pending events
        List<GameEvent> allPending = new ArrayList<>(
                state.getPendingEvents() != null ? state.getPendingEvents() : new ArrayList<>());
        allPending.addAll(attackEvents);

        System.out.println("CTX EVENTS before return: " +
                (ctx.getEvents() != null ? ctx.getEvents().size() : "null") +
                " types: " + (ctx.getEvents() != null ?
                ctx.getEvents().stream().map(e -> e.getType().name()).toList() : "[]"));
        System.out.println("ALL PENDING: " + allPending.stream().map(e -> e.getType().name()).toList());

        return state.toBuilder()
                .currentPlayerId(opponentId)
                .turnPhase(TurnPhase.DRAW)
                .turnNumber(state.getTurnNumber() + 1)
                .turnFlags(TurnFlags.fresh())
                .pendingEvents(allPending)
                .build();
    }

    /**
     * Ends the turn without attacking, processes between-turn effects
     * and passes control to the opponent.
     */
    private BoardState handleEndTurn(BoardState state, GameAction action) {
        state = processBetweenTurns(state);

        // If processBetweenTurns caused a KO, a bench choice may be pending.
        // Suspend the turn transition and store who should play next so
        // handleChooseBenchPokemon can use it after bench choice is resolved.
        if (state.getPendingBenchChoicePlayerId() != null) {
            String opponentId = getOpponentId(state, action.getPlayerId());
            return state.toBuilder()
                    .turnFlags(TurnFlags.fresh())
                    .pendingNextPlayerId(opponentId)
                    .build();
        }

        // Normal turn transition
        String opponentId = getOpponentId(state, action.getPlayerId());

        return state.toBuilder()
                .currentPlayerId(opponentId)
                .turnPhase(TurnPhase.DRAW)
                .turnNumber(state.getTurnNumber() + 1)
                .turnFlags(TurnFlags.fresh())
                .build();
    }

    /**
     * Process special condition effects between turns for both active Pokémon (POISON, BURN),
     * then check if any Pokémon were knocked out by that damage.
     * The player who just finished their turn is the 'attacker' for prize purposes — their
     * opponent takes prizes if their Pokémon is knocked out by conditions. currentPlayerId
     * at this point still refers to the player who just acted.
     */
    private BoardState processBetweenTurns(BoardState state) {
        if (state.getPlayer1State().getActivePokemon() != null) {
            statusEffectManager.processBetweenTurns(state.getPlayer1State().getActivePokemon());
        }
        if (state.getPlayer2State().getActivePokemon() != null) {
            statusEffectManager.processBetweenTurns(state.getPlayer2State().getActivePokemon());
        }

        state = checkConditionKnockouts(state);

        return state;
    }

    private BoardState checkConditionKnockouts(BoardState state) {
        KOResult r1 = checkSingleConditionKO(state,
                state.getPlayer1State(), state.getPlayer2State());
        KOResult r2 = checkSingleConditionKO(state,
                state.getPlayer2State(), state.getPlayer1State());

        // Accumulate events
        List<GameEvent> allEvents = new ArrayList<>();
        allEvents.addAll(r1.events());
        allEvents.addAll(r2.events());

        if (!allEvents.isEmpty()) {
            List<GameEvent> pending = new ArrayList<>(
                    state.getPendingEvents() != null ? state.getPendingEvents() : new ArrayList<>());
            pending.addAll(allEvents);
            state = state.toBuilder().pendingEvents(pending).build();
        }

        // Flag bench choice
        if (state.getPlayer1State().getActivePokemon() == null
                && state.getPlayer1State().getBench() != null
                && !state.getPlayer1State().getBench().isEmpty()) {
            state = state.toBuilder()
                    .pendingBenchChoicePlayerId(state.getPlayer1State().getPlayerId())
                    .build();
        }
        if (state.getPlayer2State().getActivePokemon() == null
                && state.getPlayer2State().getBench() != null
                && !state.getPlayer2State().getBench().isEmpty()) {
            state = state.toBuilder()
                    .pendingBenchChoicePlayerId(state.getPlayer2State().getPlayerId())
                    .build();
        }

        // Flag prize take — accumulate in case both players KO'd each other
        int totalPrizes = 0;
        String prizeWinnerId = null;
        if (r1.prizesToTake() > 0) {
            totalPrizes += r1.prizesToTake();
            prizeWinnerId = r1.prizeWinnerId();
        }
        if (r2.prizesToTake() > 0) {
            totalPrizes += r2.prizesToTake();
            // If both players KO'd each other simultaneously, handle separately
            // For now, last writer wins — edge case for future handling
            prizeWinnerId = r2.prizeWinnerId();
        }
        if (totalPrizes > 0) {
            int currentPending = state.getPendingPrizeTakeCount();
            state = state.toBuilder()
                    .pendingPrizeTakePlayerId(prizeWinnerId)
                    .pendingPrizeTakeCount(currentPending + totalPrizes)
                    .build();
        }

        return state;
    }

    private KOResult checkSingleConditionKO(BoardState state,
                                            PlayerState owner,
                                            PlayerState prizeWinner) {
        ActivePokemon active = owner.getActivePokemon();
        if (active == null) return KOResult.none();

        int maxHp = cardLookupPort.getMaxHp(active.getCardId());
        if (maxHp <= 0) return KOResult.none();
        if (active.getDamageCounters() * 10 < maxHp) return KOResult.none();

        // Move KO'd Pokémon to discard
        List<String> discard = new ArrayList<>(
                owner.getDiscard() != null ? owner.getDiscard() : new ArrayList<>());
        discard.add(active.getCardId());
        if (active.getAttachedEnergyIds() != null) {
            discard.addAll(active.getAttachedEnergyIds());
        }
        if (active.getAttachedToolId() != null) {
            discard.add(active.getAttachedToolId());
        }
        owner.setDiscard(discard);
        owner.setActivePokemon(null);

        int prizesToTake = resolvePrizeCountForCard(active.getCardId());
        int available = prizeWinner.getPrizes() != null ? prizeWinner.getPrizes().size() : 0;

        List<GameEvent> events = new ArrayList<>();
        events.add(GameEvent.builder()
                .type(GameEventType.POKEMON_KNOCKED_OUT)
                .gameId(state.getGameId())
                .playerId(owner.getPlayerId())
                .turnNumber(state.getTurnNumber())
                .data(Map.of("knockedOutCardId", active.getCardId()))
                .occurredAt(Instant.now())
                .build());

        return new KOResult(events, prizeWinner.getPlayerId(),
                Math.min(prizesToTake, available));
    }

    private int resolvePrizeCountForCard(String cardId) {
        return cardLookupPort.findCardById(cardId)
                .map(card -> {
                    List<String> subtypes = card.getSubtypes();
                    if (subtypes != null &&
                            (subtypes.contains("EX") || subtypes.contains("MEGA"))) {
                        return 2;
                    }
                    return 1;
                })
                .orElse(1);
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

    /**
     * Chooses a Bench Pokémon to become Active after a KO, then resumes the
     * turn switch.
     * <p>
     * Two distinct scenarios reach this handler, and they resolve the next
     * player differently:
     * <ul>
     *   <li><b>Defender was KO'd by the attacker's attack</b> — currentPlayerId
     *       is still the attacker (handleDeclareAttack never switched it while
     *       the bench choice was pending). The defender (the one sending this
     *       action) picks their replacement, and it's now correctly their
     *       turn — nextPlayerId = the chooser.</li>
     *   <li><b>Attacker KO'd their own Pokémon</b> (e.g. Confusion self-damage,
     *       now that ConfusionCheckStep continues the pipeline on tails
     *       instead of cancelling) — currentPlayerId is STILL the attacker,
     *       and the attacker is also the one sending this action (they own
     *       the KO'd Pokémon). Since the attacker already used their turn by
     *       attacking, nextPlayerId = the OPPONENT of the chooser.</li>
     * </ul>
     * These two cases are told apart by comparing action.getPlayerId() against
     * state.getCurrentPlayerId() BEFORE this handler mutates it: equal means
     * self-KO, different means opponent-KO.
     */
    private BoardState handleChooseBenchPokemon(BoardState state, GameAction action) {
        String instanceId = action.getPayloadString("instanceId");
        PlayerState ps    = state.getStateFor(action.getPlayerId());

        if (instanceId == null || ps.getBench() == null) return state;

        BenchPokemon chosen = ps.getBench().stream()
                .filter(b -> b.getInstanceId().equals(instanceId))
                .findFirst().orElse(null);

        if (chosen == null) return state;

        // Capture BEFORE mutating state — this is the disambiguation signal.
        boolean isSelfKo = action.getPlayerId().equals(state.getCurrentPlayerId());

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
        populatePokemonModifiers(newActive, chosen.getCardId());
        ps.setActivePokemon(newActive);

        // Clear pending bench choice flag
        state = state.toBuilder()
                .pendingBenchChoicePlayerId(null)
                .build();

        state = processBetweenTurns(state);

        // If pendingNextPlayerId is set (e.g. from handleEndTurn condition KO),
        // use it. Otherwise resolve based on whether this was a self-KO.
        String nextPlayerId;
        if (state.getPendingNextPlayerId() != null) {
            nextPlayerId = state.getPendingNextPlayerId();
        } else if (isSelfKo) {
            nextPlayerId = getOpponentId(state, action.getPlayerId());
        } else {
            nextPlayerId = action.getPlayerId();
        }

        return state.toBuilder()
                .currentPlayerId(nextPlayerId)
                .turnPhase(TurnPhase.DRAW)
                .turnNumber(state.getTurnNumber() + 1)
                .turnFlags(TurnFlags.fresh())
                .pendingNextPlayerId(null)
                .build();
    }

    /**
     * Resolves a pending hand discard (e.g. Malamar's Mental Trash). Moves the
     * chosen cards from the player's hand to their discard pile, clears the
     * pending flag, then resumes the turn switch that DECLARE_ATTACK suspended.
     */
    private BoardState handleDiscardFromHand(BoardState state, GameAction action) {
        if (!state.isPendingHandDiscard()) return state;
        if (!state.getPendingHandDiscardPlayerId().equals(action.getPlayerId())) return state;

        List<String> chosenCardIds = getChosenCardIds(action);
        if (chosenCardIds.size() != state.getPendingHandDiscardCount()) return state;

        PlayerState ps = state.getStateFor(action.getPlayerId());
        List<String> hand = new ArrayList<>(
                ps.getHand() != null ? ps.getHand() : new ArrayList<>());
        List<String> discard = new ArrayList<>(
                ps.getDiscard() != null ? ps.getDiscard() : new ArrayList<>());

        for (String cardId : chosenCardIds) {
            if (!hand.remove(cardId)) return state; // invalid card — abort, nothing applied
        }
        discard.addAll(chosenCardIds);
        ps.setHand(hand);
        ps.setDiscard(discard);

        state = state.toBuilder()
                .pendingHandDiscardPlayerId(null)
                .pendingHandDiscardCount(0)
                .build();

        state = processBetweenTurns(state);

        String opponentId = action.getPlayerId().equals(state.getPlayer1State().getPlayerId())
                ? state.getPlayer2State().getPlayerId()
                : state.getPlayer1State().getPlayerId();

        return state.toBuilder()
                .currentPlayerId(opponentId)
                .turnPhase(TurnPhase.DRAW)
                .turnNumber(state.getTurnNumber() + 1)
                .turnFlags(TurnFlags.fresh())
                .build();
    }

    /**
     * Maneja TAKE_PRIZE — el atacante selecciona qué carta(s) de Premio tomar
     * después de un KO. Los índices deben ser válidos y coincidir con la
     * cantidad esperada (pendingPrizeTakeCount). Emite PRIZE_TAKEN al completarse.
     */
    private BoardState handleTakePrize(BoardState state, GameAction action) {
        String playerId = action.getPlayerId();

        if (!playerId.equals(state.getPendingPrizeTakePlayerId())) {
            return state;
        }

        PlayerState ps = state.getStateFor(playerId);
        List<String> prizes = ps.getPrizes() != null
                ? new ArrayList<>(ps.getPrizes()) : new ArrayList<>();

        @SuppressWarnings("unchecked")
        List<Integer> selectedIndices = (List<Integer>) action.getPayload()
                .getOrDefault("prizeIndices", List.of());

        int expectedCount = state.getPendingPrizeTakeCount();

        if (selectedIndices.size() != expectedCount) return state;

        List<Integer> sorted = selectedIndices.stream()
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList();

        if (sorted.size() != expectedCount) return state;
        if (sorted.get(0) >= prizes.size()) return state;

        List<String> hand = new ArrayList<>(ps.getHand() != null ? ps.getHand() : new ArrayList<>());
        for (int idx : sorted) {
            hand.add(prizes.remove(idx));
        }

        ps.setPrizes(prizes);
        ps.setHand(hand);

        // Se emite PRIZE_TAKEN via pendingEvents
        List<GameEvent> pending = new ArrayList<>(
                state.getPendingEvents() != null ? state.getPendingEvents() : new ArrayList<>());
        pending.add(GameEvent.builder()
                .type(GameEventType.PRIZE_TAKEN)
                .gameId(state.getGameId())
                .playerId(playerId)
                .turnNumber(state.getTurnNumber())
                .data(Map.of("prizesRemaining", ps.getPrizes().size()))
                .occurredAt(Instant.now())
                .build());

        return state.toBuilder()
                .pendingPrizeTakePlayerId(null)
                .pendingPrizeTakeCount(0)
                .pendingEvents(pending)
                .build();
    }

    /**
     * Handles the player selecting a card from a set revealed by a card effect
     * (e.g. Great Ball). The chosen card is moved to the player's hand and the
     * remaining revealed cards are shuffled back into the deck.
     */
    private BoardState handleSelectFromDeck(BoardState state, GameAction action) {
        if (state.isPendingAttackSelection()) {
            return handleAttackDrivenSelection(state, action);
        }
        if (state.isPendingDeckSelection()) {
            return handleTrainerDrivenSelection(state, action);
        }
        return state;
    }

    private BoardState handleAttackDrivenSelection(BoardState state, GameAction action) {
        String playerId = action.getPlayerId();
        if (!state.getPendingAttackSelectionPlayerId().equals(playerId)) return state;

        AttackSelectionType selectionType = state.getPendingAttackSelectionType() != null
                ? state.getPendingAttackSelectionType()
                : AttackSelectionType.PICK;

        BoardState resolved = switch (selectionType) {
            case PICK    -> handlePickSelection(state, action);
            case REORDER -> handleReorderSelection(state, action);
        };

        return resolved.toBuilder()
                .pendingAttackSelectionKey(null)
                .pendingAttackSelectionPlayerId(null)
                .pendingAttackSelectionMaxCards(1)
                .pendingAttackSelectionType(AttackSelectionType.PICK)
                .pendingDeckSelectionCardIds(new ArrayList<>())
                .build();
    }

    private BoardState handlePickSelection(BoardState state, GameAction action) {
        List<String> chosenCardIds = getChosenCardIds(action);
        List<String> pendingCards  = state.getPendingDeckSelectionCardIds();
        PlayerState ps             = state.getStateFor(action.getPlayerId());
        int maxCards               = state.getPendingAttackSelectionMaxCards();

        List<String> hand = new ArrayList<>(
                ps.getHand() != null ? ps.getHand() : new ArrayList<>());
        List<String> deck = new ArrayList<>(
                ps.getDeck() != null ? ps.getDeck() : new ArrayList<>());

        int moved = 0;
        for (String cardId : chosenCardIds) {
            if (moved >= maxCards) break;
            if (pendingCards != null && pendingCards.contains(cardId)
                    && deck.contains(cardId)) {
                deck.remove(cardId);
                hand.add(cardId);
                moved++;
            }
        }

        Collections.shuffle(deck);
        ps.setHand(hand);
        ps.setDeck(deck);

        return state;
    }

    private BoardState handleReorderSelection(BoardState state, GameAction action) {
        List<String> orderedIds   = getOrderedCardIds(action);
        List<String> pendingCards = state.getPendingDeckSelectionCardIds();
        PlayerState ps            = state.getStateFor(action.getPlayerId());

        List<String> deck = new ArrayList<>(
                ps.getDeck() != null ? ps.getDeck() : new ArrayList<>());

        if (pendingCards != null) {
            pendingCards.forEach(deck::remove);
        }

        List<String> newTop = new ArrayList<>();
        for (String cardId : orderedIds) {
            if (pendingCards != null && pendingCards.contains(cardId)) {
                newTop.add(cardId);
            }
        }
        if (pendingCards != null) {
            for (String cardId : pendingCards) {
                if (!newTop.contains(cardId)) {
                    newTop.add(cardId);
                }
            }
        }

        deck.addAll(0, newTop);
        ps.setDeck(deck);

        return state;
    }

    private BoardState handleTrainerDrivenSelection(BoardState state, GameAction action) {
        String playerId = action.getPlayerId();
        if (!state.getPendingDeckSelectionPlayerId().equals(playerId)) return state;

        String chosenCardId       = action.getPayloadString("cardId");
        List<String> pendingCards = state.getPendingDeckSelectionCardIds();

        if (chosenCardId == null
                || pendingCards == null
                || !pendingCards.contains(chosenCardId)) {
            return state;
        }

        PlayerState ps = state.getStateFor(playerId);

        List<String> hand = new ArrayList<>(
                ps.getHand() != null ? ps.getHand() : new ArrayList<>());
        hand.add(chosenCardId);
        ps.setHand(hand);

        List<String> remaining = new ArrayList<>(pendingCards);
        remaining.remove(chosenCardId);
        List<String> deck = new ArrayList<>(
                ps.getDeck() != null ? ps.getDeck() : new ArrayList<>());
        deck.addAll(remaining);
        Collections.shuffle(deck);
        ps.setDeck(deck);

        return state.toBuilder()
                .pendingDeckSelectionPlayerId(null)
                .pendingDeckSelectionCardIds(new ArrayList<>())
                .build();
    }

    private BoardState handleForcedSwitch(BoardState state, GameAction action) {
        if (!state.isPendingForcedSwitch()) return state;
        if (!state.getPendingForcedSwitchPlayerId().equals(action.getPlayerId())) return state;

        String instanceId = action.getPayloadString("instanceId");
        PlayerState ps    = state.getStateFor(action.getPlayerId());

        if (instanceId == null || ps.getBench() == null) return state;

        BenchPokemon chosen = ps.getBench().stream()
                .filter(b -> b.getInstanceId().equals(instanceId))
                .findFirst().orElse(null);

        if (chosen == null) return state;

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
                .instanceId(chosen.getInstanceId())
                .cardId(chosen.getCardId())
                .attachedEnergyIds(chosen.getAttachedEnergyIds())
                .attachedToolId(chosen.getAttachedToolId())
                .evolutionStack(chosen.getEvolutionStack())
                .damageCounters(chosen.getDamageCounters())
                .conditions(new HashSet<>())
                .activeEffects(new ArrayList<>())
                .enteredThisTurn(true)
                .build();

        List<BenchPokemon> bench = new ArrayList<>(ps.getBench());
        bench.remove(chosen);
        bench.add(newBench);
        ps.setBench(bench);
        populatePokemonModifiers(newActive, chosen.getCardId());
        ps.setActivePokemon(newActive);

        return state.toBuilder()
                .pendingForcedSwitchPlayerId(null)
                .build();
    }

    // ─── USE_ABILITY ───────────────────────────────────────────────────────────

    /**
     * Handles a USE_ABILITY action by looking up the active ability for the
     * Pokémon identified by {@code instanceId} in the payload and applying it.
     *
     * <p>Payload expected:
     * <ul>
     *   <li>{@code instanceId} — instanceId of the Pokémon using the ability.</li>
     *   <li>{@code abilityName} — name of the ability to use.</li>
     * </ul>
     *
     * <p>If the ability is not registered or cannot be applied, returns the
     * board state unchanged.
     */
    private BoardState handleUseAbility(BoardState state, GameAction action) {
        String instanceId  = action.getPayloadString("instanceId");
        String abilityName = action.getPayloadString("abilityName");

        if (instanceId == null || abilityName == null) return state;

        String cardId = resolveCardIdByInstanceId(state, action.getPlayerId(), instanceId);
        if (cardId == null) return state;

        return cardLookupPort.findCardById(cardId)
                .flatMap(card -> activeAbilityRegistry.findAbility(card.getName(), abilityName))
                .filter(ability -> ability.canApply(state, action).isValid())
                .map(ability -> ability.apply(state, action))
                .orElse(state);
    }

    /**
     * Resolves the card ID of a Pokémon in play (Active or Bench) by its instanceId.
     * Returns null if no matching Pokémon is found.
     */
    private String resolveCardIdByInstanceId(BoardState state, String playerId,
                                              String instanceId) {
        PlayerState ps = state.getStateFor(playerId);

        if (ps.getActivePokemon() != null
                && ps.getActivePokemon().getInstanceId().equals(instanceId)) {
            return ps.getActivePokemon().getCardId();
        }
        if (ps.getBench() != null) {
            return ps.getBench().stream()
                    .filter(b -> b.getInstanceId().equals(instanceId))
                    .map(BenchPokemon::getCardId)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    // ─── HELPERS ──────────────────────────────────────────────────────────────

    private static final String FAIRY_GARDEN = "xy1-117";

    /**
     * Returns true if Fairy Garden is active and the given Pokémon qualifies
     * for free retreat (Fairy type with at least one Fairy Basic Energy attached).
     */
    private boolean isFairyGardenRetreatFree(BoardState state, ActivePokemon active) {
        if (!FAIRY_GARDEN.equals(state.getActiveStadiumCardId())) return false;
        if (active == null) return false;
        if (active.getTypes() == null
                || !active.getTypes().contains(
                        com.pokemon.tcg.domain.model.card.EnergyType.FAIRY)) return false;
        if (active.getAttachedEnergyIds() == null
                || active.getAttachedEnergyIds().isEmpty()) return false;
        return active.getAttachedEnergyIds().stream()
                .anyMatch(this::isFairyBasicEnergy);
    }

    /**
     * Returns true if the card identified by the given ID is a Fairy Basic Energy.
     */
    private boolean isFairyBasicEnergy(String cardId) {
        return cardLookupPort.findCardById(cardId)
                .map(card -> card.isBasicEnergy()
                        && card.getTypes() != null
                        && card.getTypes().contains(
                                com.pokemon.tcg.domain.model.card.EnergyType.FAIRY.name()))
                .orElse(false);
    }

    /**
     * Removes the specified energies from the Active Pokémon's attached energies
     * and adds them to the player's discard pile.
     */
    private void discardRetreatCostEnergies(PlayerState ps, List<String> energiesToDiscard) {
        if (energiesToDiscard == null || energiesToDiscard.isEmpty()) return;

        ActivePokemon active = ps.getActivePokemon();
        List<String> attached = new ArrayList<>(
                active.getAttachedEnergyIds() != null
                        ? active.getAttachedEnergyIds() : new ArrayList<>());

        for (String energyId : energiesToDiscard) {
            attached.remove(energyId);
        }
        active.setAttachedEnergyIds(attached);

        List<String> discard = new ArrayList<>(
                ps.getDiscard() != null ? ps.getDiscard() : new ArrayList<>());
        discard.addAll(energiesToDiscard);
        ps.setDiscard(discard);
    }

    /**
     * Extracts the list of energy card IDs to discard from the action payload.
     * Returns an empty list if the payload key is absent or null.
     */
    @SuppressWarnings("unchecked")
    private List<String> getEnergiesToDiscard(GameAction action) {
        Object raw = action.getPayload() != null
                ? action.getPayload().get("energyCardIdsToDiscard")
                : null;
        if (raw instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }

    private static final String RAINBOW_ENERGY_NAME = "rainbow energy";

    /**
     * Returns true if the given card ID corresponds to Rainbow Energy,
     * identified by card name (case-insensitive) from the card cache.
     */
    private boolean isRainbowEnergy(String cardId) {
        return cardLookupPort.findCardById(cardId)
                .map(card -> RAINBOW_ENERGY_NAME.equals(
                        card.getName() != null ? card.getName().toLowerCase() : ""))
                .orElse(false);
    }

    /**
     * Places 1 damage counter on the target Pokémon after Rainbow Energy is attached.
     * Applies to both the Active Pokémon and Bench Pokémon.
     */
    private void applyRainbowEnergyDamage(PlayerState ps, String targetInstanceId) {
        if (ps.getActivePokemon() != null &&
                ps.getActivePokemon().getInstanceId().equals(targetInstanceId)) {
            ps.getActivePokemon().setDamageCounters(
                    ps.getActivePokemon().getDamageCounters() + 1);
            return;
        }
        if (ps.getBench() != null) {
            ps.getBench().stream()
                    .filter(b -> b.getInstanceId().equals(targetInstanceId))
                    .findFirst()
                    .ifPresent(b -> b.setDamageCounters(b.getDamageCounters() + 1));
        }
    }

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
            ps.getActivePokemon().setBlockedAttackName(null);
            if (ps.getActivePokemon().getActiveEffects() != null) {
                ps.getActivePokemon().getActiveEffects().clear();
            }
            // Evolving clears all special conditions per rulebook
            ps.getActivePokemon().setConditions(new HashSet<>());
            List<String> stack = new ArrayList<>(ps.getActivePokemon().getEvolutionStack());
            stack.add(newCardId);
            ps.getActivePokemon().setEvolutionStack(stack);
            populatePokemonModifiers(ps.getActivePokemon(), newCardId);
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

    private void clearActiveEffects(ActivePokemon pokemon) {
        if (pokemon.getActiveEffects() != null) {
            pokemon.getActiveEffects().clear();
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> getChosenCardIds(GameAction action) {
        Object raw = action.getPayload() != null
                ? action.getPayload().get("chosenCardIds")
                : null;
        if (raw instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<String> getOrderedCardIds(GameAction action) {
        Object raw = action.getPayload() != null
                ? action.getPayload().get("orderedCardIds")
                : null;
        if (raw instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }

    private String getOpponentId(BoardState state, String playerId) {
        return playerId.equals(state.getPlayer1State().getPlayerId())
                ? state.getPlayer2State().getPlayerId()
                : state.getPlayer1State().getPlayerId();
    }

    /**
     * Resolves weaknesses, resistances and types from the card cache
     * and applies them to the given ActivePokemon builder.
     * Falls back to empty lists if the card is not found.
     */
    private void populatePokemonModifiers(ActivePokemon pokemon, String cardId) {
        cardLookupPort.findCardById(cardId).ifPresent(card -> {
            if (card.getWeaknesses() != null) {
                pokemon.setWeaknesses(new ArrayList<>(card.getWeaknesses()));
            }
            if (card.getResistances() != null) {
                pokemon.setResistances(new ArrayList<>(card.getResistances()));
            }
            if (card.getTypes() != null) {
                List<EnergyType> energyTypes = card.getTypes().stream()
                        .map(t -> {
                            try {
                                return EnergyType.valueOf(t.toUpperCase());
                            } catch (IllegalArgumentException e) {
                                return null;
                            }
                        })
                        .filter(t -> t != null)
                        .collect(java.util.stream.Collectors.toList());
                pokemon.setTypes(energyTypes);
            }
        });
    }

    private record KOResult(List<GameEvent> events, String prizeWinnerId, int prizesToTake) {
        static KOResult none() {
            return new KOResult(List.of(), null, 0);
        }
    }
}