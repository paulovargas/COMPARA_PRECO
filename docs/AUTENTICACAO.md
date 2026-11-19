# Autenticação e permissões

A API usa Spring Security com sessão JDBC no PostgreSQL. O navegador recebe apenas o cookie opaco `SESSION`; a identidade fica no servidor. O cadastro público cria um comprador. A aplicação ainda não tem formulários de conta na interface; eles serão implementados na etapa 7.

## Endpoints

| Método | Caminho | Acesso e resultado |
| --- | --- | --- |
| GET | `/api/auth/csrf` | Público; retorna `cabecalho` e `token` da sessão |
| POST | `/api/auth/cadastro` | Público com CSRF; cria comprador, retorna 201; não inicia sessão autenticada |
| POST | `/api/auth/login` | Público com CSRF; email/senha JSON; retorna usuário e inicia sessão |
| GET | `/api/auth/eu` | Autenticado; retorna o usuário da sessão |
| POST | `/api/auth/logout` | CSRF obrigatório; encerra sessão e retorna 204 |
| GET | `/api/admin/usuarios?pagina=0&tamanho=20` | ADMIN; consulta paginada, tamanho máximo 50 |
| PATCH | `/api/admin/usuarios/{id}/situacao` | ADMIN com CSRF; corpo `{"ativo":false}` ou `{"ativo":true}` |

Cadastro recebe `nome`, `email` e `senha`. Campos adicionais não atribuem perfil; `perfil=ADMIN` não permite criar um administrador. Login recebe `email` e `senha`. Respostas de usuário contêm `id`, `nome`, `email`, `perfil`, `ativo`, `criadoEm` e `versao`.

Os perfis são COMPRADOR, FORNECEDOR e ADMIN. Rotas `/api/admin/**` são exclusivas de ADMIN e `/api/fornecedor/**` de FORNECEDOR. O cadastro inicial da empresa usa POST `/api/fornecedores`, permitido para comprador e fornecedor. O sucesso altera o perfil e exige novo login. Veja [fornecedores e ofertas](FORNECEDORES_E_OFERTAS.md). Catálogo público e comparação terão suas permissões liberadas junto aos endpoints correspondentes. As demais rotas são negadas por padrão. Saúde e documentação OpenAPI são públicas; `/actuator/info` exige ADMIN.

## CSRF, sessão e navegador

1. Faça GET em `/api/auth/csrf` e mantenha o cookie recebido.
2. Envie o token no cabeçalho retornado, atualmente `X-CSRF-TOKEN`, em POST e PATCH, inclusive cadastro e login.
3. Depois do login, faça outro GET em `/api/auth/csrf` antes de novas alterações. O identificador da sessão e o token são rotacionados.
4. Logout é POST com o token atual. O cookie anterior deixa de autenticar. Para entrar de novo, obtenha outro token.

O token de uma sessão não funciona em outra. Requisições sem CSRF válido retornam 403. A cookie de sessão é HttpOnly e SameSite=Lax. O tempo de inatividade é 30 minutos; as sessões ficam persistidas em `compara_preco.spring_session` e seus atributos na tabela associada. A sessão contém identidade e perfil, sem senha nem hash.

Em desenvolvimento, use o proxy `/api/**` do Angular. Em produção, sirva frontend e API na mesma origem, com HTTPS e `SESSION_COOKIE_SECURE=true`. A API não habilita CORS irrestrito nem armazena credenciais em localStorage.

## Testar no Swagger

Abra `/swagger-ui.html`. Execute GET `/api/auth/csrf`, copie somente o valor `token` e informe em **Authorize → csrf**. O cookie é mantido pelo navegador. Execute cadastro ou login com JSON. Após login, busque outro token e atualize Authorize antes de executar PATCH ou logout. Operações GET públicas não exigem token.

O endpoint de logout é tratado pelo filtro do Spring Security e está incluído explicitamente no contrato OpenAPI.

## Exemplo no PowerShell

Com a API iniciada, crie uma sessão HTTP e obtenha o token:

```powershell
$sessao = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$csrf = Invoke-RestMethod http://localhost:8080/api/auth/csrf -WebSession $sessao
$cabecalhos = @{}
$cabecalhos[$csrf.cabecalho] = $csrf.token
$senhaProtegida = Read-Host 'Senha (12 a 64 caracteres, até 72 bytes UTF-8)' -AsSecureString
$dados = @{
  nome = 'Pessoa de Teste'
  email = 'pessoa@exemplo.com'
  senha = [System.Net.NetworkCredential]::new('', $senhaProtegida).Password
}
$corpo = [System.Text.Encoding]::UTF8.GetBytes(($dados | ConvertTo-Json))
Invoke-RestMethod http://localhost:8080/api/auth/cadastro -Method Post -WebSession $sessao -Headers $cabecalhos -ContentType 'application/json; charset=utf-8' -Body $corpo
```

Faça login e renove o token:

