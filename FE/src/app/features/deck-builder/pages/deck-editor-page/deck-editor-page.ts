import {
  ChangeDetectionStrategy, Component, computed, inject, OnInit, signal
} from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CardService } from '../../data-access/services/card.service';
import { DeckService } from '../../data-access/services/deck.service';
import {
  CardFilters, CardResponse, CardSupertype, EnergyType
} from '../../domain/models/card.models';
import {
  DeckResponse, DeckValidationResult
} from '../../domain/models/deck.models';

@Component({
  selector: 'app-deck-editor-page',
  templateUrl: './deck-editor-page.html',
  styleUrl: './deck-editor-page.css',
  imports: [FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DeckEditorPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly cardService = inject(CardService);
  private readonly deckService = inject(DeckService);

  private deckId = '';

  readonly deck = signal<DeckResponse | null>(null);
  readonly cards = signal<CardResponse[]>([]);
  readonly loadingCards = signal(false);
  readonly loadingDeck = signal(false);
  readonly cardPage = signal(0);
  readonly totalPages = signal(0);
  readonly selectedCard = signal<CardResponse | null>(null);
  readonly validationResult = signal<DeckValidationResult | null>(null);
  readonly deckNameEditing = signal(false);
  readonly deckName = signal('');
  readonly filters = signal<CardFilters>({});

  readonly totalCardCount = computed(() => this.deck()?.totalCardCount ?? 0);
  readonly isCounterFull = computed(() => this.totalCardCount() === 60);

  readonly filteredCards = computed(() => {
    const f = this.filters();
    const all = this.cards();

    if (!f.energyType && !f.pokemonSubtype) return all;

    return all.filter(c => {
      if (f.energyType && !c.types.includes(f.energyType)) return false;
      if (f.pokemonSubtype && !c.subtypes.includes(f.pokemonSubtype)) return false;
      return true;
    });
  });

  readonly supertypes: CardSupertype[] = ['POKEMON', 'ENERGY', 'TRAINER'];
  readonly energyTypes: EnergyType[] = [
    'GRASS', 'FIRE', 'WATER', 'LIGHTNING', 'PSYCHIC',
    'FIGHTING', 'DARKNESS', 'METAL', 'FAIRY', 'DRAGON', 'COLORLESS'
  ];
  readonly pokemonSubtypes = ['Basic', 'EX', 'MEGA'];

  ngOnInit(): void {
    this.deckId = this.route.snapshot.paramMap.get('deckId')!;
    this.loadDeck();
    this.loadCards(0);
  }

  loadDeck(): void {
    this.loadingDeck.set(true);
    this.deckService.listDecks().subscribe({
      next: decks => {
        const found = decks.find(d => d.id === this.deckId) ?? null;
        this.deck.set(found);
        this.deckName.set(found?.name ?? '');
        this.loadingDeck.set(false);
      },
      error: () => this.loadingDeck.set(false)
    });
  }

  loadCards(page: number): void {
    this.loadingCards.set(true);
    this.cardService.searchCards(this.filters(), page).subscribe({
      next: result => {
        if (page === 0) {
          this.cards.set(result.content);
        } else {
          this.cards.update(prev => [...prev, ...result.content]);
        }
        this.cardPage.set(result.number);
        this.totalPages.set(result.totalPages);
        this.loadingCards.set(false);
      },
      error: () => this.loadingCards.set(false)
    });
  }

  applyFilters(filters: CardFilters): void {
    this.filters.set(filters);
    this.loadCards(0);
  }

  loadMoreCards(): void {
    if (this.cardPage() + 1 < this.totalPages()) {
      this.loadCards(this.cardPage() + 1);
    }
  }

  getQuantityInDeck(cardId: string): number {
    const entry = this.deck()?.cards.find(c => c.card.id === cardId);
    return entry?.quantity ?? 0;
  }

  addCard(card: CardResponse): void {
    const current = this.getQuantityInDeck(card.id);
    if (current === 0) {
      this.deckService.addCard(this.deckId, { cardId: card.id, quantity: 1 }).subscribe({
        next: deck => this.deck.set(deck)
      });
    } else {
      this.deckService.updateCardQuantity(this.deckId, card.id, current + 1).subscribe({
        next: deck => this.deck.set(deck)
      });
    }
  }

  removeCard(card: CardResponse): void {
    const current = this.getQuantityInDeck(card.id);
    if (current <= 1) {
      this.deckService.removeCard(this.deckId, card.id).subscribe({
        next: deck => this.deck.set(deck)
      });
    } else {
      this.deckService.updateCardQuantity(this.deckId, card.id, current - 1).subscribe({
        next: deck => this.deck.set(deck)
      });
    }
  }

  validateDeck(): void {
    this.deckService.validateDeck(this.deckId).subscribe({
      next: result => this.validationResult.set(result)
    });
  }

  openCardDetail(card: CardResponse): void {
    this.selectedCard.set(card);
  }

  closeCardDetail(): void {
    this.selectedCard.set(null);
  }

  startEditingName(): void {
    this.deckNameEditing.set(true);
  }

  saveDeckName(): void {
    this.deckNameEditing.set(false);
  }
}
