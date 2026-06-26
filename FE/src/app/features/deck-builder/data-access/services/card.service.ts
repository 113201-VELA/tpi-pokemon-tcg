import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import { CardFilters, CardResponse } from '../../domain/models/card.models';

@Injectable({ providedIn: 'root' })
export class CardService {
  private readonly http   = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/cards`;

  searchCards(filters: CardFilters): Observable<CardResponse[]> {
    let params = new HttpParams().set('set', 'xy1');

    if (filters.name)            params = params.set('name', filters.name);
    if (filters.type)            params = params.set('type', filters.type);
    if (filters.energyType)      params = params.set('energyType', filters.energyType);
    if (filters.pokemonSubtype)  params = params.set('pokemonSubtype', filters.pokemonSubtype);

    return this.http.get<CardResponse[]>(this.apiUrl, { params });
  }

  getCard(id: string): Observable<CardResponse> {
    return this.http.get<CardResponse>(`${this.apiUrl}/${id}`);
  }
}
