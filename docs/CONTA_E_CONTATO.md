# Conta, favoritos, fornecedores seguidos e WhatsApp

## Interface

- `/cadastro`: cria uma conta de comprador. Após sucesso, é necessário entrar em `/entrar`.
- `/entrar`: login com email/senha e retorno à página de origem. Somente destinos internos são aceitos.
- “Minha lista” (`/minha-lista`) reúne produtos favoritos e fornecedores seguidos, com paginação e remoção. A rota exige login.
- “Favoritar produto” na busca/comparação salva o produto de catálogo. Todas as suas variantes compartilham a marcação. A lista abre a busca pelo modelo e marca para escolher uma variante; não compara características diferentes.
- “Seguir fornecedor” na comparação adiciona a empresa à lista. “Ver ofertas” abre a busca pelo fornecedor.
- Visitantes são encaminhados ao login antes de salvar. Ao voltar, clique novamente na ação desejada; nenhuma alteração é executada automaticamente durante o login.
- “Sair” encerra a sessão no servidor. Favoritos e fornecedores seguidos permanecem no banco para o próximo login.

A interface restaura a conta ao abrir e trata falhas de conexão, consulta de marcação, alteração e expiração. A expiração remove os dados privados da tela e pede novo login. Botões evitam envio duplicado; marcações repetidas do mesmo produto são sincronizadas na tela.

Senhas de cadastro precisam ter entre 12 e 64 caracteres e no máximo 72 bytes UTF-8, conforme BCrypt. Acentos/emojis podem atingir esse limite antes dos 64 caracteres. A aplicação não grava senhas, tokens ou listas privadas no localStorage. O navegador gerencia o cookie HttpOnly da sessão; a proteção CSRF é consultada antes de cada mutação. Falhas não repetem alterações automaticamente.

## API privada

Todos os endpoints abaixo exigem sessão válida. PUT/DELETE também exigem o cabeçalho retornado por `GET /api/auth/csrf`, conforme [AUTENTICACAO.md](AUTENTICACAO.md).

| Método | Rota | Resultado |
| --- | --- | --- |
| GET | `/api/conta/favoritos/produtos` | Página dos produtos da conta |
| GET | `/api/conta/favoritos/produtos/{produtoId}` | `{ "salvo": true/false }` |
| PUT | `/api/conta/favoritos/produtos/{produtoId}` | Favoritar, 204 |
| DELETE | `/api/conta/favoritos/produtos/{produtoId}` | Remover favorito, 204 |
| GET | `/api/conta/fornecedores-seguidos` | Página dos fornecedores da conta |
| GET | `/api/conta/fornecedores-seguidos/{fornecedorId}` | `{ "salvo": true/false }` |
| PUT | `/api/conta/fornecedores-seguidos/{fornecedorId}` | Seguir, 204 |
| DELETE | `/api/conta/fornecedores-seguidos/{fornecedorId}` | Deixar de seguir, 204 |

Listas aceitam `pagina` (0 a 1000000, padrão 0) e `tamanho` (1 a 50, padrão 20). O formato segue `itens`, `pagina`, `tamanho`, `totalElementos`, `totalPaginas`, com ordenação por criação decrescente e identificador para desempate. A interface usa páginas de 12 itens.

O proprietário sempre vem da sessão, sem escolha pelo payload. As operações estão disponíveis aos perfis autenticados COMPRADOR, FORNECEDOR e ADMIN. Chaves compostas impedem duplicação por usuário/recurso. PUT repetido e DELETE de item ausente retornam 204; alterar a lista de outra conta não é possível.

Para adicionar favorito, o produto precisa ter uma oferta elegível no momento da operação. Para seguir, a empresa precisa estar aprovada, com proprietário ativo e perfil FORNECEDOR. Recursos inexistentes ou indisponíveis retornam 404. Consultar a marcação de um identificador ausente retorna `salvo=false`.

Itens já salvos permanecem na lista se ficarem indisponíveis; PUT repetido continua idempotente. É possível removê-los. No produto, `disponivel` indica existência de oferta elegível; na empresa, indica aprovação e conta ativa com perfil FORNECEDOR, sem garantir estoque/ofertas atuais. Remover fisicamente produto/empresa/usuário também remove seus vínculos por chave estrangeira. As listas não expõem credenciais, email do fornecedor, motivo de moderação ou telefone.

## Contato público

`GET /api/catalogo/ofertas/{id}/contato` dispensa login e retorna `{ "url": "https://wa.me/...?..." }`, com `Cache-Control: no-store`. O servidor consulta a mesma view de elegibilidade da busca. Oferta inexistente, sem estoque, indisponível, bloqueada ou de fornecedor/conta inelegível retorna 404.

A mensagem inclui nome, marca/modelo, condição, armazenamento, cor e todos os atributos da variante; o texto é codificado em UTF-8 no parâmetro `text`. O telefone é o contato comercial normalizado da empresa. Nenhuma mensagem é enviada pelo servidor.

A interface primeiro consulta a disponibilidade do contato e depois mostra “Abrir WhatsApp”, em nova aba com `noopener noreferrer`. A abertura é uma ação explícita do visitante. O link reflete os dados do momento da consulta; condições comerciais devem ser confirmadas com o fornecedor. A aplicação não inclui checkout ou pagamento.

## Validação

```powershell
# Em backend, com JDK 25 e Docker Desktop disponíveis
.\mvnw.cmd -B -o verify
# Em frontend
npm.cmd run format:check
npm.cmd test -- --watch=false
npm.cmd run build
```

Backend: testes HTTP com PostgreSQL descartável verificam isolamento, idempotência, proprietário pelo servidor, listas indisponíveis, paginação, ausência de sessão/CSRF, bloqueio e mensagem/link de contato. Frontend: sessão, CSRF, formulários, retorno, bloqueio de envio duplicado, marcações, falhas e expiração.

Revisão visual local no Edge com 1440×1000 e 390×844: bootstrap anônimo contra a API real; fluxos preenchidos com respostas HTTP simuladas no navegador, sem criar contas ou ofertas no banco local e sem abrir/enviar mensagens ao WhatsApp.
