import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-paginacao',
  template: `@if (totalPaginas() > 1) {
    <nav class="paginacao" aria-label="Paginação dos resultados">
      <button class="botao" [disabled]="pagina() === 0" (click)="mudar.emit(pagina() - 1)">
        ← Anterior
      </button>
      <span>Página {{ pagina() + 1 }} de {{ totalPaginas() }}</span>
      <button
        class="botao"
        [disabled]="pagina() + 1 >= totalPaginas()"
        (click)="mudar.emit(pagina() + 1)"
      >
        Próxima →
      </button>
    </nav>
  }`,
  styles: `
    .paginacao {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 1rem;
      flex-wrap: wrap;
      margin: 2rem 0;
    }
    .paginacao span {
      font-size: 0.85rem;
      color: #52665e;
    }
  `,
})
export class PaginacaoComponent {
  readonly pagina = input.required<number>();
  readonly totalPaginas = input.required<number>();
  readonly mudar = output<number>();
}
