import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'decks',
    pathMatch: 'full'
  },
  {
    path: 'auth',
    loadChildren: () =>
      import('./features/auth/auth.routes').then(m => m.AUTH_ROUTES)
  },
  {
    path: 'decks',
    canActivate: [authGuard],
    loadChildren: () =>
      import('./features/deck-builder/deck-builder.routes').then(m => m.DECK_BUILDER_ROUTES)
  },
  {
    path: '**',
    redirectTo: 'decks'
  }
];
