import { Variante } from '../catalogo/catalogo.models';

export interface LinhaImportacao {
  numero: number;
  varianteId: string;
  preco: string;
  estoque: string;
  disponivel: string;
  variante: Variante | null;
  acao: 'CRIAR' | 'ATUALIZAR' | 'IGNORAR' | 'INVALIDA';
  erros: string[];
}
export interface Importacao {
  id: string;
  situacao: 'PREVIA' | 'APLICADA';
  expiraEm: string;
  criar: number;
  atualizar: number;
  ignorar: number;
  invalidas: number;
  linhas: LinhaImportacao[];
}
