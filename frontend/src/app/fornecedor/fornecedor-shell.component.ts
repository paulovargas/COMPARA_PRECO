import { Component, effect, inject, untracked } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { SessaoStore } from '../conta/sessao.store';
import { FornecedorContext } from './fornecedor.context';

@Component({
  selector: 'app-fornecedor-shell',
  providers: [FornecedorContext],
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  template: `<section class="painel" aria-labelledby="titulo-painel">
    <a routerLink="/">← Voltar à busca</a>
    <p class="eyebrow">Sua empresa no Compara Preço</p>
    <h1 id="titulo-painel">Área do fornecedor</h1>
    @if (sessao.usuario()?.perfil === 'FORNECEDOR') {
      <nav class="abas" aria-label="Painel do fornecedor">
        <a
          routerLink="/fornecedor"
          routerLinkActive="ativa"
          [routerLinkActiveOptions]="{ exact: true }"
          ariaCurrentWhenActive="page"
          >Empresa</a
        ><a routerLink="/fornecedor/ofertas" routerLinkActive="ativa" ariaCurrentWhenActive="page"
          >Ofertas</a
        ><a routerLink="/fornecedor/catalogo" routerLinkActive="ativa" ariaCurrentWhenActive="page"
          >Catálogo</a
        ><a
          routerLink="/fornecedor/solicitacoes"
          routerLinkActive="ativa"
          ariaCurrentWhenActive="page"
          >Solicitações</a
        >
        <a routerLink="/fornecedor/importacao" routerLinkActive="ativa" ariaCurrentWhenActive="page"
          >Importar ofertas</a
        >
      </nav>
      @if (contexto.carregando()) {
        <p role="status">Carregando dados da empresa…</p>
      }
      @if (contexto.erro()) {
        <div class="aviso" role="alert">
          {{ contexto.erro() }}
          <button class="link-botao" (click)="contexto.carregar()">Tentar novamente</button>
        </div>
      }
      @if (contexto.empresa(); as empresa) {
        <div class="situacao" role="status">
          <strong>{{ empresa.nomeFantasia }}</strong><span>{{
            empresa.situacao === 'APROVADO'
              ? 'Empresa aprovada'
              : empresa.situacao === 'PENDENTE'
                ? 'Aguardando aprovação'
                : 'Empresa bloqueada'
          }}</span
          ><button
            class="link-botao"
            [disabled]="contexto.carregando()"
            (click)="contexto.carregar()"
          >
            Atualizar situação
          </button>
        </div>
        @if (empresa.situacao !== 'APROVADO') {
          <p class="aviso">
            {{
              empresa.situacao === 'PENDENTE'
                ? 'Você pode atualizar o perfil. Aguarde a aprovação para cadastrar ou alterar ofertas e solicitar inclusões no catálogo.'
                : 'Seu painel está disponível para consulta. Alterações estão bloqueadas.'
            }}
            @if (empresa.motivoModeracao) {
              <span>Motivo: {{ empresa.motivoModeracao }}</span>
            }
          </p>
        }
      }
    }
    <router-outlet />
  </section>`,
})
export class FornecedorShellComponent {
  readonly sessao = inject(SessaoStore);
  readonly contexto = inject(FornecedorContext);
  private readonly router = inject(Router);
  constructor() {
    effect(() => {
      const usuario = this.sessao.usuario();
      if (this.sessao.estado() === 'pronta' && !usuario)
        void this.router.navigate(['/entrar'], { queryParams: { retorno: this.router.url } });
      else if (usuario?.perfil === 'FORNECEDOR') untracked(() => this.contexto.carregar());
    });
  }
}
