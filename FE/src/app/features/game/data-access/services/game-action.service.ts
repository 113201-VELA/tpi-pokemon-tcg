import { inject, Injectable, signal } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import { AuthService } from '../../../auth/data-access/services/auth.service';
import { environment } from '../../../../../environments/environment';
import {
  GameAction,
  GameActionType,
  GameEvent,
  OwnPlayerState,
  PublicBoardStateDTO,
} from '../../domain/models/game.models';

/**
 * Manages the WebSocket connection for an active game session.
 *
 * Responsibilities:
 * - Create and activate the STOMP client.
 * - Subscribe to public board state, private player state, and game events.
 * - Expose reactive signals that game-page and setup components can read.
 * - Send game actions to /app/game/{gameId}/action.
 * - Tear down the connection on disconnect.
 *
 * Usage:
 *   1. Call connect(gameId) when entering the game page.
 *   2. Read boardState(), pendingEvent(), privateStateUpdate() as signals.
 *   3. Call sendAction(type, payload) to send any game action.
 *   4. Call disconnect() on ngOnDestroy.
 */
@Injectable({ providedIn: 'root' })
export class GameActionService {
  private readonly authService = inject(AuthService);

  private stompClient?: Client;
  private gameId = '';

  private eventQueue: GameEvent[] = [];
  private processingQueue = false;

  // ── Signals exposed to consumers ────────────────────────────────────────

  /**
   * Latest public board state broadcast received from
   * /topic/game/{gameId}/state. Null until first message arrives.
   */
  readonly boardState = signal<PublicBoardStateDTO | null>(null);

  /**
   * Latest game event received from /topic/game/{gameId}/events.
   * Null until first event arrives. Consumers should react to changes.
   */
  readonly lastEvent = signal<GameEvent | null>(null);

  /**
   * Latest private player state received from
   * /user/queue/game/{gameId}/player. Null until first message arrives.
   */
  readonly privateStateUpdate = signal<Partial<OwnPlayerState> | null>(null);

  /** True while the STOMP connection is being established. */
  readonly connecting = signal(true);

  /** True once the STOMP connection is active and subscriptions are set up. */
  readonly connected = signal(false);

  // ── Connection lifecycle ─────────────────────────────────────────────────

  /**
   * Creates and activates the STOMP client for the given game.
   * Safe to call multiple times — disconnects any existing connection first.
   */
  connect(gameId: string): void {
    if (this.stompClient?.active) {
      this.stompClient.deactivate();
    }

    this.gameId = gameId;
    this.connecting.set(true);
    this.connected.set(false);

    const token  = this.authService.getToken();
    const wsBase = environment.wsUrl.replace(/^http/, 'ws');

    this.stompClient = new Client({
      brokerURL: wsBase,
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      reconnectDelay: 5000,
      onConnect: () => {
        this.connecting.set(false);
        this.connected.set(true);
        this.setupSubscriptions();
      },
      onDisconnect: () => {
        this.connected.set(false);
      },
      onStompError: () => {
        this.connecting.set(false);
        this.connected.set(false);
      },
    });

    this.stompClient.activate();
  }

  /** Deactivates the STOMP client and resets all signals. */
  disconnect(): void {
    this.stompClient?.deactivate();
    this.stompClient = undefined;
    this.gameId = '';
    this.boardState.set(null);
    this.lastEvent.set(null);
    this.privateStateUpdate.set(null);
    this.eventQueue = [];
    this.processingQueue = false;
    this.connecting.set(false);
    this.connected.set(false);
  }

  // ── Action sending ───────────────────────────────────────────────────────

  /**
   * Sends a game action to /app/game/{gameId}/action.
   * No-op if the client is not connected.
   *
   * @param type    The action type (e.g. 'SETUP_PLACE_ACTIVE').
   * @param payload Action-specific payload fields.
   */
  sendAction(type: GameActionType, payload: Record<string, unknown> = {}): void {
    if (!this.stompClient?.connected) return;

    const action: GameAction = { type, payload };

    this.stompClient.publish({
      destination: `/app/game/${this.gameId}/action`,
      body: JSON.stringify(action),
    });
  }

  // ── Event queue ───────────────────────────────────────────────────────────

  /**
   * Retorna el delay en ms a esperar después de procesar un evento antes
   * de procesar el siguiente. Los eventos con animaciones necesitan tiempo
   * para que no se superpongan.
   */
  private getEventDelay(event: GameEvent): number {
    switch (event.type) {
      case 'COIN_FLIP': return 3500;
      default: return 0;
    }
  }

  /** Agrega un evento a la cola y arranca el procesamiento si no está corriendo. */
  private enqueueEvent(event: GameEvent): void {
    this.eventQueue.push(event);
    if (!this.processingQueue) {
      this.processNextEvent();
    }
  }

  /** Procesa eventos uno por uno, respetando los delays por tipo de evento. */
  private processNextEvent(): void {
    if (this.eventQueue.length === 0) {
      this.processingQueue = false;
      return;
    }
    this.processingQueue = true;
    const event = this.eventQueue.shift()!;
    this.lastEvent.set(event);
    setTimeout(() => this.processNextEvent(), this.getEventDelay(event));
  }

  // ── Private helpers ──────────────────────────────────────────────────────

  /** Sets up the three STOMP subscriptions for the current game. */
  private setupSubscriptions(): void {
    // Public board state — received by both players after every action
    this.stompClient!.subscribe(
      `/topic/game/${this.gameId}/state`,
      (message: IMessage) => {
        try {
          const state: PublicBoardStateDTO = JSON.parse(message.body);
          console.log('PUBLIC STATE pendingPrizeTake:', state.pendingPrizeTakePlayerId, state.pendingPrizeTakeCount);
          this.boardState.set(state);
        } catch {
          // Ignore malformed messages
        }
      }
    );

    // Private player state — received only by this player
    // Contains hand, prizes, deck contents and setup fields
    this.stompClient!.subscribe(
      `/user/queue/game/${this.gameId}/player`,
      (message: IMessage) => {
        try {
          const partial: Partial<OwnPlayerState> = JSON.parse(message.body);
          console.log('PRIVATE STATE:', partial);
          this.privateStateUpdate.set(partial);
        } catch {
          // Ignore malformed messages
        }
      }
    );

    // Game events — knockouts, coin flips, prizes, conditions, etc.
    this.stompClient!.subscribe(
      `/topic/game/${this.gameId}/events`,
      (message: IMessage) => {
        console.log('RAW EVENT:', message.body);
        try {
          const event: GameEvent = JSON.parse(message.body);
          this.enqueueEvent(event);
        } catch {
          // Ignore malformed messages
        }
      }
    );
  }
}
