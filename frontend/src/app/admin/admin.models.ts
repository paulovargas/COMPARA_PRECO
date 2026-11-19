import { UsuarioAtual } from '../conta/sessao.store';
import { Variante } from '../catalogo/catalogo.models';
export type AreaAdmin = 'usuarios' | 'fornecedores' | 'ofertas' | 'solicitacoes';
export type TipoCatalogo = 'categorias' | 'marcas' | 'produtos' | 'variantes';
export interface UsuarioAdmin extends UsuarioAtual {
  versao: number;
}
export interface ItemAdmin {
  id: string;
  titulo: string;
  detalhes: string[];
  situacao: string;
  versao: number;
  acoes: string[];
  variante?: Variante;
  perfil?: UsuarioAtual['perfil'];
  ativo?: boolean;
}
export interface CategoriaAdmin {
  id?: string;
  nome: string;
  exigeArmazenamento: boolean;
  exigeCor: boolean;
  atributosObrigatorios: string[];
  versao: number;
}
export interface MarcaAdmin {
  id?: string;
  nome: string;
  versao: number;
}
export interface ProdutoAdmin {
  id?: string;
  categoriaId: string;
  marcaId: string;
  nome: string;
  modelo: string;
  descricao: string | null;
  imagemUrl: string | null;
  versao: number;
}
export interface VarianteAdmin {
  nomeProduto?: string;
  modelo?: string;
  marca?: string;
  id?: string;
  produtoId: string;
  armazenamentoGb: number | null;
  cor: string | null;
  condicao: Variante['condicao'];
  atributos: Record<string, string>;
  versao: number;
}
export type ItemCatalogo = CategoriaAdmin | MarcaAdmin | ProdutoAdmin | VarianteAdmin;
export interface Estatisticas {
  geradoEm: string;
  indicadores: { nome: string; quantidade: number }[];
  regioes: { uf: string; fornecedores: number; aprovados: number; ofertasElegiveis: number }[];
}
export interface Configuracao {
  aberturaContato: string;
  versao: number;
}
