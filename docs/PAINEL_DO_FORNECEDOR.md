# Painel do fornecedor

## Acesso e cadastro

Com API e frontend iniciados conforme README, abra http://127.0.0.1:4200. Entre com uma conta comum e use “Quero vender” no cabeçalho. Fornecedores veem “Painel do fornecedor”. Administradores seguem o fluxo administrativo separado.

- `/fornecedor`: cadastro ou perfil da empresa.
- `/fornecedor/ofertas`: ofertas da empresa autenticada.
- `/fornecedor/catalogo`: consulta de variantes e criação de oferta.
- `/fornecedor/solicitacoes`: solicitação de inclusão/revisão e histórico próprio.

O cadastro informa razão social, nome fantasia, descrição opcional, telefone comercial com país/DDD, cidade e UF. Uma conta possui uma empresa. O servidor define o proprietário e o perfil, invalida as sessões e exige novo login. A interface encaminha para entrar novamente; a empresa começa pendente de aprovação.

## Situação e perfil

O painel mostra a situação da empresa e o motivo de bloqueio, quando presente. “Atualizar situação” consulta os dados atuais.

- PENDENTE: perfil editável e demais áreas disponíveis para consulta; ofertas e novas solicitações aguardam aprovação.
- APROVADO: permite gerenciar ofertas e enviar solicitações, desde que a conta esteja ativa.
- BLOQUEADO: somente consulta, inclusive perfil e histórico. A API também impede alterações.

Mudar razão social ou nome fantasia de empresa aprovada exige nova análise e retira ofertas da busca até a reaprovação. Alterações de contato/descrição preservam a aprovação. Os formulários explicam essa regra antes de salvar.

Se outra sessão alterar os mesmos dados, a API retorna conflito. A edição digitada é mantida e novo envio fica bloqueado até recarregar explicitamente. “Descartar edição e recarregar dados” recupera a versão atual. Uma atualização de situação não substitui silenciosamente campos já editados.

## Ofertas

1. Abra o catálogo e pesquise produto, marca ou modelo. Texto/página ficam na URL.
2. Confira armazenamento, cor, condição e atributos. Variantes incompletas não podem ser selecionadas para uma oferta.
3. Use “Selecionar variante” e informe preço, estoque e disponibilidade.
4. Após cadastrar, a oferta aparece em “Minhas ofertas”, pendente de moderação.

Preço aceita vírgula ou ponto decimal, até duas casas, sem separador de milhares: `6500,25` ou `6500.25`. Não há arredondamento silencioso de casas adicionais. Estoque é inteiro de 0 a 1.000.000.000. Estoque zero e indisponibilidade mantêm a oferta no painel, mas a retiram da busca pública.

Editar altera somente preço, estoque e disponibilidade, enviando a versão da leitura. Variante e moderação são preservadas; para outra variante, cadastre outra oferta. Oferta bloqueada pode ter preço/estoque editados por empresa aprovada, mas isso não a aprova automaticamente.

Excluir exige abrir a confirmação na tela e confirmar a oferta indicada. A operação envia a versão atual. Após conflito de versão, é necessário recarregar antes de repetir; não há repetição automática de mutações.

O painel usa paginação de 12 itens e cancela consultas antigas ao navegar. Erros de leitura/alteração têm feedback e possibilidade de nova tentativa. Sessão expirada retira o painel privado e encaminha para login.

## Produto ou variante ausente

Use “Solicitar inclusão” para informar nome, marca, modelo, categoria e características, incluindo armazenamento, cor, condição, RAM e outros detalhes necessários. Uma revisão de variante existente deve identificar o modelo e explicar as características ausentes.

A solicitação fica vinculada à empresa da sessão e inicia PENDENTE. Não cria automaticamente produto, variante ou oferta no catálogo compartilhado. Há limite de 10 pendentes por empresa. Envios pendentes iguais, ignorando maiúsculas e espaços, retornam conflito; empresas diferentes podem solicitar o mesmo item. Um bloqueio transacional da empresa protege limite/duplicidade também sob concorrência.

A listagem própria continua acessível à empresa pendente ou bloqueada. O painel administrativo permite recusar com motivo ou atender vinculando uma variante completa. O fornecedor pode localizar a variante correspondente no catálogo e cadastrar sua oferta. Importação CSV/XLSX também está disponível.

### API das solicitações

Sessão FORNECEDOR obrigatória; POST também exige CSRF e empresa aprovada.

```text
POST /api/fornecedor/catalogo/solicitacoes
GET /api/fornecedor/catalogo/solicitacoes?pagina=0&tamanho=20
```

```json
{
  "nomeProduto": "Produto solicitado",
  "marca": "Marca do produto",
  "modelo": "Modelo exato",
  "categoria": "Smartphones",
  "caracteristicas": "256 GB, preto, novo, RAM 8 GB"
}
```

Nome: 2–160 caracteres; marca/categoria: 2–100; modelo: 2–120; características: 10–2000. Espaços são normalizados. Resposta 201 contém os dados, ID, situação, motivo, eventual variante vinculada e criação. Proprietário e situação não são escolhidos pelo payload.

GET usa o formato de página padrão; página de 0 a 1000000, tamanho de 1 a 50. Só retorna solicitações da própria conta. Códigos de conflito: `SOLICITACAO_DUPLICADA` e `LIMITE_SOLICITACOES`. Conta comum não acessa essa API; empresa bloqueada recebe 403 ao tentar criar, e pendente recebe 409.

## Validação

No backend, com JDK 25 e Docker Desktop:

```powershell
.\mvnw.cmd -B -o verify
```

No frontend:

```powershell
npm.cmd run format:check
npm.cmd test -- --watch=false
npm.cmd run build
```

Os testes do backend usam PostgreSQL descartável e cobrem o CRUD já existente, autorização e solicitações, inclusive concorrência. Os testes Angular cobrem perfis, cadastro/novo login, estado da empresa, moeda/estoque, conflito, exclusão confirmada, catálogo e solicitações. Revisão no Edge em 1440×1000 e 390×844 usa respostas simuladas para os cenários preenchidos, sem criar empresas/ofertas no banco local.

## Importação em lote

O painel também permite importar CSV/XLSX com prévia e confirmação. No catálogo, copie o código da variante. Veja o [formato, limites e tratamento de conflitos](IMPORTACAO_DE_OFERTAS.md).

A análise administrativa de solicitações está disponível. A equipe pode vincular uma variante completa ou recusar com resposta; o histórico deste painel mostra a decisão e o código correspondente. Veja [Administração](ADMINISTRACAO.md).