```powershell
$login = @{ email = $dados.email; senha = $dados.senha } | ConvertTo-Json
Invoke-RestMethod http://localhost:8080/api/auth/login -Method Post -WebSession $sessao -Headers $cabecalhos -ContentType 'application/json; charset=utf-8' -Body ([System.Text.Encoding]::UTF8.GetBytes($login))
$csrf = Invoke-RestMethod http://localhost:8080/api/auth/csrf -WebSession $sessao
$cabecalhos[$csrf.cabecalho] = $csrf.token
Invoke-RestMethod http://localhost:8080/api/auth/eu -WebSession $sessao
Invoke-RestMethod http://localhost:8080/api/auth/logout -Method Post -WebSession $sessao -Headers $cabecalhos
Remove-Variable dados, login, corpo, senhaProtegida
```

Use dados locais de teste. Não coloque senhas em arquivos versionados ou no histórico de comandos.

## Primeiro administrador

Não há conta ou senha administrativa padrão. Com o banco local configurado e no mesmo terminal usado para iniciar o backend:

```powershell
$env:APP_ADMIN_HABILITADO = 'true'
$env:APP_ADMIN_NOME = 'Administrador'
$env:APP_ADMIN_EMAIL = 'admin@exemplo.com'
$senhaAdmin = Read-Host 'Senha do administrador' -AsSecureString
$env:APP_ADMIN_SENHA = [System.Net.NetworkCredential]::new('', $senhaAdmin).Password
Set-Location backend
.\mvnw.cmd spring-boot:run
```

A inicialização cria um ADMIN somente se ainda não existir nenhum. Não altera nem promove uma conta existente com o mesmo email; um conflito interrompe a inicialização. Depois da primeira criação, encerre a API, remova essas variáveis e inicie normalmente:

```powershell
Remove-Item Env:APP_ADMIN_HABILITADO, Env:APP_ADMIN_NOME, Env:APP_ADMIN_EMAIL, Env:APP_ADMIN_SENHA
Remove-Variable senhaAdmin
.\mvnw.cmd spring-boot:run
```

O bootstrap é para a configuração inicial de uma instância. Não é um mecanismo de recuperação de conta ou de gestão de permissões. Não cria contas de demonstração.

## Bloqueio e limites

Bloquear uma conta remove suas sessões persistidas. Cada requisição autenticada também confere se o usuário ainda está ativo e mantém o mesmo perfil e email. Mudanças invalidam a sessão antiga. Desbloquear permite novo login, sem reativar cookies antigos. Um administrador não pode bloquear a própria conta nem remover o próprio perfil administrativo. A alteração versionada de acesso está descrita no [painel administrativo](ADMINISTRACAO.md).

Os contadores de tentativas são persistidos e atualizados atomicamente. Contam tentativas válidas estruturalmente, inclusive logins bem-sucedidos:

| Limite padrão | Janela | Propriedade |
| --- | --- | --- |
| 30 logins por IP | 10 minutos | `app.seguranca.limite-login-ip` |
| 10 logins por email | 10 minutos | `app.seguranca.limite-login-email` |
| 10 cadastros por IP | 1 hora | `app.seguranca.limite-cadastro-ip` |

Ao exceder, retorna 429 com `Retry-After` em segundos. Os contadores usam chaves SHA-256 e são limpos periodicamente após um dia. O IP vem da conexão HTTP; cabeçalhos de encaminhamento não são confiados automaticamente. A publicação atrás de proxy deve configurar explicitamente os proxies confiáveis para obter o IP de origem correto.

## Erros e validação

Senhas têm de 12 a 64 caracteres, com limite adicional de 72 bytes UTF-8 para BCrypt, fator 12. Espaços internos da senha são preservados. Email é normalizado para minúsculas e sem espaços nas extremidades e tem unicidade no banco. DTOs de credenciais ocultam dados em `toString`; respostas e a serialização da entidade não expõem o hash.

Erros seguem o contrato `instante`, `status`, `codigo`, `mensagem` e `campos`. Não incluem valores rejeitados, senhas, stack traces ou mensagens de banco. Credenciais incorretas, conta inexistente e bloqueada recebem a mesma resposta de login.

- 400: formato, paginação ou campos inválidos.
- 401: credenciais inválidas ou sessão ausente/expirada/revogada.
- 403: perfil sem permissão ou CSRF inválido.
- 404: usuário solicitado não encontrado por administrador autorizado.
- 409: email em uso, autobloqueio ou conflito de atualização.
- 429: limite de tentativas.

## Validação executável

`mvnw.cmd -B verify` usa PostgreSQL descartável via Testcontainers. A suíte HTTP mantém cookies reais e cobre cadastro, hash, não elevação de perfil, CSRF entre sessões, rotação, logout e replay de cookie, expiração, perfis, bloqueio e mudança de perfil. Também testa concorrência dos limites, renovação da janela e bootstrap administrativo idempotente.

Os formulários de conta serão implementados na etapa 7 do planejamento.

Referências de manutenção: [sessões JDBC do Spring Boot](https://docs.spring.io/spring-boot/reference/web/spring-session.html), [persistência de autenticação](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html), [CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html) e [armazenamento de senhas](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html).

## Interface de conta

Cadastro, login, logout e restauração de sessão estão conectados à interface Angular. O interceptor consulta CSRF antes de cada mutação e não repete alterações automaticamente. Expiração limpa a identidade e retira a lista privada da tela. Consulte [CONTA_E_CONTATO.md](CONTA_E_CONTATO.md).
