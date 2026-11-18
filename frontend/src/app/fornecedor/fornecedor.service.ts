import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Pagina, Variante } from '../catalogo/catalogo.models';
import {
  DadosEmpresa,
  DadosOferta,
  DadosSolicitacao,
  Empresa,
  OfertaFornecedor,
  SolicitacaoCatalogo,
} from './fornecedor.models';

@Injectable({ providedIn: 'root' })
export class FornecedorService {
  private readonly http = inject(HttpClient);
  perfil(): Observable<Empresa> {
    return this.http.get<Empresa>('/api/fornecedor/perfil');
  }
  cadastrar(empresa: DadosEmpresa): Observable<Empresa> {
    return this.http.post<Empresa>('/api/fornecedores', empresa);
  }
  atualizar(empresa: DadosEmpresa, versao: number): Observable<Empresa> {
    return this.http.put<Empresa>('/api/fornecedor/perfil', { empresa, versao });
  }
  ofertas(pagina: number): Observable<Pagina<OfertaFornecedor>> {
    return this.http.get<Pagina<OfertaFornecedor>>('/api/fornecedor/ofertas', {
      params: { pagina, tamanho: 12 },
    });
  }
  criarOferta(varianteId: string, dados: DadosOferta): Observable<OfertaFornecedor> {
    return this.http.post<OfertaFornecedor>('/api/fornecedor/ofertas', { varianteId, ...dados });
  }
  atualizarOferta(id: string, dados: DadosOferta, versao: number): Observable<OfertaFornecedor> {
    return this.http.put<OfertaFornecedor>(`/api/fornecedor/ofertas/${encodeURIComponent(id)}`, {
      ...dados,
      versao,
    });
  }
  excluirOferta(id: string, versao: number): Observable<void> {
    return this.http.delete<void>(`/api/fornecedor/ofertas/${encodeURIComponent(id)}`, {
      params: { versao },
    });
  }
  catalogo(texto: string, pagina: number): Observable<Pagina<Variante>> {
    return this.http.get<Pagina<Variante>>('/api/fornecedor/catalogo/variantes', {
      params: { texto, pagina, tamanho: 12 },
    });
  }
  solicitacoes(pagina: number): Observable<Pagina<SolicitacaoCatalogo>> {
    return this.http.get<Pagina<SolicitacaoCatalogo>>('/api/fornecedor/catalogo/solicitacoes', {
      params: { pagina, tamanho: 12 },
    });
  }
  solicitar(dados: DadosSolicitacao): Observable<SolicitacaoCatalogo> {
    return this.http.post<SolicitacaoCatalogo>('/api/fornecedor/catalogo/solicitacoes', dados);
  }
}
