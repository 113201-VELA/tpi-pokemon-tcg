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
    private final SetupManager setupManager;

    public TurnManager(RuleValidator ruleValidator,
                       CoinFlipService coinFlipService,
                       AttackPipeline attackPipeline,
                       StatusEffectManager statusEffectManager,
                       CardLookupPort cardLookupPort,
                       TrainerEffectRegistry trainerEffectRegistry,
                       SetupManager setupManager) {
        this.ruleValidator         = ruleValidator;
        this.coinFlipService       = coinFlipService;
        this.attackPipeline        = attackPipeline;
        this.statusEffectManager   = statusEffectManager;
        this.cardLookupPort        = cardLookupPort;
        this.trainerEffectRegistry = trainerEffectRegistry;
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
            case USE_ABILITY           -> state;
            case RETREAT               -> handleRetreat(state, action);
            case DECLARE_ATTACK        -> handleDeclareAttack(state, action);
            case END_TURN              -> handleEndTurn(state, action);
            // ── Post-KO ──────────────────────────────────────────────
            case CHOOSE_BENCH_POKEMON  -> handleChooseBenchPokemon(state, action);
            // ── Deck selection ───────────────────────────────────────
            case SELECT_FROM_DECK      -> handleSelectFromDeck(state, action);
            case FORCED_SWITCH         -> handleForcedSwitch(state, action);
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

        // If no more pending bonuses, transition to DRAW phase
        if (!state.hasAnyPendingBonus()) {
            return state.toBuilder()
                    .bonusDrawPending(false)
                    .turnPhase(TurnPhase.DRAW)
                    .gameState(GameState.ACTIVE)
                    .build();
        }

        return state.toBuilder()
                .bonusDrawPending(true)
                .build();
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

        // If a KO occurred and the defender has bench Pokémon, suspend the turn.
        // Between-turn effects and turn switch will happen after CHOOSE_BENCH_POKEMON.
        if (ctx.getBoardState().isPendingBenchChoice()) {
            return ctx.getBoardState();
        }

        // Attack resolved with no pending bench choice — process between-turn effects
        // and switch to the opponent's turn normally.
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
            clearActiveEffects(state.getPlayer1State().getActivePokemon());
        }
        if (state.getPlayer2State().getActivePokemon() != null) {
            statusEffectManager.processBetweenTurns(state.getPlayer2State().getActivePokemon());
            clearActiveEffects(state.getPlayer2State().getActivePokemon());
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

    /** Chooses a Bench Pokémon to become Active after a KO, then resumes the turn switch. */
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

        // Clear the pending bench choice flag
        state = state.toBuilder()
                .pendingBenchChoicePlayerId(null)
                .build();

        // Now that the defender has a new Active, resume the suspended turn:
        // process between-turn effects and pass control to the original attacker's opponent.
        // The attacker is whoever is NOT the player who just chose.
        String attackerId = action.getPlayerId().equals(state.getPlayer1State().getPlayerId())
                ? state.getPlayer2State().getPlayerId()
                : state.getPlayer1State().getPlayerId();

        state = processBetweenTurns(state);

        return state.toBuilder()
                .currentPlayerId(attackerId)
                .turnPhase(TurnPhase.DRAW)
                .turnNumber(state.getTurnNumber() + 1)
                .turnFlags(TurnFlags.fresh())
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

        String chosenCardId       = action.getPayloadString("cardId");
        List<String> pendingCards = state.getPendingDeckSelectionCardIds();
        PlayerState ps            = state.getStateFor(playerId);

        if (chosenCardId != null
                && pendingCards != null
                && pendingCards.contains(chosenCardId)) {
            List<String> hand = new ArrayList<>(
                    ps.getHand() != null ? ps.getHand() : new ArrayList<>());
            hand.add(chosenCardId);
            ps.setHand(hand);

            List<String> deck = new ArrayList<>(
                    ps.getDeck() != null ? ps.getDeck() : new ArrayList<>());
            deck.remove(chosenCardId);
            Collections.shuffle(deck);
            ps.setDeck(deck);
        } else {
            List<String> deck = new ArrayList<>(
                    ps.getDeck() != null ? ps.getDeck() : new ArrayList<>());
            Collections.shuffle(deck);
            ps.setDeck(deck);
        }

        return state.toBuilder()
                .pendingAttackSelectionKey(null)
                .pendingAttackSelectionPlayerId(null)
                .pendingDeckSelectionCardIds(new ArrayList<>())
                .build();
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
                .enteredThisTurn(false)
                .build();

        List<BenchPokemon> bench = new ArrayList<>(ps.getBench());
        bench.remove(chosen);
        bench.add(newBench);
        ps.setBench(bench);
        ps.setActivePokemon(newActive);

        return state.toBuilder()
                .pendingForcedSwitchPlayerId(null)
                .build();
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

    private void clearActiveEffects(ActivePokemon pokemon) {
        if (pokemon.getActiveEffects() != null) {
            pokemon.getActiveEffects().clear();
        }
    }
}