import { Component, DestroyRef, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink, Params } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  catchError,
  combineLatest,
  forkJoin,
  map,
  Observable,
  of,
  startWith,
  Subject,
  switchMap,
} from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { CatalogoService } from './catalogo.service';
import {
  CAMPOS_FILTRO,
  Comparacao,
  ESTADOS,
  FiltroCatalogo,
  OpcaoCatalogo,
  Pagina,
  VarianteBusca,
} from './catalogo.models';
import { PaginacaoComponent } from './paginacao.component';
import { FiltrosComponent } from './filtros.component';
import { CaracteristicasComponent } from './caracteristicas.component';

type EstadoPagina =
  | { tipo: 'carregando' }
  | { tipo: 'erro'; mensagem: string }
  | { tipo: 'busca'; dados: Pagina<VarianteBusca> }
  | { tipo: 'comparacao'; dados: Comparacao };
const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@Component({
  selector: 'app-catalogo-page',
  imports: [
    PaginacaoComponent,
    RouterLink,
    CurrencyPipe,
    DatePipe,
    FiltrosComponent,
    CaracteristicasComponent,
  ],
  templateUrl: './catalogo-page.component.html',
  styleUrl: './catalogo-page.component.scss',
})
export class CatalogoPageComponent {
  private readonly service = inject(CatalogoService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly recarregar = new Subject<void>();
  readonly estado = signal<EstadoPagina>({ tipo: 'carregando' });
  readonly categorias = signal<OpcaoCatalogo[]>([]);
  readonly marcas = signal<OpcaoCatalogo[]>([]);
  readonly erroOpcoes = signal(false);
  readonly filtro = signal<FiltroCatalogo>({ pagina: 0, tamanho: 12 });
  readonly varianteId = signal<string | null>(null);
  readonly imagensComErro = signal<Set<string>>(new Set());
  readonly parametros = signal<Params>({});

  constructor() {
    this.carregarOpcoes();
    combineLatest([
      this.route.paramMap,
      this.route.queryParamMap,
      this.recarregar.pipe(startWith(undefined)),
    ])
      .pipe(
        switchMap(([parametros, query]) => {
          const id = parametros.get('id');
          this.varianteId.set(id);
          this.parametros.set(
            Object.fromEntries(query.keys.map((chave) => [chave, query.get(chave)])),
          );
          const filtro: FiltroCatalogo = {
            pagina: Number(query.get('pagina') ?? 0),
            tamanho: Number(query.get('tamanho') ?? 12),
          };
          for (const campo of CAMPOS_FILTRO) {
            const valor = query.get(campo)?.trim();
            if (valor) filtro[campo] = valor;
          }
          this.filtro.set(filtro);
          if (!this.valido(filtro) || (id !== null && !UUID.test(id))) {
            return of<EstadoPagina>({
              tipo: 'erro',
              mensagem:
                'Os filtros do endereço são inválidos. Limpe a busca para começar novamente.',
            });
          }
          this.estado.set({ tipo: 'carregando' });
          const request: Observable<EstadoPagina> = id
            ? this.service
                .comparar(id, filtro)
                .pipe(map((dados) => ({ tipo: 'comparacao' as const, dados })))
            : this.service.buscar(filtro).pipe(map((dados) => ({ tipo: 'busca' as const, dados })));
          return request.pipe(
            catchError((erro: HttpErrorResponse) =>
              of<EstadoPagina>({
                tipo: 'erro',
                mensagem:
                  erro.status === 404
                    ? 'Este produto não está disponível para comparação. Volte à busca para encontrar outras opções.'
                    : 'Não foi possível carregar as ofertas. Confira sua conexão e tente novamente.',
              }),
            ),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((estado) => this.estado.set(estado));
  }

  carregarOpcoes(): void {
    this.erroOpcoes.set(false);
    forkJoin({ categorias: this.service.categorias(), marcas: this.service.marcas() })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (opcoes) => {
          this.categorias.set(opcoes.categorias);
          this.marcas.set(opcoes.marcas);
        },
        error: () => this.erroOpcoes.set(true),
      });
  }

  aplicar(filtro: Partial<FiltroCatalogo>): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { ...this.parametros(), ...filtro, pagina: 0 },
    });
  }
  limpar(): void {
    void this.router.navigate(['/']);
  }
  tentarNovamente(): void {
    this.recarregar.next();
  }
  pagina(numero: number): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { ...this.parametros(), pagina: numero },
    });
  }
  parametrosComparacao(): Params {
    return { ...this.parametros(), pagina: 0, buscaPagina: this.filtro().pagina };
  }
  parametrosVoltar(): Params {
    const { buscaPagina, ...parametros } = this.parametros();
    return {
      ...parametros,
      pagina: /^\d{1,7}$/.test(String(buscaPagina ?? '0')) ? Number(buscaPagina ?? 0) : 0,
    };
  }
  fornecedor(id: string): void {
    this.aplicar({ fornecedorId: id });
  }
  removerFornecedor(): void {
    this.aplicar({ fornecedorId: undefined });
  }
  falhaImagem(id: string): void {
    this.imagensComErro.update((ids) => new Set([...ids, id]));
  }
  private valido(f: FiltroCatalogo): boolean {
    return (
      Number.isInteger(f.pagina) &&
      f.pagina >= 0 &&
      f.pagina <= 1000000 &&
      Number.isInteger(f.tamanho) &&
      f.tamanho >= 1 &&
      f.tamanho <= 50 &&
      [f.categoriaId, f.marcaId, f.fornecedorId].every((id) => !id || UUID.test(id)) &&
      (!f.condicao || ['NOVO', 'USADO', 'RECONDICIONADO'].includes(f.condicao)) &&
      (!f.estado || ESTADOS.includes(f.estado)) &&
      (!f.armazenamentoGb ||
        (/^[1-9][0-9]*$/.test(f.armazenamentoGb) && Number(f.armazenamentoGb) <= 2147483647)) &&
      CAMPOS_FILTRO.every((campo) => !/[\u0000-\u001f\u007f]/.test(f[campo] ?? '')) &&
      (f.texto?.length ?? 0) <= 100 &&
      (f.modelo?.length ?? 0) <= 120 &&
      (f.cor?.length ?? 0) <= 80 &&
      (f.cidade?.length ?? 0) <= 100
    );
  }
}
