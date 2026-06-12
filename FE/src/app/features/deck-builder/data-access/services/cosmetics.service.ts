import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';

export interface CosmeticsOptions {
  cardBacks: string[];
  coins: string[];
}

@Injectable({ providedIn: 'root' })
export class CosmeticsService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/players`;

  /** Returns the available cosmetic options for card backs and coins. */
  getCosmeticsOptions(): Observable<CosmeticsOptions> {
    return this.http.get<CosmeticsOptions>(`${this.apiUrl}/cosmetics`);
  }
}
