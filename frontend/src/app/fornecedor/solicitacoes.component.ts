import { Component, DestroyRef, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, combineLatest, finalize, of, startWith, Subject, switchMap } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { Pagina } from '../catalogo/catalogo.models';
import { PaginacaoComponent } from '../catalogo/paginacao.component';
import { FornecedorContext } from './fornecedor.context';
import { FornecedorService } from './fornecedor.service';
import { SolicitacaoCatalogo } from './fornecedor.models';
import { mensagemOperacao } from './validacao-oferta';

@Component({
  selector: 'app-solicitacoes',
  imports: [ReactiveFormsModule, RouterLink, DatePipe, PaginacaoComponent],
  templateUrl: './solicitacoes.component.html',
})
export class SolicitacoesComponent {
  readonly contexto = inject(FornecedorContext);
  private readonly service = inject(FornecedorService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly atualizar = new Subject<void>();
  readonly dados = signal<Pagina<SolicitacaoCatalogo> | null>(null);
  readonly carregando = signal(true);
  readonly ocupado = signal(false);
  readonly erro = signal('');
  readonly erroLista = signal('');
  readonly sucesso = signal('');
  readonly form = new FormGroup({
    nomeProduto: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(2), Validators.maxLength(160)],
    }),
    marca: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(2), Validators.maxLength(100)],
    }),
    modelo: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(2), Validators.maxLength(120)],
    }),
    categoria: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(2), Validators.maxLength(100)],
    }),
    caracteristicas: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(10), Validators.maxLength(2000)],
    }),
  });
  constructor() {
    combineLatest([this.route.queryParamMap, this.atualizar.pipe(startWith(undefined))])
      .pipe(
        switchMap(([query]) => {
          const pagina = Number(query.get('pagina') ?? 0);
          this.dados.set(null);
          this.erroLista.set('');
          if (!Number.isInteger(pagina) || pagina < 0 || pagina > 1000000) {
            this.erroLista.set(
              'A página informada é inválida. Abra novamente a área de solicitações.',
            );
            return of(null);
          }
          this.carregando.set(true);
          return this.service.solicitacoes(pagina).pipe(
            catchError(() => {
              this.erroLista.set('Não foi possível carregar suas solicitações. Tente novamente.');
              return of(null);
            }),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((dados) => {
        this.dados.set(dados);
        this.carregando.set(false);
      });
  }
  recarregar(): void {
    this.atualizar.next();
  }
  pagina(pagina: number): void {
    void this.router.navigate([], { relativeTo: this.route, queryParams: { pagina } });
  }
  enviar(): void {
    if (this.ocupado() || !this.contexto.aprovado()) return;
    for (const campo of Object.values(this.form.controls))
      campo.setValue(campo.value.trim().replace(/\s+/g, ' '));
    this.form.markAllAsTouched();
    this.erro.set('');
    this.sucesso.set('');
    if (this.form.invalid) return;
    this.ocupado.set(true);
    this.service
      .solicitar(this.form.getRawValue())
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.ocupado.set(false)),
      )
      .subscribe({
        next: () => {
          this.form.reset();
          this.sucesso.set(
            'Solicitação enviada para análise. A variante será disponibilizada no catálogo após atendimento.',
          );
          if (Number(this.route.snapshot.queryParamMap.get('pagina') ?? 0) > 0) this.pagina(0);
          else this.recarregar();
        },
        error: (erro: HttpErrorResponse) => this.erro.set(mensagemOperacao(erro)),
      });
  }
}
