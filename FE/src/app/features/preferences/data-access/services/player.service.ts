import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import {
  UpdateNicknameRequest,
  UpdateNicknameResponse,
  UpdatePasswordRequest
} from '../../domain/models/preferences.models';

@Injectable({ providedIn: 'root' })
export class PlayerService {
  private readonly http    = inject(HttpClient);
  private readonly apiUrl  = `${environment.apiUrl}/players`;

  /** Updates the nickname of the authenticated player. */
  updateNickname(request: UpdateNicknameRequest): Observable<UpdateNicknameResponse> {
    return this.http.put<UpdateNicknameResponse>(`${this.apiUrl}/me/nickname`, request);
  }

  /** Updates the password of the authenticated player. Returns 204 no content. */
  updatePassword(request: UpdatePasswordRequest): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/me/password`, request);
  }
}
