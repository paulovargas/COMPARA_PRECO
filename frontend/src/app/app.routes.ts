import { Routes } from '@angular/router';
import { fornecedorGuard } from './fornecedor/fornecedor.guard';
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
  {
    path: 'fornecedor',
    canActivate: [fornecedorGuard],
    canActivateChild: [fornecedorGuard],
    loadComponent: () =>
      import('./fornecedor/fornecedor-shell.component').then((m) => m.FornecedorShellComponent),
    children: [
      {
        path: 'importacao',
        data: { exigeFornecedor: true },
        loadComponent: () =>
          import('./fornecedor/importacao.component').then((m) => m.ImportacaoComponent),
        title: 'Importar ofertas | Compara Preço',
      },
      {
        path: '',
        loadComponent: () =>
          import('./fornecedor/empresa.component').then((m) => m.EmpresaComponent),
        title: 'Minha empresa | Compara Preço',
      },
      {
        path: 'ofertas',
        data: { exigeFornecedor: true },
        loadComponent: () =>
          import('./fornecedor/ofertas.component').then((m) => m.OfertasComponent),
        title: 'Minhas ofertas | Compara Preço',
      },
      {
        path: 'catalogo',
        data: { exigeFornecedor: true },
        loadComponent: () =>
          import('./fornecedor/catalogo-fornecedor.component').then(
            (m) => m.CatalogoFornecedorComponent,
          ),
        title: 'Catálogo do fornecedor | Compara Preço',
      },
      {
        path: 'solicitacoes',
        data: { exigeFornecedor: true },
        loadComponent: () =>
          import('./fornecedor/solicitacoes.component').then((m) => m.SolicitacoesComponent),
        title: 'Solicitações de catálogo | Compara Preço',
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
