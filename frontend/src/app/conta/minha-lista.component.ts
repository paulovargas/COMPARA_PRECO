import { Component, DestroyRef, effect, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  catchError,
  combineLatest,
  map,
  Observable,
  of,
  startWith,
  Subject,
  switchMap,
} from 'rxjs';
import {
  ColecoesService,
  FornecedorSeguido,
  ProdutoFavorito,
  TipoColecao,
} from './colecoes.service';
import { Pagina } from '../catalogo/catalogo.models';
import { SessaoStore } from './sessao.store';
import { PaginacaoComponent } from '../catalogo/paginacao.component';
import { SalvarButtonComponent } from './salvar-button.component';

type EstadoLista =
  | { tipo: 'carregando' }
  | { tipo: 'erro' }
  | { tipo: 'produtos'; dados: Pagina<ProdutoFavorito> }
  | { tipo: 'fornecedores'; dados: Pagina<FornecedorSeguido> };
@Component({
  selector: 'app-minha-lista',
  imports: [RouterLink, PaginacaoComponent, SalvarButtonComponent],
  templateUrl: './minha-lista.component.html',
  styleUrl: './conta.component.scss',
})
export class MinhaListaComponent {
  private readonly service = inject(ColecoesService);
  private readonly sessao = inject(SessaoStore);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly atualizar = new Subject<void>();
  readonly estado = signal<EstadoLista>({ tipo: 'carregando' });
  readonly tipo = signal<TipoColecao>('produtos');
  constructor() {
    effect(() => {
      if (this.sessao.estado() === 'pronta' && !this.sessao.usuario()) {
        this.estado.set({ tipo: 'carregando' });
        void this.router.navigate(this.sessao.aviso() ? ['/entrar'] : ['/'], {
          queryParams: this.sessao.aviso() ? { retorno: this.router.url } : {},
        });
      }
    });
    combineLatest([this.route.queryParamMap, this.atualizar.pipe(startWith(undefined))])
      .pipe(
        switchMap(([query]) => {
          const tipo = query.get('tipo') ?? 'produtos';
          const pagina = Number(query.get('pagina') ?? 0);
          if (
            !['produtos', 'fornecedores'].includes(tipo) ||
            !Number.isInteger(pagina) ||
            pagina < 0 ||
            pagina > 1000000
          )
            return of<EstadoLista>({ tipo: 'erro' });
          this.tipo.set(tipo as TipoColecao);
          this.estado.set({ tipo: 'carregando' });
          const request: Observable<EstadoLista> =
            tipo === 'produtos'
              ? this.service
                  .produtos(pagina)
                  .pipe(map((dados) => ({ tipo: 'produtos' as const, dados })))
              : this.service
                  .fornecedores(pagina)
                  .pipe(map((dados) => ({ tipo: 'fornecedores' as const, dados })));
          return request.pipe(catchError(() => of<EstadoLista>({ tipo: 'erro' })));
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((estado) => this.estado.set(estado));
  }
  recarregar(): void {
    this.atualizar.next();
  }
  pagina(pagina: number): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { tipo: this.tipo(), pagina },
    });
  }
}
