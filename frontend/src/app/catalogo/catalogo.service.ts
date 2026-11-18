import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CAMPOS_FILTRO,
  Comparacao,
  FiltroCatalogo,
  OpcaoCatalogo,
  Pagina,
  VarianteBusca,
} from './catalogo.models';

@Injectable({ providedIn: 'root' })
export class CatalogoService {
  private readonly http = inject(HttpClient);

  buscar(filtro: FiltroCatalogo): Observable<Pagina<VarianteBusca>> {
    return this.http.get<Pagina<VarianteBusca>>('/api/catalogo/variantes', {
      params: this.parametros(filtro),
    });
  }

  comparar(id: string, filtro: FiltroCatalogo): Observable<Comparacao> {
    return this.http.get<Comparacao>(`/api/catalogo/variantes/${encodeURIComponent(id)}/ofertas`, {
      params: this.parametros(filtro),
    });
  }

  categorias(): Observable<OpcaoCatalogo[]> {
    return this.http.get<OpcaoCatalogo[]>('/api/catalogo/categorias');
  }
  marcas(): Observable<OpcaoCatalogo[]> {
    return this.http.get<OpcaoCatalogo[]>('/api/catalogo/marcas');
  }

  private parametros(filtro: FiltroCatalogo): HttpParams {
    let params = new HttpParams().set('pagina', filtro.pagina).set('tamanho', filtro.tamanho);
    for (const campo of CAMPOS_FILTRO) {
      const valor = filtro[campo]?.trim();
      if (valor) params = params.set(campo, valor);
    }
    return params;
  }
}
