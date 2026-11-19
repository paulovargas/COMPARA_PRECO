import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, startWith, switchMap, tap, catchError, of } from 'rxjs';
import { Pagina, Variante } from '../catalogo/catalogo.models';
import { PaginacaoComponent } from '../catalogo/paginacao.component';
import { mensagemOperacao } from '../fornecedor/validacao-oferta';
import { AdminService } from './admin.service';
import { ItemCatalogo, TipoCatalogo } from './admin.models';
import { ReferenciaCatalogoComponent, rotuloCatalogo } from './referencia-catalogo.component';
@Component({
  selector: 'app-catalogo-admin',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    RouterLinkActive,
    PaginacaoComponent,
    ReferenciaCatalogoComponent,
  ],
  templateUrl: './catalogo-admin.component.html',
})
export class CatalogoAdminComponent {
  private readonly api = inject(AdminService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  readonly tipo = this.route.snapshot.data['tipo'] as TipoCatalogo;
  readonly tipos: TipoCatalogo[] = ['categorias', 'marcas', 'produtos', 'variantes'];
  readonly rotulo = rotuloCatalogo;
  readonly dados = signal<Pagina<ItemCatalogo> | null>(null);
  readonly erro = signal('');
  readonly sucesso = signal('');
  readonly carregando = signal(false);
  readonly salvando = signal(false);
  readonly conflito = signal(false);
  readonly editando = signal(false);
  readonly item = signal<ItemCatalogo | null>(null);
  readonly excluir = signal<ItemCatalogo | null>(null);
  readonly form = inject(FormBuilder).nonNullable.group({
    nome: '',
    exigeArmazenamento: false,
    exigeCor: false,
    atributosObrigatorios: '',
    categoriaId: '',
    marcaId: '',
    modelo: '',
    descricao: '',
    imagemUrl: '',
    produtoId: '',
    armazenamentoGb: '',
    cor: '',
    condicao: 'NOVO',
    atributos: '',
  });
  private readonly recarregar = new Subject<void>();
  constructor() {
    this.route.queryParamMap
      .pipe(
        switchMap((params) =>
          this.recarregar.pipe(
            startWith(undefined),
            tap(() => {
              this.carregando.set(true);
              this.erro.set('');
            }),
            switchMap(() => {
              const n = Number(params.get('pagina') ?? 0);
              return this.api
                .catalogo(this.tipo, Number.isSafeInteger(n) && n >= 0 && n <= 1000000 ? n : 0)
                .pipe(
                  catchError((erro) => {
                    this.erro.set(mensagemOperacao(erro));
                    return of(null);
                  }),
                );
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((dados) => {
        this.dados.set(dados);
        this.carregando.set(false);
      });
  }
  pagina(pagina: number): void {
    if (this.salvando()) return;
    void this.router.navigate([], { relativeTo: this.route, queryParams: { pagina } });
  }
  editar(item: ItemCatalogo | null): void {
    this.item.set(item);
    this.excluir.set(null);
    this.editando.set(true);
    this.conflito.set(false);
    this.erro.set('');
    this.sucesso.set('');
    this.form.reset({
      nome: item && 'nome' in item ? item.nome : '',
      exigeArmazenamento: !!item && 'exigeArmazenamento' in item && item.exigeArmazenamento,
      exigeCor: !!item && 'exigeCor' in item && item.exigeCor,
      atributosObrigatorios:
        item && 'atributosObrigatorios' in item ? item.atributosObrigatorios.join(', ') : '',
      categoriaId: item && 'categoriaId' in item ? item.categoriaId : '',
      marcaId: item && 'marcaId' in item ? item.marcaId : '',
      modelo: item && 'modelo' in item ? item.modelo : '',
      descricao: item && 'descricao' in item ? (item.descricao ?? '') : '',
      imagemUrl: item && 'imagemUrl' in item ? (item.imagemUrl ?? '') : '',
      produtoId: item && 'produtoId' in item ? item.produtoId : '',
      armazenamentoGb: item && 'armazenamentoGb' in item ? String(item.armazenamentoGb ?? '') : '',
      cor: item && 'cor' in item ? (item.cor ?? '') : '',
      condicao: item && 'condicao' in item ? item.condicao : 'NOVO',
      atributos:
        item && 'atributos' in item
          ? Object.entries(item.atributos)
              .map(([k, v]) => `${k}=${v}`)
              .join('\n')
          : '',
    });
  }
  cancelar(): void {
    if (this.salvando()) return;
    this.editando.set(false);
    this.item.set(null);
    this.excluir.set(null);
    this.conflito.set(false);
    this.recarregar.next();
  }
  salvar(): void {
    if (this.salvando() || this.conflito()) return;
    const f = this.form.getRawValue(),
      versao = this.item()?.versao ?? 0;
    let dados: ItemCatalogo;
    if (
      (this.tipo !== 'variantes' && !f.nome.trim()) ||
      (this.tipo === 'produtos' && (!f.categoriaId || !f.marcaId || !f.modelo.trim())) ||
      (this.tipo === 'variantes' && !f.produtoId)
    ) {
      this.erro.set('Preencha o nome e as referências obrigatórias.');
      return;
    }
    if (this.tipo === 'categorias')
      dados = {
        nome: f.nome,
        exigeArmazenamento: f.exigeArmazenamento,
        exigeCor: f.exigeCor,
        atributosObrigatorios: f.atributosObrigatorios
          .split(',')
          .map((s) => s.trim())
          .filter(Boolean),
        versao,
      };
    else if (this.tipo === 'marcas') dados = { nome: f.nome, versao };
    else if (this.tipo === 'produtos')
      dados = {
        nome: f.nome,
        categoriaId: f.categoriaId,
        marcaId: f.marcaId,
        modelo: f.modelo,
        descricao: f.descricao || null,
        imagemUrl: f.imagemUrl || null,
        versao,
      };
    else {
      const atributos: Record<string, string> = {};
      for (const linha of f.atributos.split('\n').filter((s) => s.trim())) {
        const pos = linha.indexOf('=');
        const chave = linha.slice(0, pos).trim();
        if (pos < 1 || !linha.slice(pos + 1).trim() || Object.hasOwn(atributos, chave)) {
          this.erro.set('Use um atributo por linha, chave=valor, sem repetir chaves.');
          return;
        }
        atributos[chave] = linha.slice(pos + 1).trim();
      }
      if (
        f.armazenamentoGb &&
        (!/^[1-9][0-9]{0,9}$/.test(f.armazenamentoGb) || Number(f.armazenamentoGb) > 2147483647)
      ) {
        this.erro.set('Armazenamento deve ser inteiro positivo até 2147483647.');
        return;
      }
      dados = {
        produtoId: f.produtoId,
        armazenamentoGb: f.armazenamentoGb ? Number(f.armazenamentoGb) : null,
        cor: f.cor || null,
        condicao: f.condicao as Variante['condicao'],
        atributos,
        versao,
      };
    }
    this.salvando.set(true);
    this.erro.set('');
    this.api
      .salvarCatalogo(this.tipo, this.item()?.id ?? null, dados)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.salvando.set(false);
          this.editando.set(false);
          this.item.set(null);
          this.sucesso.set('Registro salvo.');
          this.recarregar.next();
        },
        error: (erro) => {
          this.salvando.set(false);
          this.erro.set(mensagemOperacao(erro));
          this.conflito.set(erro.status === 409 || erro.status === 404);
        },
      });
  }
  confirmarExclusao(): void {
    const item = this.excluir();
    if (!item?.id || this.salvando()) return;
    this.salvando.set(true);
    this.erro.set('');
    this.api
      .excluirCatalogo(this.tipo, item.id, item.versao)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.salvando.set(false);
          this.excluir.set(null);
          this.sucesso.set('Registro excluído.');
          this.recarregar.next();
        },
        error: (erro) => {
          this.salvando.set(false);
          this.excluir.set(null);
          this.erro.set(mensagemOperacao(erro));
        },
      });
  }
}
