export type GameState = 'WAITING' | 'SETUP' | 'ACTIVE' | 'FINISHED';

export interface GameResponseDTO {
  id: string;
  state: GameState;
  creatorUsername: string;
  playerCount: number;
  createdAt: string;
}

export interface CreateGameRequest {
  deckId: string;
}
