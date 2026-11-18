import { computed, DestroyRef, inject, Injectable, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { Empresa } from './fornecedor.models';
import { FornecedorService } from './fornecedor.service';

@Injectable()
export class FornecedorContext {
  private readonly service = inject(FornecedorService);
  private readonly destroyRef = inject(DestroyRef);
  readonly empresa = signal<Empresa | null>(null);
  readonly carregando = signal(false);
  readonly erro = signal('');
  readonly aprovado = computed(
    () => this.empresa()?.situacao === 'APROVADO' && !this.carregando() && !this.erro(),
  );
  carregar(): void {
    if (this.carregando()) return;
    this.carregando.set(true);
    this.erro.set('');
    this.service
      .perfil()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.carregando.set(false)),
      )
      .subscribe({
        next: (empresa) => this.empresa.set(empresa),
        error: () =>
          this.erro.set('Não foi possível carregar os dados da empresa. Tente novamente.'),
      });
  }
}
