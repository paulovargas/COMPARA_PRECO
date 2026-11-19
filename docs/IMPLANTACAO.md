# Execução preparada para produção

A distribuição foi preparada para validação local. A publicação externa depende de servidor, domínio, DNS, certificado TLS e acessos ainda não definidos. Este guia não representa uma publicação realizada.

## Arquitetura

`compose.producao.yml` é independente do Compose de desenvolvimento. O projeto padrão é `compara-preco-producao` e tem volume próprio. PostgreSQL e API não publicam portas; Nginx publica somente `127.0.0.1:8088`. Um proxy TLS no mesmo servidor deve encaminhar o domínio HTTPS para essa porta. A API usa cookies Secure, HttpOnly e SameSite=Lax. API e frontend usam a mesma origem, sem CORS adicional.

Todos os serviços têm **restart=no**, inclusive produção, conforme preferência de inicialização manual. Após reiniciar Docker/servidor, inicie explicitamente o projeto. API e Nginx executam sem root, com sistema de arquivos somente leitura e áreas temporárias limitadas. Logs têm rotação. O banco conserva o volume persistente.

O perfil `producao` aplica migrações estruturais, desabilita Swagger/OpenAPI e exige cookie Secure. A distribuição não habilita demo nem cria ofertas/contas de exemplo. O primeiro administrador é opcional e criado pelo bootstrap por ambiente.

## 1. Validar e produzir artefatos

Selecione JDK 25 e use os comandos do README. Na raiz:

```powershell
Set-Location backend
.\mvnw.cmd -B verify
Set-Location ../frontend
npm.cmd ci
npm.cmd run format:check
npm.cmd test -- --watch=false
npm.cmd run build
Set-Location ..
```

