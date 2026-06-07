import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import { CardFilters, CardPage, CardResponse } from '../../domain/models/card.models';

@Injectable({ providedIn: 'root' })
export class CardService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/cards`;

  searchCards(filters: CardFilters, page: number, size: number = 20): Observable<CardPage> {
    let params = new HttpParams()
      .set('set', 'xy1')
      .set('page', page)
      .set('size', size);

    if (filters.name) params = params.set('name', filters.name);
    if (filters.type) params = params.set('type', filters.type);

    return this.http.get<CardPage>(this.apiUrl, { params });
  }

  getCard(id: string): Observable<CardResponse> {
    return this.http.get<CardResponse>(`${this.apiUrl}/${id}`);
  }
}
