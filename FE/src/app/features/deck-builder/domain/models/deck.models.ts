import { CardResponse } from './card.models';

export interface UpdateDeckRequest {
  name?: string;
  cardBack?: string;
  coin?: string;
}

export interface CreateDeckRequest {
  name: string;
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
  cardBack: string;
  coin: string;
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
