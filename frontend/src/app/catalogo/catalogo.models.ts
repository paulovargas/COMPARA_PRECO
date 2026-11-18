export interface Pagina<T> {
  itens: T[];
  pagina: number;
  tamanho: number;
  totalElementos: number;
  totalPaginas: number;
}

export interface Variante {
  id: string;
  produtoId: string;
  nomeProduto: string;
  modelo: string;
  marca: string;
  categoria: string;
  armazenamentoGb: number | null;
  cor: string | null;
  condicao: 'NOVO' | 'USADO' | 'RECONDICIONADO';
  atributos: Record<string, string>;
  completa: boolean;
  pendencias: string[];
}

export interface VarianteBusca {
  variante: Variante;
  imagemUrl: string | null;
  menorPreco: number;
  quantidadeOfertas: number;
}

export interface OfertaPublica {
  id: string;
  fornecedorId: string;
  fornecedor: string;
  cidade: string;
  estado: string;
  preco: number;
  estoque: number;
  atualizadoEm: string;
  menorPreco: boolean;
}

export interface Comparacao {
  variante: Variante;
  menorPreco: number | null;
  ofertas: Pagina<OfertaPublica>;
}

export interface OpcaoCatalogo {
  id: string;
  nome: string;
}

export interface FiltroCatalogo {
  texto?: string;
  categoriaId?: string;
  marcaId?: string;
  modelo?: string;
  armazenamentoGb?: string;
  cor?: string;
  condicao?: string;
  fornecedorId?: string;
  estado?: string;
  cidade?: string;
  pagina: number;
  tamanho: number;
}

export const CAMPOS_FILTRO = [
  'texto',
  'categoriaId',
  'marcaId',
  'modelo',
  'armazenamentoGb',
  'cor',
  'condicao',
  'fornecedorId',
  'estado',
  'cidade',
] as const;
export const ESTADOS = [
  'AC',
  'AL',
  'AP',
  'AM',
  'BA',
  'CE',
  'DF',
  'ES',
  'GO',
  'MA',
  'MT',
  'MS',
  'MG',
  'PA',
  'PB',
  'PR',
  'PE',
  'PI',
  'RJ',
  'RN',
  'RS',
  'RO',
  'RR',
  'SC',
  'SP',
  'SE',
  'TO',
];
export const CONDICOES: Record<string, string> = {
  NOVO: 'Novo',
  USADO: 'Usado',
  RECONDICIONADO: 'Recondicionado',
};
