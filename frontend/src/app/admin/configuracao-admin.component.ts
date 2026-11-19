import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AdminService } from './admin.service';
import { mensagemOperacao } from '../fornecedor/validacao-oferta';
@Component({
  selector: 'app-configuracao-admin',
  imports: [ReactiveFormsModule],
  template: `<h2>Configurações de contato</h2>
    <p>
      Defina a abertura da mensagem de interesse no WhatsApp. O sistema acrescenta o produto e suas
      características; nenhum envio é automático.
    </p>
    @if (erro()) {
      <p class="aviso" role="alert">{{ erro() }}</p>
    }
    @if (sucesso()) {
      <p role="status">Configuração salva.</p>
    }
    @if (carregando()) {
      <p role="status">Carregando configuração…</p>
    }
    <form [formGroup]="form" (ngSubmit)="salvar()">
      <fieldset [disabled]="carregando() || salvando() || conflito() || versao() === null">
        <label for="abertura-admin">Abertura da mensagem</label
        ><textarea
          id="abertura-admin"
          formControlName="aberturaContato"
          rows="3"
          maxlength="160"
        ></textarea>
        <p>
          Prévia: {{ form.controls.aberturaContato.value }} [produto e características]. A oferta
          ainda está disponível?
        </p>
        <button class="botao principal" type="submit">
          {{ salvando() ? 'Salvando…' : 'Salvar configuração' }}
        </button>
      </fieldset>
      <button
        class="link-botao"
        type="button"
        [disabled]="salvando() || carregando()"
        (click)="carregar()"
      >
        Descartar e recarregar configuração
      </button>
    </form>`,
})
export class ConfiguracaoAdminComponent {
  private readonly api = inject(AdminService);
  private readonly destroyRef = inject(DestroyRef);
  readonly form = inject(FormBuilder).nonNullable.group({
    aberturaContato: [
      '',
      [Validators.required, Validators.minLength(3), Validators.maxLength(160)],
    ],
  });
  readonly versao = signal<number | null>(null);
  readonly carregando = signal(false);
  readonly salvando = signal(false);
  readonly conflito = signal(false);
  readonly sucesso = signal(false);
  readonly erro = signal('');
  constructor() {
    this.carregar();
  }
  carregar(): void {
    if (this.carregando() || this.salvando()) return;
    this.carregando.set(true);
    this.erro.set('');
    this.sucesso.set(false);
    this.api
      .configuracao()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (dados) => {
          this.form.reset({ aberturaContato: dados.aberturaContato });
          this.versao.set(dados.versao);
          this.conflito.set(false);
          this.carregando.set(false);
        },
        error: (erro) => {
          this.erro.set(mensagemOperacao(erro));
          this.carregando.set(false);
        },
      });
  }
  salvar(): void {
    const versao = this.versao();
    if (this.salvando() || this.carregando() || this.conflito() || versao === null) return;
    if (this.form.invalid || this.form.controls.aberturaContato.value.trim().length < 3) {
      this.erro.set('Informe de 3 a 160 caracteres.');
      return;
    }
    this.salvando.set(true);
    this.erro.set('');
    this.sucesso.set(false);
    this.api
      .configurar({ ...this.form.getRawValue(), versao })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (dados) => {
          this.versao.set(dados.versao);
          this.form.reset({ aberturaContato: dados.aberturaContato });
          this.salvando.set(false);
          this.sucesso.set(true);
        },
        error: (erro) => {
          this.erro.set(mensagemOperacao(erro));
          this.conflito.set(erro.status === 409);
          this.salvando.set(false);
        },
      });
  }
}
