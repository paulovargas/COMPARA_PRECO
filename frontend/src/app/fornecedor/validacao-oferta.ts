import { HttpErrorResponse } from '@angular/common/http';
export function precoDecimal(valor: string): number | null {
  if (!/^(0|[1-9][0-9]{0,9})([,.][0-9]{1,2})?$/.test(valor.trim())) return null;
  const preco = Number(valor.trim().replace(',', '.'));
  return preco > 0 && preco <= 9999999999.99 ? preco : null;
}
export function estoqueInteiro(valor: string): number | null {
  return /^(0|[1-9][0-9]{0,9})$/.test(valor) && Number(valor) <= 1000000000 ? Number(valor) : null;
}
export function mensagemOperacao(erro: HttpErrorResponse): string {
  if (erro.status === 401) return 'Sua sessão expirou. Entre novamente para continuar.';
  if (erro.status === 403)
    return 'Esta conta ou empresa não pode realizar alterações. Atualize a situação da empresa.';
  const mensagens: Record<string, string> = {
    REGISTRO_DESATUALIZADO:
      'Os dados foram alterados em outra sessão. Sua edição foi mantida; descarte e recarregue os dados antes de tentar novamente.',
    FORNECEDOR_NAO_APROVADO:
      'A empresa precisa estar aprovada. Atualize a situação da empresa antes de continuar.',
    VARIANTE_INCOMPLETA:
      'Esta variante está incompleta. Selecione outra ou solicite a inclusão das características.',
    SOLICITACAO_DUPLICADA: 'Já existe uma solicitação pendente com estes dados.',
    LIMITE_SOLICITACOES:
      'Você já possui 10 solicitações pendentes. Aguarde a análise antes de enviar outras.',
  };
  if (mensagens[erro.error?.codigo]) return mensagens[erro.error.codigo];
  if (erro.status === 409)
    return 'Não foi possível salvar: o item já existe ou seus dados mudaram. Recarregue os dados para conferir.';
  if (erro.status === 400) return 'Confira os campos informados e tente novamente.';
  if (erro.status === 404) return 'O item não está mais disponível. Recarregue os dados.';
  return 'Não foi possível concluir a operação. Confira sua conexão e tente novamente.';
}
