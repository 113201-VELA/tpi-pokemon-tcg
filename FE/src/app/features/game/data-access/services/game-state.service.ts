import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import { GameStateResponse } from '../../domain/models/game.models';

@Injectable({ providedIn: 'root' })
export class GameStateService {
  private readonly http   = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/games`;

  /** Returns the current board state for the authenticated player. */
  getState(gameId: string): Observable<GameStateResponse> {
    return this.http.get<GameStateResponse>(`${this.apiUrl}/${gameId}/state`);
  }

  /** Surrenders the current game. The opponent is declared the winner. */
  surrender(gameId: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${gameId}/surrender`, {});
  }
}
