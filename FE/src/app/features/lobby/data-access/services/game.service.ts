import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
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
}
