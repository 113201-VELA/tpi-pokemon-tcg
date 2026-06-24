import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'auth/login', pathMatch: 'full' },
  {
    path: 'auth',
    loadChildren: () =>
      import('./features/auth/auth.routes').then(m => m.AUTH_ROUTES)
  },
  {
    path: 'home',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/home/pages/home-page/home-page').then(m => m.HomePage)
  },
  {
    path: 'decks',
    canActivate: [authGuard],
    loadChildren: () =>
      import('./features/deck-builder/deck-builder.routes').then(m => m.DECK_BUILDER_ROUTES)
  },
  {
    path: 'lobby',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/lobby/pages/lobby-page/lobby-page').then(m => m.LobbyPage)
  },
  {
    path: 'preferences',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/preferences/pages/preferences-page/preferences-page')
        .then(m => m.PreferencesPage)
  },
  { path: '**', redirectTo: 'home' }
];
