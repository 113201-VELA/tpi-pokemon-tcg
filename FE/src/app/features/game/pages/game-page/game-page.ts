import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  OnDestroy,
  OnInit,
  signal,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../auth/data-access/services/auth.service';
import { GameStateService } from '../../data-access/services/game-state.service';
import { GameActionService } from '../../data-access/services/game-action.service';
import {
  GameStateResponse,
  OwnPlayerState,
  OpponentPlayerState,
  PublicBoardStateDTO,
  PublicPlayerStateDTO,
} from '../../domain/models/game.models';
import { CardResponse } from '../../../deck-builder/domain/models/card.models';
import { ActivePokemonSlot } from '../../components/active-pokemon-slot/active-pokemon-slot';
import { BenchPokemonSlot } from '../../components/bench-pokemon-slot/bench-pokemon-slot';
import { DraggableCardDirective } from '../../../../shared/directives/draggable-card.directive';
import { DropZoneDirective } from '../../../../shared/directives/drop-zone.directive';
import { DragStateService } from '../../../../shared/services/drag-state.service';

@Component({
  selector: 'app-game-page',
  imports: [ActivePokemonSlot, BenchPokemonSlot, DraggableCardDirective, DropZoneDirective],
  templateUrl: './game-page.html',
  styleUrl: './game-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GamePage implements OnInit, OnDestroy {
  private readonly route            = inject(ActivatedRoute);
  private readonly router           = inject(Router);
  private readonly authService      = inject(AuthService);
  private readonly gameStateService = inject(GameStateService);
  private readonly dragStateService = inject(DragStateService);
  readonly gameActionService        = inject(GameActionService);

  private gameId = '';

  // ── Local UI state ──────────────────────────────────────────────────────
  readonly loading      = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly selectedCard = signal<CardResponse | null>(null);

  /** Fallback board state from REST endpoint until WebSocket broadcasts arrive. */
  private readonly initialState = signal<GameStateResponse | null>(null);

  // Coin animation state
  readonly showCoinFlip = signal(false);
  readonly coinResult   = signal<'HEADS' | 'TAILS' | null>(null);

  // Surrender modal state
  readonly showSurrenderModal = signal(false);
  readonly surrendering       = signal(false);

  // Attack selection modal
  readonly showAttackModal  = signal(false);

  // Error feedback (action rejected by backend)
  readonly actionError = signal<string | null>(null);

  // Bench choice modal (after KO)
  readonly showBenchChoiceModal = signal(false);

  /**
   * Combined board state: starts from the public broadcast and merges in
   * any private player state updates (hand, prizes, deck contents, setup fields).
   */
  readonly boardState = computed<GameStateResponse | null>(() => {
    const pub  = this.gameActionService.boardState();
    const priv = this.gameActionService.privateStateUpdate();
    if (!pub) return this.initialState();

    const me = this.authService.currentUser();
    if (!me) return this.initialState();

    const ownPublic: PublicPlayerStateDTO =
      pub.player1State.playerId === me.id
        ? pub.player1State
        : pub.player2State;

    const opponentPublic: PublicPlayerStateDTO =
      pub.player1State.playerId === me.id
        ? pub.player2State
        : pub.player1State;

    const ownState: OwnPlayerState = priv
      ? {
          ...priv,
          playerName: ownPublic.playerName,
          cardBack:   ownPublic.cardBack,
          coin:       ownPublic.coin,
        } as OwnPlayerState
      : {
          playerId:            ownPublic.playerId,
          playerName:          ownPublic.playerName,
          cardBack:            ownPublic.cardBack,
          coin:                ownPublic.coin,
          active:              ownPublic.active,
          bench:               ownPublic.bench,
          hand:                [],
          deckCount:           ownPublic.deckCount,
          prizes:              [],
          discardPile:         ownPublic.discardPile,
          totalMulligans:      ownPublic.totalMulligans,
          mulliganBonusDraws:  ownPublic.mulliganBonusDraws,
          setupConfirmed:      ownPublic.setupConfirmed,
        };

    return {
      gameId:              pub.gameId,
      gameState:           pub.gameState,
      turnPhase:           pub.turnPhase,
      currentPlayerId:     pub.currentPlayerId,
      turnNumber:          pub.turnNumber,
      activeStadiumCardId: pub.activeStadiumCardId,
      turnFlags:           pub.turnFlags,
      ownState,
      opponentState: {
        playerId:            opponentPublic.playerId,
        playerName:          opponentPublic.playerName,
        cardBack:            opponentPublic.cardBack,
        coin:                opponentPublic.coin,
        active:              opponentPublic.active,
        bench:               opponentPublic.bench,
        cardsInHand:         opponentPublic.cardsInHand,
        deckCount:           opponentPublic.deckCount,
        prizesCount:         opponentPublic.prizesCount,
        discardPile:         opponentPublic.discardPile,
        totalMulligans:      opponentPublic.totalMulligans,
        mulliganBonusDraws:  opponentPublic.mulliganBonusDraws,
        setupConfirmed:      opponentPublic.setupConfirmed,
      },
    };
  });

  // ── Setup derived state ─────────────────────────────────────────────────

  /** True while gameState === 'SETUP'. */
  readonly isSetupPhase = computed(
    () => this.boardState()?.gameState === 'SETUP'
  );

  /** True if the player has no Basic Pokémon in hand and must mulligan. */
  readonly needsMulligan = computed(() => {
    const state = this.boardState();
    if (!state) return false;
    if (state.ownState.active) return false; // ya colocó activo, no puede mulligar
    const hand = state.ownState.hand ?? [];
    return hand.length > 0 && !hand.some(c => this.isBasicPokemon(c));
  });

  /** True when this player has bonus draws to accept and setup is not confirmed. */
  readonly bonusDrawPending = computed(() => {
    const state = this.boardState();
    if (!state) return false;
    return (
      (state.bonusDrawPending ?? false) &&
      (state.ownState.mulliganBonusDraws ?? 0) > 0 &&
      !(state.ownState.setupConfirmed ?? false)
    );
  });

  /** Max bonus draws available for this player. */
  readonly maxBonusDraws = computed(
    () => this.boardState()?.ownState.mulliganBonusDraws ?? 0
  );

  /** True when the player can confirm setup. */
  readonly canConfirmSetup = computed(() => {
    const state = this.boardState();
    if (!state) return false;
    return (
      !!state.ownState.active &&
      !(state.ownState.setupConfirmed ?? false) &&
      !this.needsMulligan()
    );
  });

  /** Setup status message shown in the header during SETUP. */
  readonly setupStatusMessage = computed(() => {
    const state = this.boardState();
    if (!state) return '';
    if (this.needsMulligan()) {
      return 'No Basic Pokémon in hand — declare a mulligan.';
    }
    if (this.bonusDrawPending()) {
      const opp = state.opponentState.totalMulligans ?? 0;
      return `Opponent had ${opp} mulligan(s) — choose bonus cards to draw.`;
    }
    if (state.ownState.setupConfirmed ?? false) {
      return 'Setup confirmed. Waiting for opponent…';
    }
    if (!state.ownState.active) {
      return 'Drag a Basic Pokémon from your hand to the Active slot.';
    }
    return 'Optionally add Pokémon to the Bench, then confirm your setup.';
  });

  /** Range array [0..maxBonusDraws] for bonus draw buttons. */
  readonly bonusDrawOptions = computed(() =>
    Array.from({ length: this.maxBonusDraws() + 1 }, (_, i) => i)
  );

  // ── Turn derived state ──────────────────────────────────────────────────

  /** True during the DRAW phase when it is the player's turn. */
  readonly isDrawPhase = computed(
    () => this.boardState()?.turnPhase === 'DRAW' && this.isMyTurn()
  );

  /** True during the MAIN phase when it is the player's turn. */
  readonly isMainPhase = computed(
    () => this.boardState()?.turnPhase === 'MAIN' && this.isMyTurn()
  );

  /**
   * True when the player can attach an energy this turn.
   * Requires MAIN phase, player's turn, and no energy attached yet this turn.
   */
  readonly canAttachEnergy = computed(() => {
    const state = this.boardState();
    if (!state) return false;
    return (
      this.isMainPhase() &&
      !(state.turnFlags?.energyAttachedThisTurn ?? false)
    );
  });

  /**
   * True when the player can declare an attack.
   * Requires MAIN phase, player's turn, and an Active Pokémon that can attack.
   */
  readonly canAttack = computed(() => {
    const state = this.boardState();
    if (!state) return false;
    return this.isMainPhase() && !!(state.ownState.active?.canAttack);
  });

  /**
   * Attacks available on the own Active Pokémon.
   * Empty if no Active Pokémon or no card data.
   */
  readonly availableAttacks = computed(() => {
    return this.boardState()?.ownState.active?.card?.attacks ?? [];
  });

  /**
   * True when this player must choose a bench Pokémon to replace their KO'd Active.
   * Blocks all other actions until resolved.
   */
  readonly mustChooseBench = computed(() => {
    const pub = this.gameActionService.boardState();
    const me  = this.authService.currentUser();
    if (!pub || !me) return false;
    return pub.pendingBenchChoicePlayerId === me.id;
  });

  /**
   * Bench Pokémon available to choose from after a KO.
   * Only relevant when mustChooseBench() is true.
   */
  readonly benchChoiceOptions = computed(() => {
    return this.boardState()?.ownState.bench ?? [];
  });

  /** True when the player can place a Basic Pokémon on the bench. */
  readonly canPlaceBasic = computed(() => {
    const state = this.boardState();
    if (!state) return false;
    return (
      this.isMainPhase() &&
      (state.ownState.bench ?? []).length < 5
    );
  });

  /** The card ID currently being dragged, for use in acceptDrop evaluations. */
  readonly draggedCardId = computed(
    () => this.dragStateService.draggedCardId() ?? ''
  );

  /** True when the player can evolve Pokémon (MAIN phase, player's turn). */
  readonly canEvolve = computed(() => this.isMainPhase());

  // ── Lifecycle ────────────────────────────────────────────────────────────
  constructor() {
    effect(() => {
      const event = this.gameActionService.lastEvent();
      if (event) this.handleGameEvent(event);
    });

    // Open bench choice modal when pendingBenchChoicePlayerId is set for this player
    effect(() => {
      if (this.mustChooseBench()) {
        this.showBenchChoiceModal.set(true);
      }
    });

    // Close bench choice modal when the requirement clears (e.g. timeout / auto-resolve)
    effect(() => {
      if (!this.mustChooseBench()) {
        this.showBenchChoiceModal.set(false);
      }
    });

    // Auto-send DRAW_CARD on turn 0 for the first player (no draw on first turn per rulebook)
    effect(() => {
      const state = this.boardState();
      const me    = this.authService.currentUser();
      if (!state || !me) return;

      const pub = this.gameActionService.boardState();
      if (!pub) return;

      const isFirstPlayer  = pub.firstPlayerId === me.id;
      const isDrawPhase    = state.turnPhase === 'DRAW';
      const isTurnZero     = state.turnNumber === 0;
      const isCurrentPlayer = state.currentPlayerId === me.id;

      if (isFirstPlayer && isDrawPhase && isTurnZero && isCurrentPlayer) {
        this.gameActionService.sendAction('DRAW_CARD', {});
      }
    });
  }

  ngOnInit(): void {
    this.gameId = this.route.snapshot.paramMap.get('id')!;
    this.loadInitialState();
    this.gameActionService.connect(this.gameId);
  }

  ngOnDestroy(): void {
    this.gameActionService.disconnect();
  }

  // ── Data loading ──────────────────────────────────────────────────────────
  private loadInitialState(): void {
    this.gameStateService.getState(this.gameId).subscribe({
      next: state => {
        this.initialState.set(state);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Could not load game state.');
        this.loading.set(false);
      },
    });
  }

  // ── Event handling ────────────────────────────────────────────────────────
  private handleGameEvent(
    event: { type: string; data: Record<string, unknown> }
  ): void {
    switch (event.type) {
      case 'GAME_OVER':
        this.router.navigate(['/lobby']);
        break;
      case 'COIN_FLIP':
        this.coinResult.set(event.data['result'] as 'HEADS' | 'TAILS');
        this.showCoinFlip.set(true);
        setTimeout(() => this.showCoinFlip.set(false), 3000);
        break;
      case 'POKEMON_KNOCKED_OUT':
        if (this.mustChooseBench()) {
          this.showBenchChoiceModal.set(true);
        }
        break;
      case 'TURN_ENDED':
        if (event.data['error']) {
          this.actionError.set(event.data['error'] as string);
          setTimeout(() => this.actionError.set(null), 3000);
        }
        break;
    }
  }

  // ── Setup actions ─────────────────────────────────────────────────────────

  /** Sends MULLIGAN_CONFIRM. */
  declareMulligan(): void {
    this.gameActionService.sendAction('MULLIGAN_CONFIRM', {});
  }

  /**
   * Called when a card is dropped onto the own Active slot during setup.
   * Validates the card is a Basic Pokémon before sending.
   */
  onSetupDropActive(cardId: string): void {
    const card = this.boardState()?.ownState.hand.find(c => c.id === cardId);
    if (!card || !this.isBasicPokemon(card)) return;
    this.gameActionService.sendAction('SETUP_PLACE_ACTIVE', { cardId });
  }

  /**
   * Called when a card is dropped onto a own Bench slot during setup.
   * Validates the card is a Basic Pokémon and the slot is empty.
   */
  onSetupDropBench(cardId: string, slotIndex: number): void {
    const state = this.boardState();
    if (!state) return;
    if ((state.ownState.bench ?? []).length >= 5) return;
    if (state.ownState.bench[slotIndex]) return;
    const card = state.ownState.hand.find(c => c.id === cardId);
    if (!card || !this.isBasicPokemon(card)) return;
    this.gameActionService.sendAction('SETUP_PLACE_BENCH', { cardId });
  }

  /** Sends CONFIRM_SETUP. */
  confirmSetup(): void {
    if (!this.canConfirmSetup()) return;
    this.gameActionService.sendAction('CONFIRM_SETUP', {});
  }

  /** Sends ACCEPT_MULLIGAN_BONUS with chosen card count. */
  acceptBonusDraws(cardsToDraw: number): void {
    const clamped = Math.max(0, Math.min(cardsToDraw, this.maxBonusDraws()));
    this.gameActionService.sendAction('ACCEPT_MULLIGAN_BONUS', { cardsToDraw: clamped });
  }

  // ── Turn actions ──────────────────────────────────────────────────────────

  /** Sends DRAW_CARD. Called when the player clicks their deck during DRAW phase. */
  drawCard(): void {
    if (!this.isDrawPhase()) return;
    this.gameActionService.sendAction('DRAW_CARD', {});
  }

  /**
   * Called when an energy card is dropped onto a Pokémon slot during MAIN phase.
   * @param cardId      The energy card ID from the hand.
   * @param targetInstanceId The instanceId of the target Pokémon.
   */
  onAttachEnergy(cardId: string, targetInstanceId: string): void {
    if (!this.canAttachEnergy()) {
      console.log('canAttachEnergy false');
      return;
    }
    const card = this.boardState()?.ownState.hand.find(c => c.id === cardId);
    console.log('card found:', card);
    if (!card) return;
    if (card.supertype !== 'ENERGY') {
      console.log('not energy, supertype:', card.supertype);
      return;
    }
    console.log('sending ATTACH_ENERGY', cardId, targetInstanceId);
    this.gameActionService.sendAction('ATTACH_ENERGY', { cardId, targetInstanceId });
    console.log('ATTACH_ENERGY payload:', { cardId, targetInstanceId });
  }

  /** Called when a Basic Pokémon is dropped onto an empty bench slot during MAIN phase. */
  onPlaceBasic(cardId: string): void {
    if (!this.canPlaceBasic()) return;
    const card = this.boardState()?.ownState.hand.find(c => c.id === cardId);
    if (!card || !this.isBasicPokemon(card)) return;
    this.gameActionService.sendAction('PLACE_BASIC_POKEMON', { cardId });
  }

  /**
   * Returns true if the given evolution card can be played onto the target Pokémon.
   * Used to control which slots accept the drop.
   */
  canEvolveTarget(
    evolutionCardId: string,
    targetCard: CardResponse | null,
    enteredThisTurn: boolean
  ): boolean {
    if (!this.canEvolve()) return false;
    if (!targetCard) return false;
    if (enteredThisTurn) return false;

    const evolutionCard = this.boardState()?.ownState.hand.find(
      c => c.id === evolutionCardId
    );
    if (!evolutionCard) return false;
    if (!evolutionCard.evolvesFrom) return false;

    return targetCard.name === evolutionCard.evolvesFrom;
  }

  /**
   * Called when an evolution card is dropped onto a Pokémon slot.
   * Validates the evolution is legal before sending the action.
   */
  onEvolve(
    cardId: string,
    targetInstanceId: string,
    targetCard: CardResponse | null,
    enteredThisTurn: boolean
  ): void {
    if (!this.canEvolveTarget(cardId, targetCard, enteredThisTurn)) return;
    this.gameActionService.sendAction('EVOLVE_POKEMON', {
      cardId,
      targetInstanceId,
    });
  }

  /** Opens the attack selection modal. */
  openAttackModal(): void {
    if (!this.canAttack()) return;
    this.showAttackModal.set(true);
  }

  /** Closes the attack selection modal without acting. */
  closeAttackModal(): void {
    this.showAttackModal.set(false);
  }

  /**
   * Sends DECLARE_ATTACK with the chosen attack name.
   * Closes the modal immediately — backend will validate.
   */
  declareAttack(attackName: string): void {
    this.gameActionService.sendAction('DECLARE_ATTACK', { attackName });
    this.showAttackModal.set(false);
  }

  /** Sends END_TURN. */
  endTurn(): void {
    if (!this.isMainPhase()) return;
    this.gameActionService.sendAction('END_TURN', {});
  }

  /** Opens the bench choice modal when a KO requires it. */
  openBenchChoiceModal(): void {
    this.showBenchChoiceModal.set(true);
  }

  /** Sends CHOOSE_BENCH_POKEMON with the selected instanceId. */
  chooseBenchPokemon(instanceId: string): void {
    this.gameActionService.sendAction('CHOOSE_BENCH_POKEMON', { instanceId });
    this.showBenchChoiceModal.set(false);
  }

  // ── Card detail modal ─────────────────────────────────────────────────────
  openCardDetail(card: CardResponse | null): void {
    if (!card) return;
    this.selectedCard.set(card);
  }

  closeCardDetail(): void {
    this.selectedCard.set(null);
  }

  // ── Helpers ───────────────────────────────────────────────────────────────
  isMyTurn(): boolean {
    const me = this.authService.currentUser();
    return this.boardState()?.currentPlayerId === me?.id;
  }

  /** Returns bench slots as fixed array of 5, filling missing slots with null. */
  getBenchSlots<T>(bench: T[]): (T | null)[] {
    return Array.from({ length: 5 }, (_, i) => bench[i] ?? null);
  }

  /** Returns the top card of a discard pile, or null if empty. */
  getDiscardTop(pile: CardResponse[]): CardResponse | null {
    return pile.length > 0 ? pile[pile.length - 1] : null;
  }

  /** Returns prize slots as fixed array of 6. */
  getPrizeSlots(prizes: CardResponse[]): (CardResponse | null)[] {
    return Array.from({ length: 6 }, (_, i) => prizes[i] ?? null);
  }

  coinImageSrc(): string {
    return this.coinResult() === 'HEADS'
      ? 'assets/coin/defaultCoinHead.png'
      : 'assets/coin/defaultCoinTail.png';
  }

  openSurrenderModal(): void {
    this.showSurrenderModal.set(true);
  }

  closeSurrenderModal(): void {
    this.showSurrenderModal.set(false);
  }

  confirmSurrender(): void {
    this.surrendering.set(true);
    this.gameStateService.surrender(this.gameId).subscribe({
      next: () => {
        this.surrendering.set(false);
        this.showSurrenderModal.set(false);
        this.router.navigate(['/lobby']);
      },
      error: () => {
        this.surrendering.set(false);
        this.showSurrenderModal.set(false);
      },
    });
  }

  /**
   * Returns true if the card is a Basic Pokémon.
   * Used to validate drag sources during setup.
   */
  isBasicPokemon(card: CardResponse): boolean {
    return card.supertype === 'POKEMON' && (card.subtypes ?? []).includes('Basic');
  }

  isEvolutionCard(card: CardResponse): boolean {
    if (card.supertype !== 'POKEMON') return false;
    const subtypes = card.subtypes ?? [];
    return !subtypes.includes('Basic');
  }

  /** Resolves the asset path for a card back skin. */
  getCardBackUrl(skin: string): string {
    const map: Record<string, string> = {
      DEFAULT:    'assets/cardBack/defaultBack.png',
      PIKACHU:    'assets/cardBack/pikachuBack.png',
      BULBASAUR:  'assets/cardBack/bulbasaurBack.png',
      CHARMANDER: 'assets/cardBack/charmanderBack.png',
      SQUIRTLE:   'assets/cardBack/squirtleBack.png',
    };
    return map[skin] ?? map['DEFAULT'];
  }
}
