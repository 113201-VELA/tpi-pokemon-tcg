import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../../../environments/environment';
import {
  CreateGameRequest,
  GameCreatedResponse,
  GameResponseDTO,
  JoinGameRequest
} from '../../domain/models/lobby.models';

@Injectable({ providedIn: 'root' })
export class GameService {
  private readonly http   = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/games`;

  /** Returns all games currently in WAITING state. */
  listOpenGames(): Observable<GameResponseDTO[]> {
    return this.http.get<GameResponseDTO[]>(this.apiUrl);
  }

  /** Creates a new game and returns the created Game entity. */
  createGame(request: CreateGameRequest): Observable<GameCreatedResponse> {
    return this.http.post<GameCreatedResponse>(this.apiUrl, request);
  }

  /** Joins an existing WAITING game as player 2. */
  joinGame(gameId: string, request: JoinGameRequest): Observable<GameCreatedResponse> {
    return this.http.post<GameCreatedResponse>(`${this.apiUrl}/${gameId}/join`, request);
  }

  /** Cancels a WAITING game. Only the creator can call this. */
  cancelGame(gameId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${gameId}`);
  }

  /**
   * Returns the authenticated player's current active game, if any.
   * Returns null if the server responds with 204 No Content.
   */
  getActiveGame(): Observable<GameCreatedResponse | null> {
    return this.http.get<GameCreatedResponse>(
      `${this.apiUrl}/my-active-game`,
      { observe: 'response' }
    ).pipe(
      map(response => response.status === 204 ? null : response.body)
    );
  }
}
