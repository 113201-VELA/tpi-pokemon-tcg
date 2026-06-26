import {
  ChangeDetectionStrategy,
  Component,
  inject,
  OnDestroy,
  OnInit,
  signal,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Client } from '@stomp/stompjs';
import { AuthService } from '../../../auth/data-access/services/auth.service';
import { GameStateService } from '../../data-access/services/game-state.service';
import {
  GameStateResponse,
  SpecialCondition,
} from '../../domain/models/game.models';
import { CardResponse } from '../../../deck-builder/domain/models/card.models';
import { environment } from '../../../../../environments/environment';
import { ActivePokemonSlot } from '../../components/active-pokemon-slot/active-pokemon-slot';
import { BenchPokemonSlot } from '../../components/bench-pokemon-slot/bench-pokemon-slot';

@Component({
  selector: 'app-game-page',
  imports: [ActivePokemonSlot, BenchPokemonSlot],
  templateUrl: './game-page.html',
  styleUrl: './game-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GamePage implements OnInit, OnDestroy {
  private readonly route            = inject(ActivatedRoute);
  private readonly router           = inject(Router);
  private readonly authService      = inject(AuthService);
  private readonly gameStateService = inject(GameStateService);

  private stompClient?: Client;
  private gameId = '';

  // ── State ──────────────────────────────────────────────────────────────
  readonly boardState   = signal<GameStateResponse | null>(null);
  readonly selectedCard = signal<CardResponse | null>(null);
  readonly loading      = signal(true);
  readonly errorMessage = signal<string | null>(null);

  // Coin animation state
  readonly showCoinFlip = signal(false);
  readonly coinResult   = signal<'HEADS' | 'TAILS' | null>(null);

  // ── Lifecycle ───────────────────────────────────────────────────────────
  ngOnInit(): void {
    this.gameId = this.route.snapshot.paramMap.get('id')!;
    this.loadInitialState();
    this.connectWebSocket();
  }

  ngOnDestroy(): void {
    this.stompClient?.deactivate();
  }

  // ── Data loading ────────────────────────────────────────────────────────
  private loadInitialState(): void {
    this.gameStateService.getState(this.gameId).subscribe({
      next: state => {
        this.boardState.set(state);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set('Could not load game state.');
        this.loading.set(false);
      },
    });
  }

  // ── WebSocket ───────────────────────────────────────────────────────────
  private connectWebSocket(): void {
    const token  = this.authService.getToken();
    const wsBase = environment.wsUrl.replace(/^http/, 'ws');

    this.stompClient = new Client({
      brokerURL: `${wsBase}/ws`,
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      reconnectDelay: 5000,
      onConnect: () => {
        // Subscribe to public board state broadcast
        this.stompClient!.subscribe(
          `/topic/game/${this.gameId}/state`,
          message => {
            try {
              const state: GameStateResponse = JSON.parse(message.body);
              this.boardState.set(state);
            } catch {
              // Ignore malformed messages
            }
          }
        );

        // Subscribe to private player state (own hand, prizes, etc.)
        this.stompClient!.subscribe(
          `/user/queue/game/${this.gameId}/player`,
          message => {
            try {
              const privateState = JSON.parse(message.body);
              this.boardState.update(prev =>
                prev ? { ...prev, ownState: { ...prev.ownState, ...privateState } } : prev
              );
            } catch {
              // Ignore malformed messages
            }
          }
        );

        // Subscribe to game events (knockouts, prizes, coin flips, etc.)
        this.stompClient!.subscribe(
          `/topic/game/${this.gameId}/events`,
          message => {
            try {
              const event = JSON.parse(message.body);
              this.handleGameEvent(event);
            } catch {
              // Ignore malformed messages
            }
          }
        );
      },
    });

    this.stompClient.activate();
  }

  private handleGameEvent(
    event: { type: string; data: Record<string, unknown> }
  ): void {
    switch (event.type) {
      case 'GAME_OVER':
        // Navigate back to lobby when game ends
        this.router.navigate(['/lobby']);
        break;
      case 'COIN_FLIP':
        // Show coin flip animation for 3 seconds
        this.coinResult.set(event.data['result'] as 'HEADS' | 'TAILS');
        this.showCoinFlip.set(true);
        setTimeout(() => this.showCoinFlip.set(false), 3000);
        break;
    }
  }

  // ── Card detail modal ────────────────────────────────────────────────────
  openCardDetail(card: CardResponse | null): void {
    if (!card) return;
    this.selectedCard.set(card);
  }

  closeCardDetail(): void {
    this.selectedCard.set(null);
  }

  // ── Helpers ──────────────────────────────────────────────────────────────
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

  coinImageSrc(): string {
    return this.coinResult() === 'HEADS'
      ? 'assets/coin/defaultCoinHead.png'
      : 'assets/coin/defaultCoinTail.png';
  }

  goBackToLobby(): void {
    this.router.navigate(['/lobby']);
  }
}
