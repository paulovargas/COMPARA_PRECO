import { Component, effect, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { SessaoStore } from '../conta/sessao.store';
@Component({
  selector: 'app-admin-shell',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  template: `<section class="painel admin">
    <a routerLink="/">← Voltar à busca</a>
    <p class="eyebrow">Gestão da plataforma</p>
    <h1>Painel administrativo</h1>
    <nav class="abas" aria-label="Administração">
      @for (aba of abas; track aba.path) {
        <a
          [routerLink]="'/admin/' + aba.path"
          routerLinkActive="ativa"
          [routerLinkActiveOptions]="{ exact: true }"
          ariaCurrentWhenActive="page"
          >{{ aba.nome }}</a
        >
      }
    </nav>
    @if (sessao.usuario()?.perfil === 'ADMIN') {
      <router-outlet />
    }
  </section>`,
})
export class AdminShellComponent {
  readonly sessao = inject(SessaoStore);
  private readonly router = inject(Router);
  readonly abas = [
    { path: '', nome: 'Visão geral' },
    { path: 'usuarios', nome: 'Usuários' },
    { path: 'fornecedores', nome: 'Fornecedores' },
    { path: 'ofertas', nome: 'Ofertas' },
    { path: 'catalogo/categorias', nome: 'Catálogo' },
    { path: 'solicitacoes', nome: 'Solicitações' },
    { path: 'configuracoes', nome: 'Configurações' },
  ];
  constructor() {
    effect(() => {
      if (this.sessao.estado() === 'pronta' && this.sessao.usuario()?.perfil !== 'ADMIN') {
        const expirou = !this.sessao.usuario() && !!this.sessao.aviso();
        void this.router.navigate(expirou ? ['/entrar'] : ['/'], {
          queryParams: expirou ? { retorno: this.router.url } : {},
        });
      }
    });
  }
}
