import { Component, DestroyRef, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, combineLatest, finalize, of, startWith, Subject, switchMap } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { Pagina } from '../catalogo/catalogo.models';
import { CaracteristicasComponent } from '../catalogo/caracteristicas.component';
import { PaginacaoComponent } from '../catalogo/paginacao.component';
import { OfertaFornecedor } from './fornecedor.models';
import { FornecedorService } from './fornecedor.service';
import { FornecedorContext } from './fornecedor.context';
import { OfertaEditorComponent } from './oferta-editor.component';
import { mensagemOperacao } from './validacao-oferta';

@Component({
  selector: 'app-ofertas',
  imports: [
    RouterLink,
    CurrencyPipe,
    DatePipe,
    CaracteristicasComponent,
    PaginacaoComponent,
    OfertaEditorComponent,
  ],
  templateUrl: './ofertas.component.html',
})
export class OfertasComponent {
  readonly contexto = inject(FornecedorContext);
  private readonly service = inject(FornecedorService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly atualizar = new Subject<void>();
  readonly dados = signal<Pagina<OfertaFornecedor> | null>(null);
  readonly carregando = signal(true);
  readonly erro = signal('');
  readonly sucesso = signal(
    this.route.snapshot.queryParamMap.get('criada') === '1'
      ? 'Oferta cadastrada e enviada para moderação.'
      : '',
  );
  readonly editando = signal<OfertaFornecedor | null>(null);
  readonly exclusao = signal<OfertaFornecedor | null>(null);
  readonly excluindo = signal(false);
  readonly conflitoExclusao = signal(false);
  constructor() {
    combineLatest([this.route.queryParamMap, this.atualizar.pipe(startWith(undefined))])
      .pipe(
        switchMap(([query]) => {
          const pagina = Number(query.get('pagina') ?? 0);
          this.dados.set(null);
          this.erro.set('');
          if (!Number.isInteger(pagina) || pagina < 0 || pagina > 1000000) {
            this.erro.set('A página informada é inválida. Abra novamente a área de ofertas.');
            this.carregando.set(false);
            return of(null);
          }
          this.carregando.set(true);
          return this.service.ofertas(pagina).pipe(
            catchError(() => {
              this.erro.set('Não foi possível carregar suas ofertas. Tente novamente.');
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
    this.conflitoExclusao.set(false);
    this.editando.set(null);
    this.exclusao.set(null);
    this.atualizar.next();
  }
  pagina(pagina: number): void {
    this.conflitoExclusao.set(false);
    this.editando.set(null);
    this.exclusao.set(null);
    void this.router.navigate([], { relativeTo: this.route, queryParams: { pagina } });
  }
  salva(): void {
    this.sucesso.set('Oferta atualizada. A moderação foi preservada.');
    this.recarregar();
  }
  confirmarExclusao(): void {
    const oferta = this.exclusao();
    if (!oferta || this.excluindo() || this.conflitoExclusao() || !this.contexto.aprovado()) return;
    this.excluindo.set(true);
    this.erro.set('');
    this.service
      .excluirOferta(oferta.id, oferta.versao)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.excluindo.set(false)),
      )
      .subscribe({
        next: () => {
          this.sucesso.set('Oferta excluída.');
          this.recarregar();
        },
        error: (erro: HttpErrorResponse) => {
          this.conflitoExclusao.set(
            erro.status === 409 && erro.error?.codigo === 'REGISTRO_DESATUALIZADO',
          );
          this.erro.set(mensagemOperacao(erro));
        },
      });
  }
}
