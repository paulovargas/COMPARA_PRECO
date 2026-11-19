import { Component, DestroyRef, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FornecedorContext } from './fornecedor.context';
import { FornecedorService } from './fornecedor.service';
import { Importacao } from './importacao.models';
import { mensagemOperacao } from './validacao-oferta';
import { CaracteristicasComponent } from '../catalogo/caracteristicas.component';

@Component({
  selector: 'app-importacao',
  imports: [DatePipe, RouterLink, CaracteristicasComponent],
  templateUrl: './importacao.component.html',
})
export class ImportacaoComponent {
  readonly contexto = inject(FornecedorContext);
  private readonly api = inject(FornecedorService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  readonly arquivo = signal<File | null>(null);
  readonly previa = signal<Importacao | null>(null);
  readonly ocupado = signal(false);
  readonly erro = signal('');
  readonly bloqueada = signal(false);
  constructor() {
    const id = this.route.snapshot.queryParamMap.get('previa');
    if (id) {
      this.ocupado.set(true);
      this.api
        .importacao(id)
        .pipe(takeUntilDestroyed())
        .subscribe({
          next: (previa) => {
            this.previa.set(previa);
            this.ocupado.set(false);
          },
          error: (erro) => {
            this.erro.set(mensagemOperacao(erro));
            this.ocupado.set(false);
          },
        });
    }
  }
  selecionar(evento: Event): void {
    const arquivo = (evento.target as HTMLInputElement).files?.[0] ?? null;
    this.arquivo.set(arquivo);
    this.erro.set('');
    this.previa.set(null);
    this.bloqueada.set(false);
    void this.router.navigate([], { relativeTo: this.route, queryParams: {} });
    if (
      arquivo &&
      (!/\.(csv|xlsx)$/i.test(arquivo.name) || arquivo.size > 2 * 1024 * 1024 || !arquivo.size)
    ) {
      this.erro.set('Escolha um arquivo CSV ou XLSX preenchido, de até 2 MB.');
      this.arquivo.set(null);
    }
  }
  preparar(): void {
    const arquivo = this.arquivo();
    if (!arquivo || this.ocupado() || !this.contexto.aprovado()) return;
    this.ocupado.set(true);
    this.erro.set('');
    this.previa.set(null);
    this.bloqueada.set(false);
    this.api
      .prepararImportacao(arquivo)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (previa) => {
          this.previa.set(previa);
          this.ocupado.set(false);
          void this.router.navigate([], {
            relativeTo: this.route,
            queryParams: { previa: previa.id },
          });
        },
        error: (erro) => {
          this.erro.set(mensagemOperacao(erro));
          this.ocupado.set(false);
        },
      });
  }
  confirmar(): void {
    const previa = this.previa();
    if (
      !previa ||
      previa.invalidas ||
      previa.situacao !== 'PREVIA' ||
      this.ocupado() ||
      this.bloqueada() ||
      !this.contexto.aprovado()
    )
      return;
    this.ocupado.set(true);
    this.erro.set('');
    this.api
      .confirmarImportacao(previa.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (resultado) => {
          this.previa.set(resultado);
          this.ocupado.set(false);
        },
        error: (erro) => {
          this.erro.set(mensagemOperacao(erro));
          this.ocupado.set(false);
          this.bloqueada.set(erro.status === 409 || erro.status === 410 || erro.status === 404);
        },
      });
  }
}
