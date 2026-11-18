import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { finalize } from 'rxjs';
import { AutenticacaoService } from './conta/autenticacao.service';

@Component({
  selector: 'app-root',
  imports: [RouterLink, RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent {
  readonly auth = inject(AutenticacaoService);
  private readonly router = inject(Router);
  readonly saindo = signal(false);
  readonly erroSaida = signal('');
  constructor() {
    this.carregarSessao();
  }
  carregarSessao(): void {
    this.auth.carregar().subscribe({ error: () => {} });
  }
  retorno(): string {
    return this.router.url;
  }
  sair(): void {
    if (this.saindo()) return;
    this.saindo.set(true);
    this.erroSaida.set('');
    this.auth
      .sair()
      .pipe(finalize(() => this.saindo.set(false)))
      .subscribe({
        next: () => {
          void this.router.navigate(['/']);
        },
        error: () => this.erroSaida.set('Não foi possível encerrar a sessão. Tente novamente.'),
      });
  }
}
