import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable } from 'rxjs';
import { Pagina } from '../catalogo/catalogo.models';
import { Empresa, OfertaFornecedor, SolicitacaoCatalogo } from '../fornecedor/fornecedor.models';
import {
  AreaAdmin,
  Configuracao,
  Estatisticas,
  ItemAdmin,
  ItemCatalogo,
  TipoCatalogo,
  UsuarioAdmin,
} from './admin.models';

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);
  listar(area: AreaAdmin, pagina: number, situacao: string): Observable<Pagina<ItemAdmin>> {
    const params: Record<string, string | number> = { pagina, tamanho: 12 };
    if (situacao && area !== 'usuarios')
      params[area === 'ofertas' ? 'moderacao' : 'situacao'] = situacao;
    const url = `/api/admin/${area}`;
    if (area === 'usuarios')
      return this.http.get<Pagina<UsuarioAdmin>>(url, { params }).pipe(
        map((p) => ({
          ...p,
          itens: p.itens.map((u) => ({
            id: u.id,
            titulo: u.nome,
            detalhes: [u.email, u.perfil],
            situacao: u.ativo ? 'ATIVO' : 'BLOQUEADO',
            versao: u.versao,
            perfil: u.perfil,
            ativo: u.ativo,
            acoes: ['ACESSO'],
          })),
        })),
      );
    if (area === 'fornecedores')
      return this.http.get<Pagina<Empresa>>(url, { params }).pipe(
        map((p) => ({
          ...p,
          itens: p.itens.map((e) => ({
            id: e.id,
            titulo: e.nomeFantasia,
            detalhes: [
              e.razaoSocial,
              `${e.cidade} / ${e.estado}`,
              e.descricao ?? '',
              e.motivoModeracao ?? '',
            ],
            situacao: e.situacao,
            versao: e.versao,
            acoes:
              e.situacao === 'PENDENTE'
                ? ['APROVADO', 'BLOQUEADO']
                : e.situacao === 'APROVADO'
                  ? ['BLOQUEADO']
                  : ['PENDENTE'],
          })),
        })),
      );
    if (area === 'ofertas')
      return this.http.get<Pagina<OfertaFornecedor>>(url, { params }).pipe(
        map((p) => ({
          ...p,
          itens: p.itens.map((o) => ({
            id: o.id,
            titulo: o.variante.nomeProduto,
            detalhes: [
              o.fornecedor,
              `R$ ${o.preco.toFixed(2).replace('.', ',')} · Estoque: ${o.estoque} · ${o.disponivel ? 'Disponível' : 'Indisponível'}`,
              o.motivoModeracao ?? '',
            ],
            situacao: o.moderacao,
            versao: o.versao,
            variante: o.variante,
            acoes:
              o.moderacao === 'PENDENTE'
                ? ['APROVADA', 'BLOQUEADA']
                : o.moderacao === 'APROVADA'
                  ? ['BLOQUEADA']
                  : ['PENDENTE'],
          })),
        })),
      );
    return this.http
      .get<
        Pagina<{
          solicitacao: SolicitacaoCatalogo;
          fornecedor: string;
          fornecedorId: string;
          versao: number;
        }>
      >(url, { params })
      .pipe(
        map((p) => ({
          ...p,
          itens: p.itens.map(({ solicitacao: s, fornecedor, versao }) => ({
            id: s.id,
            titulo: s.nomeProduto,
            detalhes: [
              fornecedor,
              `${s.marca} · ${s.modelo} · ${s.categoria}`,
              s.caracteristicas,
              s.motivo ?? '',
              s.varianteId ? `Variante vinculada: ${s.varianteId}` : '',
            ],
            situacao: s.situacao,
            versao,
            acoes: s.situacao === 'PENDENTE' ? ['ATENDIDA', 'RECUSADA'] : [],
          })),
        })),
      );
  }
  alterar(
    area: AreaAdmin,
    item: ItemAdmin,
    dados: { destino: string; motivo: string; varianteId: string; ativo: boolean; perfil: string },
  ): Observable<unknown> {
    const url = `/api/admin/${area}/${encodeURIComponent(item.id)}`;
    if (area === 'usuarios')
      return this.http.put(`${url}/acesso`, {
        ativo: dados.ativo,
        perfil: dados.perfil,
        versao: item.versao,
      });
    if (area === 'solicitacoes')
      return this.http.put(`${url}/resolucao`, {
        situacao: dados.destino,
        motivo: dados.motivo,
        varianteId: dados.destino === 'ATENDIDA' ? dados.varianteId : null,
        versao: item.versao,
      });
    return this.http.patch(`${url}/${area === 'ofertas' ? 'moderacao' : 'situacao'}`, {
      [area === 'ofertas' ? 'moderacao' : 'situacao']: dados.destino,
      motivo: dados.motivo || null,
      versao: item.versao,
    });
  }
  catalogo(tipo: TipoCatalogo, pagina: number): Observable<Pagina<ItemCatalogo>> {
    return this.http.get<Pagina<ItemCatalogo>>(`/api/admin/catalogo/${tipo}`, {
      params: { pagina, tamanho: 12 },
    });
  }
  salvarCatalogo(
    tipo: TipoCatalogo,
    id: string | null,
    dados: ItemCatalogo,
  ): Observable<ItemCatalogo> {
    const url = `/api/admin/catalogo/${tipo}`;
    return id
      ? this.http.put<ItemCatalogo>(`${url}/${encodeURIComponent(id)}`, dados)
      : this.http.post<ItemCatalogo>(url, dados);
  }
  excluirCatalogo(tipo: TipoCatalogo, id: string, versao: number): Observable<void> {
    return this.http.delete<void>(`/api/admin/catalogo/${tipo}/${encodeURIComponent(id)}`, {
      params: { versao },
    });
  }
  estatisticas(): Observable<Estatisticas> {
    return this.http.get<Estatisticas>('/api/admin/estatisticas');
  }
  configuracao(): Observable<Configuracao> {
    return this.http.get<Configuracao>('/api/admin/configuracoes');
  }
  configurar(dados: Configuracao): Observable<Configuracao> {
    return this.http.put<Configuracao>('/api/admin/configuracoes', dados);
  }
}
