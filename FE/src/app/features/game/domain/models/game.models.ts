import { CardResponse } from '../../../deck-builder/domain/models/card.models';

export type GameState  = 'WAITING' | 'SETUP' | 'ACTIVE' | 'FINISHED' | 'CANCELLED';
export type TurnPhase  = 'DRAW' | 'MAIN' | 'ATTACK' | 'BETWEEN_TURNS' | 'SETUP';
export type SpecialCondition =
  'ASLEEP' | 'BURNED' | 'CONFUSED' | 'PARALYZED' | 'POISONED';

export interface TurnFlags {
  energyAttachedThisTurn: boolean;
  retreatedThisTurn: boolean;
  supporterPlayedThisTurn: boolean;
  stadiumPlayedThisTurn: boolean;
  attackedThisTurn: boolean;
}

export interface ActivePokemonDTO {
  instanceId: string;
  cardId: string;
  card: CardResponse | null;
  attachedEnergies: CardResponse[];
  attachedTool: CardResponse | null;
  evolutionStack: CardResponse[];
  damageCounters: number;
  currentHp: number;
  maxHp: number;
  conditions: SpecialCondition[];
  enteredThisTurn: boolean;
  canAttack: boolean;
  canRetreat: boolean;
}

export interface BenchPokemonDTO {
  instanceId: string;
  cardId: string;
  card: CardResponse | null;
  attachedEnergies: CardResponse[];
  attachedTool: CardResponse | null;
  evolutionStack: CardResponse[];
  damageCounters: number;
  currentHp: number;
  maxHp: number;
  enteredThisTurn: boolean;
}

export interface OwnPlayerState {
  playerId: string;
  playerName: string;
  cardBack: string;
  coin: string;
  active: ActivePokemonDTO | null;
  bench: BenchPokemonDTO[];
  hand: CardResponse[];
  deckCount: number;
  prizes: CardResponse[];
  discardPile: CardResponse[];
  // ── Setup fields (present when gameState === 'SETUP') ──────────────
  totalMulligans?: number;
  mulliganBonusDraws?: number;
  setupConfirmed?: boolean;
}

export interface OpponentPlayerState {
  playerId: string;
  playerName: string;
  cardBack: string;
  coin: string;
  active: ActivePokemonDTO | null;
  bench: BenchPokemonDTO[];
  cardsInHand: number;
  deckCount: number;
  prizesCount: number;
  discardPile: CardResponse[];
  // ── Setup fields (present when gameState === 'SETUP') ──────────────
  totalMulligans?: number;
  mulliganBonusDraws?: number;
  setupConfirmed?: boolean;
}

/** Setup-phase info visible about the opponent (public state). */
export interface OpponentSetupState {
  totalMulligans: number;
  mulliganBonusDraws: number;
  setupConfirmed: boolean;
}

export interface GameStateResponse {
  gameId: string;
  gameState: GameState;
  turnPhase: TurnPhase;
  currentPlayerId: string;
  turnNumber: number;
  activeStadiumCardId: string | null;
  turnFlags: TurnFlags;
  ownState: OwnPlayerState;
  opponentState: OpponentPlayerState;
  // ── Setup field ────────────────────────────────────────────────────
  bonusDrawPending?: boolean;
}

// ─────────────────────────────────────────────────────────────────────────────
// Public DTOs emitted by WebSocket (FE Spec 19)
// ─────────────────────────────────────────────────────────────────────────────

export interface PublicPlayerStateDTO {
  playerId: string;
  playerName: string;
  cardBack: string;
  coin: string;
  active: ActivePokemonDTO | null;
  bench: BenchPokemonDTO[];
  deckCount: number;
  discardPile: CardResponse[];
  cardsInHand: number;
  prizesCount: number;
  totalMulligans?: number;
  mulliganBonusDraws?: number;
  setupConfirmed?: boolean;
}

export interface PublicBoardStateDTO {
  gameId: string;
  gameState: GameState;
  turnPhase: TurnPhase;
  currentPlayerId: string;
  turnNumber: number;
  activeStadiumCardId: string | null;
  turnFlags: TurnFlags;
  bonusDrawPending: boolean;
  pendingBenchChoicePlayerId: string | null;
  firstPlayerId: string | null;
  pendingBonusPlacement: string[];
  player1State: PublicPlayerStateDTO;
  player2State: PublicPlayerStateDTO;
}

// ─────────────────────────────────────────────────────────────────────────────
// WebSocket action types
// ─────────────────────────────────────────────────────────────────────────────

/** All action types the client can send to /app/game/{id}/action. */
export type GameActionType =
  // Setup
  | 'MULLIGAN_CONFIRM'
  | 'SETUP_PLACE_ACTIVE'
  | 'SETUP_PLACE_BENCH'
  | 'ACCEPT_MULLIGAN_BONUS'
  | 'CONFIRM_SETUP'
  | 'CONFIRM_BONUS_PLACEMENT'
  // Draw
  | 'DRAW_CARD'
  // Main phase
  | 'PLACE_BASIC_POKEMON'
  | 'ATTACH_ENERGY'
  | 'PLAY_TRAINER'
  | 'EVOLVE_POKEMON'
  | 'RETREAT'
  | 'DECLARE_ATTACK'
  | 'END_TURN'
  // Post-KO
  | 'CHOOSE_BENCH_POKEMON';

/** Generic action payload sent over WebSocket. */
export interface GameAction {
  type: GameActionType;
  payload: Record<string, unknown>;
}

/** Game event received from /topic/game/{id}/events. */
export interface GameEvent {
  type: string;
  gameId: string;
  playerId: string;
  turnNumber: number;
  data: Record<string, unknown>;
  occurredAt: number;
}

/** Maps energy type strings to display icons. */
export const ENERGY_ICONS: Record<string, string> = {
  GRASS:     '🌿',
  FIRE:      '🔥',
  WATER:     '💧',
  LIGHTNING: '⚡',
  PSYCHIC:   '🔮',
  FIGHTING:  '👊',
  DARKNESS:  '🌑',
  METAL:     '⚙️',
  FAIRY:     '✨',
  DRAGON:    '🐉',
  COLORLESS: '⭐',
};

/** Display config for each special condition. */
export const CONDITION_CONFIG: Record<SpecialCondition, { label: string; color: string }> = {
  ASLEEP:    { label: 'ASLEEP',    color: '#6366f1' },
  BURNED:    { label: 'BURNED',    color: '#f97316' },
  CONFUSED:  { label: 'CONFUSED',  color: '#ec4899' },
  PARALYZED: { label: 'PARALYZED', color: '#eab308' },
  POISONED:  { label: 'POISONED',  color: '#a855f7' },
};

/** Groups attached energies by type for display. */
export function groupEnergies(
  energies: CardResponse[]
): { type: string; icon: string; count: number }[] {
  const groups: Record<string, number> = {};
  for (const e of energies) {
    const type = e.types?.[0] ?? 'COLORLESS';
    groups[type] = (groups[type] ?? 0) + 1;
  }
  return Object.entries(groups).map(([type, count]) => ({
    type,
    icon: ENERGY_ICONS[type] ?? '⭐',
    count,
  }));
}

/** Calculates HP percentage for a Pokémon. */
export function getHpPercent(currentHp: number, maxHp: number): number {
  if (!maxHp) return 0;
  return Math.round((currentHp / maxHp) * 100);
}

/** Returns CSS class based on HP percentage. */
export function getHpClass(percent: number): string {
  if (percent > 50) return 'hp--high';
  if (percent > 25) return 'hp--mid';
  return 'hp--low';
}
