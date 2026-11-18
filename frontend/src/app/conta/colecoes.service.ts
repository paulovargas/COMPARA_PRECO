import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject, tap } from 'rxjs';
import { SessaoStore } from './sessao.store';
import { Pagina } from '../catalogo/catalogo.models';

export type TipoColecao = 'produtos' | 'fornecedores';
export interface ProdutoFavorito {
  produtoId: string;
  nome: string;
  modelo: string;
  marcaId: string;
  marca: string;
  disponivel: boolean;
}
export interface FornecedorSeguido {
  fornecedorId: string;
  nome: string;
  cidade: string;
  estado: string;
  disponivel: boolean;
}
@Injectable({ providedIn: 'root' })
export class ColecoesService {
  private readonly http = inject(HttpClient);
  private readonly sessao = inject(SessaoStore);
  readonly alteracoes = new Subject<{
    tipo: TipoColecao;
    id: string;
    salvo: boolean;
    usuario: string | undefined;
  }>();
  caminho(tipo: TipoColecao): string {
    return tipo === 'produtos'
      ? '/api/conta/favoritos/produtos'
      : '/api/conta/fornecedores-seguidos';
  }
  marcado(tipo: TipoColecao, id: string): Observable<{ salvo: boolean }> {
    return this.http.get<{ salvo: boolean }>(`${this.caminho(tipo)}/${encodeURIComponent(id)}`);
  }
  alterar(tipo: TipoColecao, id: string, salvo: boolean): Observable<void> {
    const usuario = this.sessao.usuario()?.id;
    const url = `${this.caminho(tipo)}/${encodeURIComponent(id)}`;
    return (salvo ? this.http.put<void>(url, {}) : this.http.delete<void>(url)).pipe(
      tap(() => this.alteracoes.next({ tipo, id, salvo, usuario })),
    );
  }
  produtos(pagina: number): Observable<Pagina<ProdutoFavorito>> {
    return this.http.get<Pagina<ProdutoFavorito>>(this.caminho('produtos'), {
      params: { pagina, tamanho: 12 },
    });
  }
  fornecedores(pagina: number): Observable<Pagina<FornecedorSeguido>> {
    return this.http.get<Pagina<FornecedorSeguido>>(this.caminho('fornecedores'), {
      params: { pagina, tamanho: 12 },
    });
  }
}
