import { Component, DestroyRef, inject, input, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-contato',
  template: `@if (url()) {
      <a class="botao principal" [href]="url()" target="_blank" rel="noopener noreferrer"
        >Abrir WhatsApp ↗</a
      >
    } @else {
      <button class="botao principal" [disabled]="ocupado()" (click)="consultar()">
        {{ ocupado() ? 'Consultando…' : 'Consultar contato' }}
      </button>
    }
    @if (erro()) {
      <small role="alert">{{ erro() }}</small>
    }`,
  styles: `
    :host {
      display: block;
    }
    small {
      display: block;
      color: #a23220;
      font-size: 0.75rem;
      max-width: 240px;
      line-height: 1.4;
      margin-top: 0.5rem;
    }
  `,
})
export class ContatoComponent {
  readonly ofertaId = input.required<string>();
  private readonly http = inject(HttpClient);
  private readonly destroyRef = inject(DestroyRef);
  readonly url = signal('');
  readonly ocupado = signal(false);
  readonly erro = signal('');
  consultar(): void {
    if (this.ocupado()) return;
    this.ocupado.set(true);
    this.erro.set('');
    this.http
      .get<{ url: string }>(`/api/catalogo/ofertas/${encodeURIComponent(this.ofertaId())}/contato`)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.ocupado.set(false)),
      )
      .subscribe({
        next: (contato) => {
          if (/^https:\/\/wa\.me\/[1-9][0-9]{9,14}\?text=/.test(contato.url))
            this.url.set(contato.url);
          else this.erro.set('O contato não está disponível. Tente novamente.');
        },
        error: (erro: HttpErrorResponse) =>
          this.erro.set(
            erro.status === 404
              ? 'Esta oferta não está mais disponível para contato.'
              : 'Não foi possível consultar o contato. Tente novamente.',
          ),
      });
  }
}
