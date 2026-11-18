import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs';
import { AutenticacaoService } from './autenticacao.service';
import { destinoInterno } from './conta.guard';

@Component({
  selector: 'app-conta-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './conta-form.component.html',
  styleUrl: './conta.component.scss',
})
export class ContaFormComponent {
  private readonly auth = inject(AutenticacaoService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  readonly cadastro = this.route.snapshot.data['cadastro'] === true;
  readonly retorno = destinoInterno(this.route.snapshot.queryParamMap.get('retorno'));
  readonly criada = this.route.snapshot.queryParamMap.get('criada') === '1';
  readonly ocupado = signal(false);
  readonly erro = signal('');
  readonly form = new FormGroup({
    nome: new FormControl('', {
      nonNullable: true,
      validators: this.cadastro
        ? [Validators.required, Validators.minLength(2), Validators.maxLength(100)]
        : [],
    }),
    email: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.email, Validators.maxLength(254)],
    }),
    senha: new FormControl('', {
      nonNullable: true,
      validators: this.cadastro
        ? [
            Validators.required,
            Validators.minLength(12),
            Validators.maxLength(64),
            (control) =>
              new TextEncoder().encode(control.value).length > 72 ? { bytes: true } : null,
          ]
        : [Validators.required],
    }),
  });
  enviar(): void {
    if (this.ocupado()) return;
    this.form.controls.email.setValue(this.form.controls.email.value.trim());
    this.form.controls.nome.setValue(this.form.controls.nome.value.trim().replace(/ +/g, ' '));
    this.form.markAllAsTouched();
    this.erro.set('');
    if (this.form.invalid) return;
    this.ocupado.set(true);
    const { nome, email, senha } = this.form.getRawValue();
    const request = this.cadastro
      ? this.auth.cadastrar(nome, email, senha)
      : this.auth.entrar(email, senha);
    request.pipe(finalize(() => this.ocupado.set(false))).subscribe({
      next: () => {
        this.form.controls.senha.reset();
        if (this.cadastro)
          void this.router.navigate(['/entrar'], {
            queryParams: { criada: 1, retorno: this.retorno },
          });
        else void this.router.navigateByUrl(this.retorno);
      },
      error: (erro: HttpErrorResponse) =>
        this.erro.set(
          erro.status === 401
            ? 'Email ou senha inválidos.'
            : erro.status === 409
              ? 'Este email já está cadastrado. Entre na sua conta.'
              : erro.status === 429
                ? 'Muitas tentativas. Aguarde alguns minutos antes de tentar novamente.'
                : erro.status === 400
                  ? 'Confira os dados informados e os requisitos da senha.'
                  : 'Não foi possível concluir. Confira sua conexão e tente novamente.',
        ),
    });
  }
}
