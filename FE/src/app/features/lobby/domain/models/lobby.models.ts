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

export interface JoinGameRequest {
  deckId: string;
}

// Shape returned by createGame and joinGame (backend returns Game entity)
export interface GameCreatedResponse {
  id: string;
  state: GameState;
}

