import {
  ChangeDetectionStrategy, Component, computed, inject, OnInit, signal
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CardService } from '../../data-access/services/card.service';
import { DeckService } from '../../data-access/services/deck.service';
import { CosmeticsService } from '../../data-access/services/cosmetics.service';
import {
  CardFilters, CardResponse, CardSupertype, EnergyType
} from '../../domain/models/card.models';
import { DeckResponse } from '../../domain/models/deck.models';

@Component({
  selector: 'app-deck-editor-page',
  templateUrl: './deck-editor-page.html',
  styleUrl: './deck-editor-page.css',
  imports: [FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DeckEditorPage implements OnInit {
  private readonly route          = inject(ActivatedRoute);
  private readonly router         = inject(Router);
  private readonly cardService    = inject(CardService);
  private readonly deckService    = inject(DeckService);
  private readonly cosmeticsService = inject(CosmeticsService);

  private deckId = '';

  readonly deck            = signal<DeckResponse | null>(null);
  readonly cards           = signal<CardResponse[]>([]);
  readonly loadingCards    = signal(false);
  readonly loadingDeck     = signal(false);
  readonly cardPage        = signal(0);
  readonly totalPages      = signal(0);
  readonly selectedCard    = signal<CardResponse | null>(null);
  readonly deckNameEditing = signal(false);
  readonly deckName        = signal('');
  readonly filters         = signal<CardFilters>({});

  // Cosmetics state
  readonly cardBackOptions  = signal<string[]>([]);
  readonly coinOptions      = signal<string[]>([]);
  readonly selectedCardBack = signal<string>('DEFAULT');
  readonly selectedCoin     = signal<string>('DEFAULT');
  readonly savingCosmetics  = signal(false);

  /** Resolves the asset path for the currently selected card back. */
  readonly cardBackPreviewUrl = computed(() => {
    const map: Record<string, string> = {
      DEFAULT:    'assets/cardBack/defaultBack.png',
      PIKACHU:    'assets/cardBack/pikachuBack.png',
      BULBASAUR:  'assets/cardBack/bulbasaurBack.png',
      CHARMANDER: 'assets/cardBack/charmanderBack.png',
      SQUIRTLE:   'assets/cardBack/squirtleBack.png',
    };
    return map[this.selectedCardBack()] ?? map['DEFAULT'];
  });

  /** Resolves the asset path for the currently selected coin (heads side). */
  readonly coinPreviewUrl = computed(() => {
    const map: Record<string, string> = {
      DEFAULT:    'assets/coin/defaultCoinHead.png',
      PIKACHU:    'assets/coin/pikachuCoin.png',
      BULBASAUR:  'assets/coin/bulbasaurCoin.png',
      CHARMANDER: 'assets/coin/charmanderCoin.png',
      SQUIRTLE:   'assets/coin/squirtleCoin.png',
    };
    return map[this.selectedCoin()] ?? map['DEFAULT'];
  });

  readonly totalCardCount  = computed(() => this.deck()?.totalCardCount ?? 0);
  readonly isCounterFull   = computed(() => this.totalCardCount() === 60);

  readonly cardNameCounts = computed(() => {
    const currentDeck = this.deck();
    const counts: Record<string, number> = {};
    if (!currentDeck) return counts;
    for (const entry of currentDeck.cards) {
      const card = entry.card;
      const isBasicEnergy = card.supertype === 'ENERGY' && card.subtypes.includes('Basic');
      if (!isBasicEnergy) {
        counts[card.name] = (counts[card.name] || 0) + entry.quantity;
      }
    }
    return counts;
  });

  readonly hasNoExcessCopies = computed(() => {
    return !Object.values(this.cardNameCounts()).some(count => count > 4);
  });

  readonly hasBasicPokemon = computed(() => {
    return this.deck()?.cards.some(entry =>
      entry.card.supertype === 'POKEMON' && entry.card.subtypes.includes('Basic')
    ) ?? false;
  });

  readonly isDeckValidForPlaying = computed(() => {
    return this.totalCardCount() === 60 && this.hasNoExcessCopies() && this.hasBasicPokemon();
  });

  readonly filteredCards = computed(() => {
    const f   = this.filters();
    const all = this.cards();

    if (!f.energyType && !f.pokemonSubtype) return all;

    return all.filter(c => {
      if (f.energyType    && !c.types.includes(f.energyType))       return false;
      if (f.pokemonSubtype && !c.subtypes.includes(f.pokemonSubtype)) return false;
      return true;
    });
  });

  readonly supertypes: CardSupertype[] = ['POKEMON', 'ENERGY', 'TRAINER'];
  readonly energyTypes: EnergyType[]   = [
    'GRASS', 'FIRE', 'WATER', 'LIGHTNING', 'PSYCHIC',
    'FIGHTING', 'DARKNESS', 'METAL', 'FAIRY', 'DRAGON', 'COLORLESS'
  ];
  readonly pokemonSubtypes = ['Basic', 'EX', 'MEGA'];

  ngOnInit(): void {
    this.deckId = this.route.snapshot.paramMap.get('deckId')!;
    this.loadDeck();
    this.loadCards(0);
    this.loadCosmeticsOptions();
  }

  loadDeck(): void {
    this.loadingDeck.set(true);
    this.deckService.listDecks().subscribe({
      next: decks => {
        const found = decks.find(d => d.id === this.deckId) ?? null;
        this.deck.set(found);
        this.deckName.set(found?.name ?? '');
        this.selectedCardBack.set(found?.cardBack ?? 'DEFAULT');
        this.selectedCoin.set(found?.coin ?? 'DEFAULT');
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

  private loadCosmeticsOptions(): void {
    this.cosmeticsService.getCosmeticsOptions().subscribe({
      next: options => {
        this.cardBackOptions.set(options.cardBacks);
        this.coinOptions.set(options.coins);
      }
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

  canAddCard(card: CardResponse): boolean {
    if (this.isCounterFull()) return false;

    const isBasicEnergy = card.supertype === 'ENERGY' && card.subtypes.includes('Basic');
    if (isBasicEnergy) return true;

    const currentCount = this.cardNameCounts()[card.name] || 0;
    return currentCount < 4;
  }

  addCard(card: CardResponse): void {
    if (!this.canAddCard(card)) return;

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

  openCardDetail(card: CardResponse): void {
    this.selectedCard.set(card);
  }

  closeCardDetail(): void {
    this.selectedCard.set(null);
  }

  startEditingName(): void {
    this.deckNameEditing.set(true);
  }

  cancelEditingName(): void {
    this.deckName.set(this.deck()?.name ?? '');
    this.deckNameEditing.set(false);
  }

  saveDeckName(): void {
    const newName = this.deckName().trim();
    if (!newName || newName === this.deck()?.name) {
      this.deckNameEditing.set(false);
      return;
    }

    this.deckService.updateDeck(this.deckId, { name: newName }).subscribe({
      next: updatedDeck => {
        this.deck.set(updatedDeck);
        this.deckName.set(updatedDeck.name);
        this.deckNameEditing.set(false);
      },
      error: () => {
        this.deckName.set(this.deck()?.name || '');
        this.deckNameEditing.set(false);
      }
    });
  }

  saveCosmetics(): void {
    const cardBack = this.selectedCardBack();
    const coin     = this.selectedCoin();

    if (
      cardBack === this.deck()?.cardBack &&
      coin     === this.deck()?.coin
    ) return;

    this.savingCosmetics.set(true);
    this.deckService.updateDeck(this.deckId, { cardBack, coin }).subscribe({
      next: updatedDeck => {
        this.deck.set(updatedDeck);
        this.savingCosmetics.set(false);
      },
      error: () => this.savingCosmetics.set(false)
    });
  }

  goBackToDecks(): void {
    this.router.navigate(['/decks']);
  }
}
