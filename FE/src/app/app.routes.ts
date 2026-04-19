import { Routes } from '@angular/router';
import { PokedexPage } from './features/pokedex/pages/pokedex-page/pokedex-page';

export const routes: Routes = [
  {
    path: '',
    component: PokedexPage
  },
  {
    path: '**',
    redirectTo: ''
  }
];
