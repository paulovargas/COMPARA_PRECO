import { Component, DestroyRef, inject, input, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AdminService } from './admin.service';
import { ItemCatalogo, TipoCatalogo } from './admin.models';
import { Pagina } from '../catalogo/catalogo.models';
import { PaginacaoComponent } from '../catalogo/paginacao.component';
import { mensagemOperacao } from '../fornecedor/validacao-oferta';
export function rotuloCatalogo(item: ItemCatalogo): string {
  return 'nome' in item
    ? item.nome
    : `${item.nomeProduto ?? 'Produto ' + item.produtoId} · ${item.marca ?? ''} ${item.modelo ?? ''} · ${item.armazenamentoGb ?? '—'} GB · ${item.cor ?? 'sem cor'} · ${item.condicao} · ${Object.entries(
        item.atributos,
      )
        .map(([k, v]) => `${k}: ${v}`)
        .join(', ')} · Produto ${item.produtoId}`;
}
@Component({
  selector: 'app-referencia-catalogo',
  imports: [PaginacaoComponent],
  template: ` <div class="referencia">
    <p>Referência: {{ tipo() }} · {{ selecionada() || valor() || 'Nenhuma selecionada' }}</p>
    <button class="botao" type="button" [disabled]="carregando()" (click)="carregar(0)">
      Escolher {{ tipo() }}
    </button>
    @if (erro()) {
      <p class="aviso" role="alert">{{ erro() }}</p>
    }
    @if (dados(); as pagina) {
      <div class="opcoes-referencia">
        @for (item of pagina.itens; track item.id) {
          <button class="botao opcao-referencia" type="button" (click)="selecionar(item)">
            {{ rotulo(item) }}
          </button>
        } @empty {
          <p>Nenhum registro. Cadastre no catálogo e retorne.</p>
        }
      </div>
      <app-paginacao
        [pagina]="pagina.pagina"
        [totalPaginas]="pagina.totalPaginas"
        (mudar)="carregar($event)"
      />
    }
  </div>`,
})
export class ReferenciaCatalogoComponent {
  readonly tipo = input.required<TipoCatalogo>();
  readonly valor = input('');
  readonly escolher = output<string>();
  private readonly api = inject(AdminService);
  private readonly destroyRef = inject(DestroyRef);
  readonly dados = signal<Pagina<ItemCatalogo> | null>(null);
  readonly carregando = signal(false);
  readonly erro = signal('');
  readonly rotulo = rotuloCatalogo;
  readonly selecionada = signal('');
  carregar(pagina: number): void {
    if (this.carregando()) return;
    this.carregando.set(true);
    this.erro.set('');
    this.api
      .catalogo(this.tipo(), pagina)
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
  selecionar(item: ItemCatalogo): void {
    if (item.id) {
      this.escolher.emit(item.id);
      this.selecionada.set(rotuloCatalogo(item));
    }
    this.dados.set(null);
  }
}
