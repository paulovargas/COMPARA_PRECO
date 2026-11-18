import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./catalogo/catalogo-page.component').then((m) => m.CatalogoPageComponent),
    title: 'Buscar produtos | Compara Preço',
  },
  {
    path: 'comparar/:id',
    loadComponent: () =>
      import('./catalogo/catalogo-page.component').then((m) => m.CatalogoPageComponent),
    title: 'Comparar ofertas | Compara Preço',
  },
  { path: '**', redirectTo: '' },
];
