import { Routes } from '@angular/router';
import { contaGuard } from './conta/conta.guard';

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
  {
    path: 'entrar',
    loadComponent: () => import('./conta/conta-form.component').then((m) => m.ContaFormComponent),
    title: 'Entrar | Compara Preço',
  },
  {
    path: 'cadastro',
    loadComponent: () => import('./conta/conta-form.component').then((m) => m.ContaFormComponent),
    data: { cadastro: true },
    title: 'Criar conta | Compara Preço',
  },
  {
    path: 'minha-lista',
    canActivate: [contaGuard],
    loadComponent: () => import('./conta/minha-lista.component').then((m) => m.MinhaListaComponent),
    title: 'Minha lista | Compara Preço',
  },
  { path: '**', redirectTo: '' },
];
