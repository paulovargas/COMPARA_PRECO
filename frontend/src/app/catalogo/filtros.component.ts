import { Component, effect, input, output } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  CAMPOS_FILTRO,
  CONDICOES,
  ESTADOS,
  FiltroCatalogo,
  OpcaoCatalogo,
} from './catalogo.models';

@Component({
  selector: 'app-filtros',
  imports: [ReactiveFormsModule],
  templateUrl: './filtros.component.html',
})
export class FiltrosComponent {
  readonly filtro = input.required<FiltroCatalogo>();
  readonly categorias = input<OpcaoCatalogo[]>([]);
  readonly marcas = input<OpcaoCatalogo[]>([]);
  readonly aplicar = output<Partial<FiltroCatalogo>>();
  readonly limpar = output<void>();
  readonly estados = ESTADOS;
  readonly condicoes = Object.entries(CONDICOES);
  readonly form = new FormGroup({
    texto: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(100)] }),
    categoriaId: new FormControl('', { nonNullable: true }),
    marcaId: new FormControl('', { nonNullable: true }),
    modelo: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(120)] }),
    armazenamentoGb: new FormControl('', {
      nonNullable: true,
      validators: [Validators.pattern(/^[1-9][0-9]*$/), Validators.max(2147483647)],
    }),
    cor: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(80)] }),
    condicao: new FormControl('', { nonNullable: true }),
    estado: new FormControl('', { nonNullable: true }),
    cidade: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(100)] }),
  });

  constructor() {
    effect(() => {
      const filtro = this.filtro();
      for (const campo of CAMPOS_FILTRO) {
        if (campo !== 'fornecedorId') this.form.controls[campo].setValue(filtro[campo] ?? '');
      }
      this.form.markAsPristine();
    });
  }

  temCategoria(): boolean {
    return this.categorias().some((c) => c.id === this.filtro().categoriaId);
  }
  temMarca(): boolean {
    return this.marcas().some((m) => m.id === this.filtro().marcaId);
  }
  quantidadeFiltros(): number {
    return CAMPOS_FILTRO.filter((campo) => campo !== 'texto' && !!this.filtro()[campo]).length;
  }

  buscar(): void {
    this.form.markAllAsTouched();
    if (this.form.valid) this.aplicar.emit(this.form.getRawValue());
  }
}
