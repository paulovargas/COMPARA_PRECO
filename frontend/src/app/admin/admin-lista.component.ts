import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, startWith, switchMap, catchError, of, tap } from 'rxjs';
import { Pagina } from '../catalogo/catalogo.models';
import { CaracteristicasComponent } from '../catalogo/caracteristicas.component';
import { PaginacaoComponent } from '../catalogo/paginacao.component';
import { SessaoStore } from '../conta/sessao.store';
import { mensagemOperacao } from '../fornecedor/validacao-oferta';
import { AdminService } from './admin.service';
import { AreaAdmin, ItemAdmin } from './admin.models';
import { ReferenciaCatalogoComponent } from './referencia-catalogo.component';
@Component({
  selector: 'app-admin-lista',
  imports: [
    ReactiveFormsModule,
    PaginacaoComponent,
    CaracteristicasComponent,
    ReferenciaCatalogoComponent,
  ],
  templateUrl: './admin-lista.component.html',
})
export class AdminListaComponent {
  private readonly api = inject(AdminService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  readonly sessao = inject(SessaoStore);
  readonly area = this.route.snapshot.data['area'] as AreaAdmin;
  readonly titulo = {
    usuarios: 'Usuários e permissões',
    fornecedores: 'Moderação de fornecedores',
    ofertas: 'Moderação de ofertas',
    solicitacoes: 'Solicitações de catálogo',
  }[this.area];
  readonly dados = signal<Pagina<ItemAdmin> | null>(null);
  readonly erro = signal('');
  readonly sucesso = signal('');
  readonly carregando = signal(false);
  readonly salvando = signal(false);
  readonly conflito = signal(false);
  readonly selecionado = signal<ItemAdmin | null>(null);
  readonly form = inject(FormBuilder).nonNullable.group({
    destino: '',
    motivo: '',
    varianteId: '',
    ativo: true,
    perfil: 'COMPRADOR',
  });
  private readonly recarregar = new Subject<void>();
  readonly filtros =
    this.area === 'usuarios'
      ? []
      : this.area === 'fornecedores'
        ? ['PENDENTE', 'APROVADO', 'BLOQUEADO']
        : this.area === 'ofertas'
          ? ['PENDENTE', 'APROVADA', 'BLOQUEADA']
          : ['PENDENTE', 'ATENDIDA', 'RECUSADA'];
  readonly filtro = signal(this.route.snapshot.queryParamMap.get('situacao') ?? '');
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
              const pagina = Number(params.get('pagina') ?? 0);
              const situacao = params.get('situacao') ?? '';
              this.filtro.set(situacao);
              return this.api
                .listar(
                  this.area,
                  Number.isSafeInteger(pagina) && pagina >= 0 && pagina <= 1000000 ? pagina : 0,
                  situacao,
                )
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
  filtrar(evento: Event): void {
    this.pagina(0, (evento.target as HTMLSelectElement).value);
  }
  pagina(pagina: number, situacao = this.filtro()): void {
    if (this.salvando()) return;
    this.selecionado.set(null);
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { pagina, situacao: situacao || null },
    });
  }
  editar(item: ItemAdmin): void {
    if (this.salvando()) return;
    this.selecionado.set(item);
    this.conflito.set(false);
    this.erro.set('');
    this.sucesso.set('');
    this.form.reset({
      destino: item.acoes[0] ?? '',
      motivo: '',
      varianteId: '',
      ativo: item.ativo ?? true,
      perfil: item.perfil ?? 'COMPRADOR',
    });
  }
  cancelar(): void {
    if (this.salvando()) return;
    this.selecionado.set(null);
    this.conflito.set(false);
    this.recarregar.next();
  }
  salvar(): void {
    const item = this.selecionado();
    if (!item || this.salvando() || this.conflito()) return;
    const dados = this.form.getRawValue();
    const exigeMotivo = this.area === 'solicitacoes' || dados.destino.startsWith('BLOQUEAD');
    if (exigeMotivo && dados.motivo.trim().length < 3) {
      this.erro.set('Informe um motivo ou resposta com pelo menos 3 caracteres.');
      return;
    }
    if (this.area === 'solicitacoes' && dados.destino === 'ATENDIDA' && !dados.varianteId) {
      this.erro.set('Selecione a variante correspondente antes de atender.');
      return;
    }
    this.salvando.set(true);
    this.erro.set('');
    this.api
      .alterar(this.area, item, dados)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.salvando.set(false);
          this.selecionado.set(null);
          this.sucesso.set('Alteração concluída.');
          this.recarregar.next();
        },
        error: (erro) => {
          this.salvando.set(false);
          this.erro.set(mensagemOperacao(erro));
          this.conflito.set(erro.status === 409 || erro.status === 404);
        },
      });
  }
}
