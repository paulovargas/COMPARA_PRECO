import { Variante } from '../catalogo/catalogo.models';
export interface DadosEmpresa {
  razaoSocial: string;
  nomeFantasia: string;
  descricao: string | null;
  whatsapp: string;
  cidade: string;
  estado: string;
}
export interface Empresa extends DadosEmpresa {
  id: string;
  usuarioId: string;
  situacao: 'PENDENTE' | 'APROVADO' | 'BLOQUEADO';
  motivoModeracao: string | null;
  criadoEm: string;
  atualizadoEm: string;
  versao: number;
}
export interface OfertaFornecedor {
  id: string;
  fornecedorId: string;
  fornecedor: string;
  variante: Variante;
  preco: number;
  estoque: number;
  disponivel: boolean;
  moderacao: 'PENDENTE' | 'APROVADA' | 'BLOQUEADA';
  motivoModeracao: string | null;
  criadoEm: string;
  atualizadoEm: string;
  versao: number;
}
export interface DadosOferta {
  preco: number;
  estoque: number;
  disponivel: boolean;
}
export interface DadosSolicitacao {
  nomeProduto: string;
  marca: string;
  modelo: string;
  categoria: string;
  caracteristicas: string;
}
export interface SolicitacaoCatalogo extends DadosSolicitacao {
  id: string;
  situacao: 'PENDENTE' | 'ATENDIDA' | 'RECUSADA';
  motivo: string | null;
  varianteId: string | null;
  criadoEm: string;
}
