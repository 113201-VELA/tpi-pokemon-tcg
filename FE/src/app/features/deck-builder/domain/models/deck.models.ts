import { CardResponse } from './card.models';

export interface CreateDeckRequest {
  name: string;
  description?: string;
}

export interface AddCardRequest {
  cardId: string;
  quantity: number;
}

export interface DeckCardResponse {
  id: string;
  card: CardResponse;
  quantity: number;
}

export interface DeckResponse {
  id: string;
  name: string;
  description?: string;
  valid: boolean;
  cards: DeckCardResponse[];
  totalCardCount: number;
}

export interface DeckValidationResult {
  valid: boolean;
  totalCards: number;
  exactly60: boolean;
  noExcessCopies: boolean;
  hasBasicPokemon: boolean;
}
