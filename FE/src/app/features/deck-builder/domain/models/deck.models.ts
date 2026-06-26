import { CardResponse } from './card.models';

export interface UpdateDeckRequest {
  name?: string;
  cardBack?: string;
  coin?: string;
  featuredCardId?: string;   // empty string to clear, card ID to set
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
  featuredCardId: string | null;
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
