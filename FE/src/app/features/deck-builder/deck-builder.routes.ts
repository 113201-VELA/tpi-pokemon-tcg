import { Routes } from '@angular/router';

export const DECK_BUILDER_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/deck-list-page/deck-list-page').then(m => m.DeckListPage)
  },
  {
    path: ':deckId/edit',
    loadComponent: () =>
      import('./pages/deck-editor-page/deck-editor-page').then(m => m.DeckEditorPage)
  }
];
