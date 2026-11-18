import {
  afterNextRender,
  Component,
  DestroyRef,
  effect,
  ElementRef,
  inject,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { Variante } from '../catalogo/catalogo.models';
import { CaracteristicasComponent } from '../catalogo/caracteristicas.component';
import { FornecedorContext } from './fornecedor.context';
import { FornecedorService } from './fornecedor.service';
import { OfertaFornecedor } from './fornecedor.models';
import { estoqueInteiro, mensagemOperacao, precoDecimal } from './validacao-oferta';

@Component({
  selector: 'app-oferta-editor',
  imports: [ReactiveFormsModule, CaracteristicasComponent],
  template: `
    @let selecionada = oferta()?.variante ?? variante();
    @if (selecionada) {
      <form
        class="form-painel"
        [formGroup]="form"
        (ngSubmit)="salvar()"
        [attr.aria-busy]="ocupado()"
      >
        <h3>{{ oferta() ? 'Editar oferta' : 'Nova oferta' }} · {{ selecionada.nomeProduto }}</h3>
        <app-caracteristicas [variante]="selecionada" />
        <p class="descricao">
          A variante não muda durante a edição. Preço e estoque não alteram a moderação da oferta.
        </p>
        @if (erro()) {
          <p class="aviso" role="alert">{{ erro() }}</p>
        }
        <fieldset [disabled]="ocupado() || !contexto.aprovado()">
          <legend>Condições da oferta</legend>
          <div class="campos">
            <div>
              <label for="preco-oferta">Preço (R$)</label
              ><input
                #precoInput
                id="preco-oferta"
                formControlName="preco"
                inputmode="decimal"
                aria-describedby="ajuda-preco"
              /><small id="ajuda-preco"
                >Ex.: 6500,00. Use até 2 casas decimais, sem separador de milhares.</small
              >
              @if (form.controls.preco.touched && form.controls.preco.invalid) {
                <small class="erro-campo"
                  >Informe um preço positivo válido de até R$ 9.999.999.999,99.</small
                >
              }
            </div>
            <div>
              <label for="estoque-oferta">Estoque</label
              ><input id="estoque-oferta" formControlName="estoque" inputmode="numeric" />
              @if (form.controls.estoque.touched && form.controls.estoque.invalid) {
                <small class="erro-campo">Informe um inteiro de 0 a 1.000.000.000.</small>
              }
            </div>
            <label class="checkbox"
              ><input type="checkbox" formControlName="disponivel" />Oferta disponível</label
            >
          </div>
          <p class="descricao">
            Ofertas novas aguardam moderação. Sem estoque ou indisponíveis não aparecem na busca
            pública.
          </p>
          <button class="botao principal" type="submit" [disabled]="conflito()">
            {{ ocupado() ? 'Salvando…' : oferta() ? 'Salvar oferta' : 'Cadastrar oferta' }}
          </button>
        </fieldset>
        <button class="link-botao" type="button" [disabled]="ocupado()" (click)="cancelar.emit()">
          {{ erro() ? 'Cancelar edição e recarregar' : 'Cancelar' }}
        </button>
      </form>
    }
  `,
})
export class OfertaEditorComponent {
  readonly oferta = input<OfertaFornecedor | null>(null);
  readonly variante = input<Variante | null>(null);
  readonly salva = output<OfertaFornecedor>();
  readonly cancelar = output<void>();
  readonly contexto = inject(FornecedorContext);
  private readonly service = inject(FornecedorService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly campoPreco = viewChild<ElementRef<HTMLInputElement>>('precoInput');
  readonly ocupado = signal(false);
  readonly erro = signal('');
  readonly conflito = signal(false);
  readonly form = new FormGroup({
    preco: new FormControl('', {
      nonNullable: true,
      validators: [(control) => (precoDecimal(control.value) === null ? { preco: true } : null)],
    }),
    estoque: new FormControl('0', {
      nonNullable: true,
      validators: [
        (control) => (estoqueInteiro(control.value) === null ? { estoque: true } : null),
      ],
    }),
    disponivel: new FormControl(true, { nonNullable: true }),
  });
  constructor() {
    afterNextRender(() => this.campoPreco()?.nativeElement.focus());
    effect(() => {
      const oferta = this.oferta();
      if (oferta)
        this.form.reset({
          preco: oferta.preco.toFixed(2).replace('.', ','),
          estoque: String(oferta.estoque),
          disponivel: oferta.disponivel,
        });
    });
  }
  salvar(): void {
    if (this.ocupado() || this.conflito() || !this.contexto.aprovado()) return;
    this.form.markAllAsTouched();
    this.erro.set('');
    if (this.form.invalid) return;
    const valor = this.form.getRawValue();
    const oferta = this.oferta();
    const variante = this.variante();
    if (!oferta && (!variante || !variante.completa)) return;
    const dados = {
      preco: precoDecimal(valor.preco)!,
      estoque: estoqueInteiro(valor.estoque)!,
      disponivel: valor.disponivel,
    };
    this.ocupado.set(true);
    (oferta
      ? this.service.atualizarOferta(oferta.id, dados, oferta.versao)
      : this.service.criarOferta(variante!.id, dados)
    )
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.ocupado.set(false)),
      )
      .subscribe({
        next: (resultado) => this.salva.emit(resultado),
        error: (erro: HttpErrorResponse) => {
          this.conflito.set(erro.status === 409 && erro.error?.codigo === 'REGISTRO_DESATUALIZADO');
          this.erro.set(mensagemOperacao(erro));
        },
      });
  }
}
