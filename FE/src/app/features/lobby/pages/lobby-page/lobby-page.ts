import {
  ChangeDetectionStrategy,
  Component,
  inject,
  OnDestroy,
  OnInit,
  signal,
} from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { Client } from '@stomp/stompjs';
import { DeckService } from '../../../deck-builder/data-access/services/deck.service';
import { GameService } from '../../data-access/services/game.service';
import { AuthService } from '../../../auth/data-access/services/auth.service';
import { DeckResponse } from '../../../deck-builder/domain/models/deck.models';
import { GameResponseDTO } from '../../domain/models/lobby.models';
import { environment } from '../../../../../environments/environment';

@Component({
  selector: 'app-lobby-page',
  templateUrl: './lobby-page.html',
  styleUrl: './lobby-page.css',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LobbyPage implements OnInit, OnDestroy {
  private readonly router      = inject(Router);
  private readonly deckService = inject(DeckService);
  private readonly gameService = inject(GameService);
  private readonly authService = inject(AuthService);

  private stompClient?: Client;

  // ── State ──────────────────────────────────────────────────────────────
  readonly validDecks       = signal<DeckResponse[]>([]);
  readonly openGames        = signal<GameResponseDTO[]>([]);
  readonly selectedDeckId   = signal<string | null>(null);

  readonly loadingDecks     = signal(false);
  readonly loadingGames     = signal(false);
  readonly creating         = signal(false);
  readonly joiningGameId    = signal<string | null>(null);
  readonly cancellingGameId = signal<string | null>(null);

  readonly errorMessage     = signal<string | null>(null);
  readonly successMessage   = signal<string | null>(null);

  readonly currentUsername  = this.authService.currentUser()?.username ?? '';

  // ── Lifecycle ───────────────────────────────────────────────────────────
  ngOnInit(): void {
    this.loadValidDecks();
    this.loadOpenGames();
    this.connectWebSocket();
  }

  ngOnDestroy(): void {
    this.stompClient?.deactivate();
  }

  // ── Data loading ────────────────────────────────────────────────────────
  private loadValidDecks(): void {
    this.loadingDecks.set(true);
    this.deckService.listDecks().subscribe({
      next: decks => {
        this.validDecks.set(decks.filter(d => d.valid));
        this.loadingDecks.set(false);
      },
      error: () => {
        this.errorMessage.set('Could not load decks.');
        this.loadingDecks.set(false);
      },
    });
  }

  loadOpenGames(): void {
    this.loadingGames.set(true);
    this.gameService.listOpenGames().subscribe({
      next: games => {
        this.openGames.set(games);
        this.loadingGames.set(false);
      },
      error: () => {
        this.errorMessage.set('Could not load open games.');
        this.loadingGames.set(false);
      },
    });
  }

  // ── WebSocket ───────────────────────────────────────────────────────────
  private connectWebSocket(): void {
    const token = this.authService.getToken();

    // Derive WebSocket URL from the configured API base URL
    const wsBase = environment.wsUrl.replace(/^http/, 'ws');

    this.stompClient = new Client({
      brokerURL: `${wsBase}/ws`,
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      reconnectDelay: 5000,
      onConnect: () => {
        // Subscribe to lobby topic for real-time game list updates
        this.stompClient!.subscribe('/topic/lobby', () => {
          this.loadOpenGames();
        });
      },
    });

    this.stompClient.activate();
  }

  // ── Actions ─────────────────────────────────────────────────────────────
  selectDeck(deckId: string): void {
    this.selectedDeckId.set(
      this.selectedDeckId() === deckId ? null : deckId
    );
    this.clearMessages();
  }

  createGame(): void {
    const deckId = this.selectedDeckId();
    if (!deckId) return;

    this.creating.set(true);
    this.clearMessages();

    this.gameService.createGame({ deckId }).subscribe({
      next: () => {
        this.creating.set(false);
        this.successMessage.set('Game created. Waiting for an opponent...');
        this.loadOpenGames();
      },
      error: (err: HttpErrorResponse) => {
        this.creating.set(false);
        this.errorMessage.set(err.error?.message ?? 'Could not create game.');
      },
    });
  }

  joinGame(gameId: string): void {
    const deckId = this.selectedDeckId();
    if (!deckId) return;

    this.joiningGameId.set(gameId);
    this.clearMessages();

    this.gameService.joinGame(gameId, { deckId }).subscribe({
      next: game => {
        this.joiningGameId.set(null);
        this.router.navigate(['/game', game.id]);
      },
      error: (err: HttpErrorResponse) => {
        this.joiningGameId.set(null);
        this.errorMessage.set(err.error?.message ?? 'Could not join game.');
      },
    });
  }

  cancelGame(event: Event, gameId: string): void {
    event.stopPropagation();
    this.cancellingGameId.set(gameId);
    this.clearMessages();

    this.gameService.cancelGame(gameId).subscribe({
      next: () => {
        this.cancellingGameId.set(null);
        this.openGames.update(games => games.filter(g => g.id !== gameId));
      },
      error: (err: HttpErrorResponse) => {
        this.cancellingGameId.set(null);
        this.errorMessage.set(err.error?.message ?? 'Could not cancel game.');
      },
    });
  }

  // ── Helpers ─────────────────────────────────────────────────────────────
  private clearMessages(): void {
    this.errorMessage.set(null);
    this.successMessage.set(null);
  }

  get canAct(): boolean {
    return this.selectedDeckId() !== null;
  }

  isOwnGame(game: GameResponseDTO): boolean {
    return game.creatorUsername === this.currentUsername;
  }

  getFeaturedCardImage(deck: DeckResponse): string | null {
    if (!deck.featuredCardId) return null;
    const entry = deck.cards.find(c => c.card.id === deck.featuredCardId);
    return entry?.card.imageSmall ?? null;
  }

  formatTime(iso: string): string {
    return new Date(iso).toLocaleTimeString('en-US', {
      hour: '2-digit',
      minute: '2-digit',
    });
  }
}
