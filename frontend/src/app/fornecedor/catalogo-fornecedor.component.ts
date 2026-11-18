import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, combineLatest, of, startWith, Subject, switchMap } from 'rxjs';
import { Pagina, Variante } from '../catalogo/catalogo.models';
import { CaracteristicasComponent } from '../catalogo/caracteristicas.component';
import { PaginacaoComponent } from '../catalogo/paginacao.component';
import { FornecedorContext } from './fornecedor.context';
import { FornecedorService } from './fornecedor.service';
import { OfertaEditorComponent } from './oferta-editor.component';

@Component({
  selector: 'app-catalogo-fornecedor',
  imports: [
    RouterLink,
    ReactiveFormsModule,
    CaracteristicasComponent,
    PaginacaoComponent,
    OfertaEditorComponent,
  ],
  templateUrl: './catalogo-fornecedor.component.html',
})
export class CatalogoFornecedorComponent {
  readonly contexto = inject(FornecedorContext);
  private readonly service = inject(FornecedorService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly atualizar = new Subject<void>();
  readonly dados = signal<Pagina<Variante> | null>(null);
  readonly carregando = signal(true);
  readonly erro = signal('');
  readonly selecionada = signal<Variante | null>(null);
  readonly form = new FormGroup({
    texto: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(100)] }),
  });
  constructor() {
    combineLatest([this.route.queryParamMap, this.atualizar.pipe(startWith(undefined))])
      .pipe(
        switchMap(([query]) => {
          const texto = query.get('texto') ?? '';
          const pagina = Number(query.get('pagina') ?? 0);
          this.form.controls.texto.setValue(texto);
          this.selecionada.set(null);
          this.dados.set(null);
          this.erro.set('');
          if (
            texto.length > 100 ||
            /[\u0000-\u001f\u007f]/.test(texto) ||
            !Number.isInteger(pagina) ||
            pagina < 0 ||
            pagina > 1000000
          ) {
            this.erro.set('Confira os filtros do endereço e faça uma nova pesquisa.');
            return of(null);
          }
          this.carregando.set(true);
          return this.service.catalogo(texto, pagina).pipe(
            catchError(() => {
              this.erro.set(
                'Não foi possível carregar o catálogo. Faça uma nova pesquisa para tentar novamente.',
              );
              return of(null);
            }),
          );
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((dados) => {
        this.dados.set(dados);
        this.carregando.set(false);
      });
  }
  buscar(): void {
    this.form.markAllAsTouched();
    if (this.form.valid) this.navegar(0, this.form.controls.texto.value.trim());
  }
  pagina(pagina: number): void {
    this.navegar(pagina, this.route.snapshot.queryParamMap.get('texto') ?? '');
  }
  private navegar(pagina: number, texto: string): void {
    void this.router
      .navigate([], { relativeTo: this.route, queryParams: { texto, pagina } })
      .then((navegou) => {
        if (!navegou) this.atualizar.next();
      });
  }
  criada(): void {
    void this.router.navigate(['/fornecedor/ofertas'], { queryParams: { criada: 1 } });
  }
}
