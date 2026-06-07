import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { DeckService } from '../../data-access/services/deck.service';
import { DeckResponse } from '../../domain/models/deck.models';

@Component({
  selector: 'app-deck-list-page',
  templateUrl: './deck-list-page.html',
  styleUrl: './deck-list-page.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DeckListPage implements OnInit {
  private readonly deckService = inject(DeckService);
  private readonly router = inject(Router);

  readonly decks = signal<DeckResponse[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.loadDecks();
  }

  loadDecks(): void {
    this.loading.set(true);
    this.error.set(null);
    this.deckService.listDecks().subscribe({
      next: decks => {
        this.decks.set(decks);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar los mazos.');
        this.loading.set(false);
      }
    });
  }

  createDeck(): void {
    this.deckService.createDeck({ name: 'Nuevo mazo' }).subscribe({
      next: deck => this.router.navigate(['/decks', deck.id, 'edit']),
      error: () => this.error.set('No se pudo crear el mazo.')
    });
  }

  openDeck(deck: DeckResponse): void {
    this.router.navigate(['/decks', deck.id, 'edit']);
  }

  deleteDeck(event: Event, deck: DeckResponse): void {
    event.stopPropagation();
    if (!window.confirm(`¿Eliminar el mazo "${deck.name}"?`)) return;
    this.deckService.deleteDeck(deck.id).subscribe({
      next: () => this.decks.update(decks => decks.filter(d => d.id !== deck.id)),
      error: () => this.error.set('No se pudo eliminar el mazo.')
    });
  }
}
