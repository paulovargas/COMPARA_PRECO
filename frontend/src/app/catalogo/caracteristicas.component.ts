import { Component, input } from '@angular/core';
import { KeyValuePipe } from '@angular/common';
import { CONDICOES, Variante } from './catalogo.models';

@Component({
  selector: 'app-caracteristicas',
  imports: [KeyValuePipe],
  template: `<div class="caracteristicas" aria-label="Características da variante">
    <span>{{ condicoes[variante().condicao] }}</span>
    @if (variante().armazenamentoGb; as armazenamento) {
      <span>{{ armazenamento }} GB</span>
    }
    @if (variante().cor; as cor) {
      <span>{{ cor }}</span>
    }
    @for (atributo of variante().atributos | keyvalue; track atributo.key) {
      <span>{{ rotulo(atributo.key) }}: {{ atributo.value }}</span>
    }
  </div>`,
  styles: `
    .caracteristicas {
      display: flex;
      flex-wrap: wrap;
      gap: 0.4rem;
    }
    span {
      background: #f1f5f2;
      border: 1px solid #e2e9e3;
      padding: 0.35rem 0.6rem;
      border-radius: 0.35rem;
      font-size: 0.78rem;
      color: #43594f;
      overflow-wrap: anywhere;
    }
  `,
})
export class CaracteristicasComponent {
  readonly variante = input.required<Variante>();
  readonly condicoes = CONDICOES;
  rotulo(chave: string): string {
    return (
      ({ ram_gb: 'RAM (GB)', tela_polegadas: 'Tela (pol.)' } as Record<string, string>)[chave] ??
      chave.replaceAll('_', ' ')
    );
  }
}
