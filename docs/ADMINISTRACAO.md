# Painel administrativo

Acesse <http://localhost:4200/admin> com perfil **ADMIN**. O cabeçalho mostra o link **Administração** somente para esse perfil. Guardas protegem a navegação e todas as APIs `/api/admin/**` exigem ADMIN no servidor. Escritas exigem CSRF de `/api/auth/csrf`.

## Primeiro acesso

Não há senha administrativa fixa nem administrador nas cargas demonstrativas. Use o [bootstrap por ambiente](AUTENTICACAO.md) para criar o primeiro administrador. O bootstrap não promove email já cadastrado e não cria outro administrador se já existir um. Senhas precisam respeitar os limites do cadastro.

No terminal do backend, configure `APP_ADMIN_HABILITADO=true`, `APP_ADMIN_NOME`, `APP_ADMIN_EMAIL` e `APP_ADMIN_SENHA`, e execute:

```powershell
.\mvnw.cmd -B spring-boot:run "-Dspring-boot.run.profiles=demo"
```

Remova as variáveis de bootstrap do terminal depois de encerrar o processo. Nunca versione senha ou arquivo `.env`. O perfil demo é indicado para o banco local já populado; ambientes sem demonstração usam o perfil padrão.

## Usuários e permissões

- Lista paginada com identidade, perfil e situação. A edição só grava ao clicar **Confirmar alteração**.
- Perfis disponíveis: COMPRADOR, FORNECEDOR e ADMIN. FORNECEDOR só pode ser atribuído a quem já cadastrou uma empresa. Alterar o perfil do dono de uma empresa pode retirar suas ofertas da busca; o vínculo com a empresa é preservado.
- Alterar acesso ou bloquear invalida sessões da conta afetada. Reativação não recupera sessões antigas; exige login.
- É proibido bloquear a própria conta ou retirar o próprio perfil ADMIN. Alterações de acesso são serializadas, e o autor é revalidado depois de aguardar; dois administradores não conseguem bloquear um ao outro e deixar a plataforma sem administrador ativo.
- O painel envia a versão lida. Conflitos preservam a edição e exigem **Descartar e recarregar**. O endpoint anterior PATCH de situação permanece compatível; o painel utiliza PUT versionado de acesso.

## Fornecedores e ofertas

As listas têm filtro de situação e paginação. Escolha **Revisar decisão**, confira o registro e confirme. Bloquear exige motivo de pelo menos três caracteres.

| Registro | Transições |
| --- | --- |
| Fornecedor pendente | Aprovar ou bloquear |
| Fornecedor aprovado | Bloquear |
| Fornecedor bloqueado | Retornar para pendente, depois aprovar |
| Oferta pendente | Aprovar ou bloquear |
| Oferta aprovada | Bloquear |
| Oferta bloqueada | Retornar para pendente, depois aprovar |

Aprovar uma oferta exige empresa aprovada, dono ativo com perfil fornecedor e variante completa. Aprovação da oferta não garante publicação: disponibilidade, estoque e todas as regras de elegibilidade continuam aplicadas. Moderação não modifica preço/estoque; alterações usam a versão recebida.

## Catálogo

Abas **categorias**, **marcas**, **produtos** e **variantes** permitem criar, editar e excluir. Referências são escolhidas em listas paginadas; a escolha não fica restrita à primeira página.