Os Dockerfiles copiam somente o JAR empacotado e a distribuição Angular. Execute build/testes antes de construir as imagens. Contextos `.dockerignore` excluem segredos, dependências e fontes desnecessárias. Imagens base: [Eclipse Temurin](https://hub.docker.com/_/eclipse-temurin/tags) e [Nginx](https://hub.docker.com/_/nginx), com versões fixas nos Dockerfiles. Reavaliar atualizações antes de publicar em uma data posterior.

## 2. Configurar ambiente separado

```powershell
Copy-Item .env.producao.example .env.producao
```

Edite `.env.producao`, ignorado pelo Git:

- `PROD_DB_PASSWORD`: senha exclusiva e forte; não reutilizar desenvolvimento.
- `PROD_DB_NAME`: padrão `compara_preco`. O usuário do banco é `compara_preco`.
- `PROD_WEB_PORT`: padrão 8088, sempre em loopback.
- `APP_VERSION`: etiqueta local/versionada das imagens, por exemplo um hash de commit.
- No primeiro início, `PROD_ADMIN_HABILITADO=true`, nome/email e senha exclusiva nas variáveis `PROD_ADMIN_*`. Depois de criar a conta, desabilite bootstrap e remova a senha do arquivo, recriando a API e reiniciando o frontend em seguida. Nunca use as contas demo em produção.

Restrinja acesso ao arquivo de ambiente e aos backups. No Linux, use `chmod 600 .env.producao`. Não envie o arquivo para Git nem publique saída de `docker compose config` que inclua segredos; valide com `config --quiet`.

## 3. Construir e iniciar manualmente

```powershell
docker compose --env-file .env.producao -f compose.producao.yml config --quiet
docker compose --env-file .env.producao -f compose.producao.yml build
docker compose --env-file .env.producao -f compose.producao.yml up -d --wait
docker compose --env-file .env.producao -f compose.producao.yml ps
```

Se `docker compose` não estiver disponível no Windows, substitua por:

```powershell
& 'C:\Program Files\Docker\Docker\resources\bin\docker-compose.exe' --env-file .env.producao -f compose.producao.yml up -d --wait
```

Inspeção de logs: `docker compose --env-file .env.producao -f compose.producao.yml logs --tail=100 api`. Acesso local: <http://localhost:8088>. Para validar cookies Secure em HTTP local, use navegador com suporte à exceção de localhost, como Edge/Chromium; não desabilite Secure para publicar. Em acesso remoto use HTTPS.

O Nginx encaminha `/api/` para a API, permite recarregar rotas Angular e limita o corpo a 3 MB; a API mantém limite de arquivo de 2 MB. `/healthz` verifica o frontend. Actuator/Swagger/OpenAPI não são publicados pelo frontend. Healthcheck interno da API consulta `/actuator/health`.

Parar sem remover dados:

```powershell
docker compose --env-file .env.producao -f compose.producao.yml stop
```

Não utilize `down -v`: remove permanentemente o volume. Não reutilize o nome do projeto de desenvolvimento nem seu volume.

## 4. Domínio e HTTPS — pendente

Antes de exposição externa, configure DNS e proxy TLS no host para o domínio escolhido. Encaminhe toda a origem para `http://127.0.0.1:8088`, preserve Host, redirecione HTTP para HTTPS e permita upload de até 3 MB. A API não confia em headers encaminhados para decisões de autenticação; cookies Secure são explícitos. O limite por IP pode tratar acessos pelo proxy como origem compartilhada; o limite por email continua individual. Revisar IP confiável/rate limiting no proxy conforme a topologia antes da abertura pública.

Verifique validade/renovação do certificado, redirecionamento HTTP, cookie Secure/HttpOnly/SameSite, CSRF, rotas privadas, limites e contato. Exponha somente o proxy; mantenha PostgreSQL/API inacessíveis externamente. Não houve criação de DNS, contratação de servidor, envio de imagem a registry ou publicação remota.

## 5. Backup e restauração

Os scripts PowerShell funcionam com Docker CLI e o projeto Compose identificado por labels. O backup usa formato custom do PostgreSQL e `docker cp`, evitando corrupção de binário por redirecionamento do PowerShell antigo. Um destino existente é recusado.

```powershell
.\scripts\backup.ps1 -Destino .tools/backups/antes-atualizacao.dump
.\scripts\restaurar.ps1 -Arquivo .tools/backups/antes-atualizacao.dump -NovoBanco compara_restaurado
```

Para outra instância, use `-Projeto` em ambos; backup aceita `-Banco`. Restauração cria banco **novo** e recusa se ele já existir; nunca apaga o banco atual. Usa transação única e interrupção em erro. Após restaurar, confira schema, migrações, contagens e fluxos antes de mudar a API. Para recuperação, pare a API, ajuste `PROD_DB_NAME` para o banco restaurado e inicie novamente. Sessões e contas fazem parte do backup; proteja e armazene cópias fora do servidor, com retenção e teste periódico.

A criação do volume não faz backup automaticamente. Agendamento, retenção, cópia externa e monitoramento precisam ser definidos para o servidor escolhido.

## 6. Atualização e reversão

1. Identifique a versão atual de imagens/código e retenha as imagens anteriores.
2. Execute validação e faça backup verificado antes de migrar.
3. Gere os novos artefatos, altere `APP_VERSION` e construa/inicie. Flyway aplica as migrações na inicialização da API.
4. Confira saúde e fluxos antes de liberar tráfego.
5. Para reverter código, use imagens anteriores somente se compatíveis com o schema. Para migração incompatível, restaure o backup em banco novo e direcione a versão anterior para ele. Não altere o histórico Flyway nem aplique scripts de desfazer improvisados.

## Limites da entrega local

A implantação foi preparada para host único. Não inclui operação gerenciada, alta disponibilidade, armazenamento próprio de imagens, observabilidade externa, agendamento de backups ou autenticação de dois fatores. Produtos usam URLs de imagem. O relatório é uma fotografia por UF; não há checkout, pagamentos, vendas ou coleta externa. Escopo de publicação e operação deve ser finalizado com o destino escolhido.

## Roteiro integrado reproduz?vel

`scripts/validar-homologacao.mjs` usa Node 22 e APIs HTTP reais. Cria contas, empresa, cat?logo, oferta, favoritos, importa??o e solicita??o; testa aprova??o, modera??o, bloqueio e reaprova??o. N?o remove os registros criados e n?o envia mensagens. Execute somente em inst?ncia isolada de homologa??o, nunca no banco que deseja preservar:

```powershell
$env:SMOKE_BASE_URL = 'http://localhost:8088'
$env:SMOKE_ADMIN_EMAIL = Read-Host 'Email do admin da homologa??o'
$senhaHomologacao = Read-Host 'Senha do admin da homologa??o' -AsSecureString
$env:SMOKE_ADMIN_SENHA = [Net.NetworkCredential]::new('', $senhaHomologacao).Password
$env:SMOKE_PERMITIR_GRAVACAO = 'SIM'
node scripts/validar-homologacao.mjs
Remove-Item Env:SMOKE_ADMIN_SENHA
Remove-Item Env:SMOKE_PERMITIR_GRAVACAO
```

A confirma??o `SIM` e credenciais s?o obrigat?rias. O roteiro n?o registra senhas, cookies ou CSRF no resultado. O teste de cookies no navegador e HTTPS externo ? separado da valida??o HTTP.
