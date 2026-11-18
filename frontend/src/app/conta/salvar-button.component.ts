import { Component, computed, DestroyRef, inject, input, output, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { catchError, finalize, of, switchMap } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { SessaoStore } from './sessao.store';
import { ColecoesService, TipoColecao } from './colecoes.service';

@Component({
  selector: 'app-salvar-button',
  template: `<button
      class="link-botao"
      type="button"
      [disabled]="ocupado() || carregando() || resolvendoSessao()"
      [attr.aria-pressed]="salvo()"
      (click)="alterar()"
    >
      {{
        ocupado() || carregando() || resolvendoSessao()
          ? 'Aguarde…'
          : erroConsulta()
            ? 'Consultar novamente'
            : tipo() === 'produtos'
              ? salvo()
                ? '♥ Favoritado · remover'
                : '♡ Favoritar produto'
              : salvo()
                ? 'Seguindo · deixar de seguir'
                : 'Seguir fornecedor'
      }}
    </button>
    @if (erro()) {
      <small role="alert">{{ erro() }}</small>
    }`,
  styles: `
    :host {
      display: block;
    }
    small {
      display: block;
      max-width: 280px;
      color: #a23220;
      font-size: 0.75rem;
      line-height: 1.4;
    }
  `,
})
export class SalvarButtonComponent {
  readonly id = input.required<string>();
  readonly tipo = input.required<TipoColecao>();
  readonly alterado = output<void>();
  private readonly sessao = inject(SessaoStore);
  private readonly service = inject(ColecoesService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private revisao = 0;
  readonly resolvendoSessao = computed(() => this.sessao.estado() === 'carregando');
  readonly salvo = signal(false);
  readonly ocupado = signal(false);
  readonly carregando = signal(false);
  readonly erro = signal('');
  readonly erroConsulta = signal(false);
  private readonly identidade = computed(() => ({
    usuario: this.sessao.usuario()?.id,
    id: this.id(),
    tipo: this.tipo(),
  }));
  constructor() {
    toObservable(this.identidade)
      .pipe(
        switchMap(({ usuario, id, tipo }) => {
          this.salvo.set(false);
          this.erro.set('');
          this.erroConsulta.set(false);
          this.carregando.set(!!usuario);
          const revisao = ++this.revisao;
          return (usuario ? this.service.marcado(tipo, id) : of({ salvo: false })).pipe(
            catchError(() => {
              if (revisao !== this.revisao) return of({ salvo: false });
              this.erroConsulta.set(true);
              this.erro.set('Não foi possível consultar sua lista. Tente novamente.');
              return of({ salvo: false });
            }),
            finalize(() => {
              if (revisao === this.revisao) this.carregando.set(false);
            }),
            // Uma resposta antiga não sobrescreve uma alteração feita em outro botão do mesmo produto.
            switchMap((resultado) => of({ ...resultado, revisao })),
          );
        }),
        takeUntilDestroyed(),
      )
      .subscribe((resultado) => {
        if (resultado.revisao === this.revisao) this.salvo.set(resultado.salvo);
      });
    this.service.alteracoes.pipe(takeUntilDestroyed()).subscribe((mudanca) => {
      if (
        mudanca.usuario === this.sessao.usuario()?.id &&
        mudanca.id === this.id() &&
        mudanca.tipo === this.tipo()
      ) {
        this.revisao++;
        this.salvo.set(mudanca.salvo);
        this.carregando.set(false);
      }
    });
  }
  alterar(): void {
    if (this.ocupado() || this.carregando() || this.resolvendoSessao()) return;
    if (!this.sessao.usuario()) {
      void this.router.navigate(['/entrar'], { queryParams: { retorno: this.router.url } });
      return;
    }
    if (this.erroConsulta()) {
      this.consultarNovamente();
      return;
    }
    const usuario = this.sessao.usuario()!.id;
    this.ocupado.set(true);
    this.erro.set('');
    this.service
      .alterar(this.tipo(), this.id(), !this.salvo())
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.ocupado.set(false)),
      )
      .subscribe({
        next: () => {
          if (this.sessao.usuario()?.id === usuario) this.alterado.emit();
        },
        error: (erro: HttpErrorResponse) => {
          if (erro.status === 401)
            void this.router.navigate(['/entrar'], { queryParams: { retorno: this.router.url } });
          else
            this.erro.set(
              erro.status === 404
                ? 'Este item não está disponível para salvar.'
                : 'Não foi possível alterar sua lista. Tente novamente.',
            );
        },
      });
  }
  private consultarNovamente(): void {
    const revisao = ++this.revisao;
    this.carregando.set(true);
    this.service
      .marcado(this.tipo(), this.id())
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          if (revisao === this.revisao) this.carregando.set(false);
        }),
      )
      .subscribe({
        next: (resultado) => {
          if (revisao !== this.revisao) return;
          this.salvo.set(resultado.salvo);
          this.erroConsulta.set(false);
          this.erro.set('');
        },
        error: () => this.erro.set('Não foi possível consultar sua lista. Tente novamente.'),
      });
  }
}