- Categoria: nome, exigência de armazenamento/cor e até oito chaves de atributos obrigatórios.
- Marca: nome.
- Produto: nome, modelo, categoria, marca, descrição e URL HTTP(S) da imagem. A aplicação armazena a URL; não faz upload de imagens nesta etapa.
- Variante: produto, armazenamento inteiro positivo quando informado, cor, condição e até oito atributos `chave=valor`, um por linha. Chaves seguem `[a-z][a-z0-9_]{0,31}`, sem `cor`, `condicao` ou `armazenamento_gb`; valores até 64 caracteres. Características são normalizadas pelas mesmas regras do catálogo existente.
- Variantes incompletas podem ser cadastradas para posterior revisão, mas não recebem ofertas nem atendem solicitações enquanto incompletas.
- Todas as edições e exclusões conferem versão. Nomes/identidades equivalentes continuam sujeitos às restrições de unicidade do banco.
- Categoria com produtos não pode mudar suas regras obrigatórias. Produto com variantes não pode mudar categoria, marca ou modelo; nome, descrição e imagem podem ser corrigidos.
- Variante com ofertas ou vinculada a solicitação atendida não pode ser alterada. Crie outra variante para características diferentes.
- Exclusão exige confirmação explícita e é recusada quando há referências. Nenhuma exclusão administrativa apaga ofertas/favoritos/solicitações em cascata.
- Operações administrativas do catálogo são serializadas. A edição de variante e a criação/importação de ofertas usam bloqueios compatíveis; uma prévia de importação cuja variante mudou exige nova revisão.

## Solicitações de catálogo

Lista com fornecedor, dados descritivos, situação e resposta. Para **ATENDIDA**, selecione explicitamente uma variante completa e informe a resposta. Se ela ainda não existir, cadastre produto/variante no catálogo e volte à solicitação. Confira modelo, marca, armazenamento, cor, condição e atributos: não há correspondência automática por título.

Para **RECUSADA**, informe o motivo e não vincule variante. Somente solicitações pendentes podem ser finalizadas, uma única vez, com versão. A resposta e o código da variante aparecem no histórico do fornecedor. Não há criação automática de oferta ou envio de email/WhatsApp.

## Indicadores, relatório e configuração

A visão geral mostra uma fotografia consistente do banco, com horário de geração: contas, empresas por situação, ofertas por moderação e elegibilidade, produtos, variantes e solicitações pendentes. O CSV agrega fornecedores, aprovados e ofertas elegíveis por UF. Conta empresas aprovadas independentemente do perfil/atividade do dono; ofertas elegíveis aplicam todos os critérios de publicação. Nenhum dado pessoal é exportado. Não há métricas históricas de vendas ou cliques.

A configuração permite alterar a abertura da mensagem de contato WhatsApp (3–160 caracteres, sem controles). O sistema acrescenta as características da oferta e a pergunta final; o contato público continua revalidando elegibilidade. A configuração tem versão e não repete gravação automaticamente após falha.

## Endpoints adicionados

| Método | Endpoint | Operação |
| --- | --- | --- |
| PUT | `/api/admin/usuarios/{id}/acesso` | Ativo, perfil e versão |
| GET/POST | `/api/admin/catalogo/{categorias,marcas,produtos,variantes}` | Listar/criar |
| PUT/DELETE | `/api/admin/catalogo/{tipo}/{id}` | Editar ou excluir com versão |
| GET | `/api/admin/solicitacoes` | Listar com filtro opcional |
| PUT | `/api/admin/solicitacoes/{id}/resolucao` | Atender/recusar com resposta e versão |
| GET | `/api/admin/estatisticas` | Indicadores e regiões |
| GET | `/api/admin/relatorios/regioes.csv` | Relatório agregado |
| GET/PUT | `/api/admin/configuracoes` | Consultar/alterar abertura do contato |

Listas usam página a partir de zero e tamanho de 1 a 50. Exclusões usam `?versao=...`. O OpenAPI descreve os DTOs. IDs e campos descritivos retornados pelo catálogo não permitem escolher proprietário nem alterar moderação.

## Verificação

```powershell
Set-Location backend
.\mvnw.cmd -B verify
Set-Location ../frontend
npm.cmd run format:check
npm.cmd test -- --watch=false
npm.cmd run build
```

As escritas de testes usam PostgreSQL descartável. Não realize testes de exclusão/moderação nos dados de demonstração que deseja preservar.
