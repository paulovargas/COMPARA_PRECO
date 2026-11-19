import { Component, DestroyRef, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AdminService } from './admin.service';
import { Estatisticas } from './admin.models';
import { mensagemOperacao } from '../fornecedor/validacao-oferta';
@Component({
  selector: 'app-visao-geral',
  imports: [DatePipe],
  template: `<h2>Visão geral</h2>
    <p>
      Fotografia atual dos registros da plataforma. Aprovação da empresa e elegibilidade das ofertas
      têm critérios diferentes.
    </p>
    <div class="acoes">
      <button class="botao" [disabled]="carregando()" (click)="carregar()">
        Atualizar indicadores</button
      ><a href="/api/admin/relatorios/regioes.csv" download="regioes.csv"
        >Baixar relatório por estado (CSV)</a
      >
    </div>
    @if (carregando()) {
      <p role="status">Carregando indicadores…</p>
    }
    @if (erro()) {
      <p class="aviso" role="alert">{{ erro() }}</p>
    }
    @if (dados(); as d) {
      <p>Gerado em {{ d.geradoEm | date: 'dd/MM/yyyy HH:mm' }}</p>
      <div class="lista-painel indicadores">
        @for (i of d.indicadores; track i.nome) {
          <article class="cartao">
            <h3>{{ i.nome }}</h3>
            <strong class="numero-indicador">{{ i.quantidade }}</strong>
          </article>
        }
      </div>
      <h3>Fornecedores e ofertas por estado</h3>
      <div class="lista-painel">
        @for (r of d.regioes; track r.uf) {
          <article class="cartao">
            <h3>{{ r.uf }}</h3>
            <p>
              {{ r.fornecedores }} fornecedores · {{ r.aprovados }} aprovados ·
              {{ r.ofertasElegiveis }} ofertas elegíveis
            </p>
          </article>
        } @empty {
          <p>Nenhum fornecedor cadastrado.</p>
        }
      </div>
    }`,
})
export class VisaoGeralComponent {
  private readonly api = inject(AdminService);
  private readonly destroyRef = inject(DestroyRef);
  readonly dados = signal<Estatisticas | null>(null);
  readonly carregando = signal(false);
  readonly erro = signal('');
  constructor() {
    this.carregar();
  }
  carregar(): void {
    if (this.carregando()) return;
    this.carregando.set(true);
    this.erro.set('');
    this.api
      .estatisticas()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (dados) => {
          this.dados.set(dados);
          this.carregando.set(false);
        },
        error: (erro) => {
          this.erro.set(mensagemOperacao(erro));
          this.carregando.set(false);
        },
      });
  }
}
