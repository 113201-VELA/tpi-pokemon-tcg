import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import {
  AddCardRequest,
  CreateDeckRequest,
  DeckResponse,
  DeckValidationResult,
  UpdateDeckRequest
} from '../../domain/models/deck.models';

@Injectable({ providedIn: 'root' })
export class DeckService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/decks`;

  listDecks(): Observable<DeckResponse[]> {
    return this.http.get<DeckResponse[]>(this.apiUrl);
  }

  createDeck(request: CreateDeckRequest): Observable<DeckResponse> {
    return this.http.post<DeckResponse>(this.apiUrl, request);
  }

  addCard(deckId: string, request: AddCardRequest): Observable<DeckResponse> {
    return this.http.post<DeckResponse>(`${this.apiUrl}/${deckId}/cards`, request);
  }

  updateCardQuantity(deckId: string, cardId: string, quantity: number): Observable<DeckResponse> {
    return this.http.put<DeckResponse>(
      `${this.apiUrl}/${deckId}/cards/${cardId}`,
      { quantity }
    );
  }

  removeCard(deckId: string, cardId: string): Observable<DeckResponse> {
    return this.http.delete<DeckResponse>(`${this.apiUrl}/${deckId}/cards/${cardId}`);
  }

  deleteDeck(deckId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${deckId}`);
  }

  updateDeck(deckId: string, request: UpdateDeckRequest): Observable<DeckResponse> {
    return this.http.put<DeckResponse>(`${this.apiUrl}/${deckId}`, request);
  }

  validateDeck(deckId: string): Observable<DeckValidationResult> {
    return this.http.post<DeckValidationResult>(`${this.apiUrl}/${deckId}/validate`, {});
  }
}
