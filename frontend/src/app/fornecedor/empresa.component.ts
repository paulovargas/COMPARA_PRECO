import { Component, computed, DestroyRef, effect, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { SessaoStore } from '../conta/sessao.store';
import { ESTADOS } from '../catalogo/catalogo.models';
import { FornecedorContext } from './fornecedor.context';
import { FornecedorService } from './fornecedor.service';
import { DadosEmpresa } from './fornecedor.models';
import { mensagemOperacao } from './validacao-oferta';

@Component({
  selector: 'app-empresa',
  imports: [ReactiveFormsModule],
  templateUrl: './empresa.component.html',
})
export class EmpresaComponent {
  readonly contexto = inject(FornecedorContext);
  private readonly service = inject(FornecedorService);
  private readonly sessao = inject(SessaoStore);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private versao = 0;
  readonly cadastro = computed(() => this.sessao.usuario()?.perfil === 'COMPRADOR');
  readonly podeEditar = computed(
    () =>
      this.cadastro() ||
      (!!this.contexto.empresa() &&
        this.contexto.empresa()?.situacao !== 'BLOQUEADO' &&
        !this.contexto.carregando() &&
        !this.contexto.erro()),
  );
  readonly estados = ESTADOS;
  readonly ocupado = signal(false);
  readonly erro = signal('');
  readonly conflito = signal(false);
  readonly sucesso = signal('');
  readonly form = new FormGroup({
    razaoSocial: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(3), Validators.maxLength(160)],
    }),
    nomeFantasia: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(3), Validators.maxLength(120)],
    }),
    descricao: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(2000)] }),
    whatsapp: new FormControl('', {
      nonNullable: true,
      validators: [
        Validators.required,
        (control) =>
          /^[1-9][0-9]{9,14}$/.test(String(control.value).replace(/[() .+\-]/g, ''))
            ? null
            : { whatsapp: true },
      ],
    }),
    cidade: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(2), Validators.maxLength(100)],
    }),
    estado: new FormControl('', {
      nonNullable: true,
      validators: [
        Validators.required,
        (control) => (ESTADOS.includes(control.value) ? null : { estado: true }),
      ],
    }),
  });
  constructor() {
    effect(() => {
      const empresa = this.contexto.empresa();
      if (empresa && !this.form.dirty) {
        this.form.reset({ ...empresa, descricao: empresa.descricao ?? '' });
        this.versao = empresa.versao;
      }
    });
  }
  salvar(): void {
    if (this.ocupado() || this.conflito() || !this.podeEditar()) return;
    for (const campo of ['razaoSocial', 'nomeFantasia', 'cidade'] as const)
      this.form.controls[campo].setValue(
        this.form.controls[campo].value.trim().replace(/\s+/g, ' '),
      );
    this.form.markAllAsTouched();
    this.erro.set('');
    this.sucesso.set('');
    if (this.form.invalid) return;
    const valor = this.form.getRawValue();
    const dados: DadosEmpresa = { ...valor, descricao: valor.descricao.trim() || null };
    const cadastro = this.cadastro();
    this.ocupado.set(true);
    (cadastro ? this.service.cadastrar(dados) : this.service.atualizar(dados, this.versao))
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.ocupado.set(false)),
      )
      .subscribe({
        next: (empresa) => {
          if (cadastro) {
            this.sessao.resolver(null);
            this.sessao.aviso.set(
              'Empresa cadastrada. Entre novamente para acompanhar a aprovação.',
            );
            void this.router.navigate(['/entrar'], { queryParams: { retorno: '/fornecedor' } });
          } else {
            this.form.reset({ ...empresa, descricao: empresa.descricao ?? '' });
            this.versao = empresa.versao;
            this.contexto.empresa.set(empresa);
            this.sucesso.set('Dados da empresa atualizados.');
          }
        },
        error: (erro: HttpErrorResponse) => {
          this.conflito.set(erro.status === 409 && erro.error?.codigo === 'REGISTRO_DESATUALIZADO');
          this.erro.set(mensagemOperacao(erro));
        },
      });
  }
  recarregar(): void {
    this.conflito.set(false);
    this.form.markAsPristine();
    this.erro.set('');
    this.sucesso.set('');
    this.contexto.carregar();
  }
}
