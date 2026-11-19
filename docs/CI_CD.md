# CI/CD ? GitHub Actions e GHCR

O projeto usa **CI autom?tico, publica??o autom?tica de imagens ap?s valida??o da main e deploy manual**, conforme escolha de Paulo. Workflows preparados no c?digo; esta entrega n?o fez push, publica??o no GHCR nem conex?o a servidor.

## Fluxo

1. Push ou pull request inicia **CI e imagens** (`validacao.yml`). Backend executa Maven verify com PostgreSQL descart?vel; frontend executa npm ci, formata??o, testes e build.
2. Cada job constr?i sua imagem Docker com labels de reposit?rio e SHA, salva a imagem como artefato e mant?m o artefato por um dia. As imagens aprovadas s?o reutilizadas na publica??o, sem reconstru??o em outro job.
3. Somente push/execu??o manual da **main**, ap?s ambos os jobs aprovados, publica no GHCR. Pull requests e outras branches n?o publicam. O job de publica??o tem `packages: write`; os demais ficam com `contents: read`.
4. As imagens recebem o SHA completo do commit. N?o h? tag `latest` nem deploy autom?tico:

```text
ghcr.io/paulovargas/compara_preco-api:<SHA-de-40-caracteres>
ghcr.io/paulovargas/compara_preco-web:<SHA-de-40-caracteres>
```

5. **Deploy manual** (`deploy.yml`) s? executa na main, recebe um SHA publicado e utiliza o Environment `producao`. Transfere os arquivos Compose, baixa as duas imagens da mesma vers?o, confere a label de revis?o, faz backup do banco, sobe servi?os, reinicia o frontend e verifica frontend/API pelo proxy. S? ent?o registra `current-release` no servidor.

O GHCR permite substituir tags; proteja as permiss?es de escrita do pacote. O SHA identifica a revis?o do c?digo e as labels s?o conferidas no deploy. Rerodar um CI pode atualizar a imagem daquela revis?o. Os digests s?o registrados na sa?da do push.

## Habilitar no GitHub

Ap?s revisar o commit local, envie a main ao remoto j? configurado. Nenhum token pessoal de publica??o precisa ser colocado no c?digo: o workflow usa `GITHUB_TOKEN`, conforme a [documenta??o do GitHub](https://docs.github.com/en/actions/tutorials/publish-packages/publish-docker-images).

Em **Settings ? Actions**, permita as actions usadas e confirme que a organiza??o/reposit?rio permite `packages: write`. O job solicita essa permiss?o explicitamente. No primeiro push, os pacotes podem ser privados. Confira visibilidade e v?nculo dos pacotes ao reposit?rio; n?o torne p?blicos sem decidir isso explicitamente.

Configure prote??o/ruleset para a main: pull request, revis?o e checks `backend` e `frontend` obrigat?rios. Restrinja quem pode enviar diretamente ? main. As actions est?o fixadas por SHA; atualiza??es devem ser revisadas.

O resumo do job `publicar` informa as refer?ncias das imagens. Use o SHA completo de uma execu??o aprovada; nunca informe uma branch ou `latest` no deploy.

## Preparar o servidor ? pendente de destino

Requisitos: Linux com Bash, Docker Engine, **Docker Compose ? 2.24**, curl, flock e SSH. O overlay usa `!reset` para remover build; o servidor s? recebe Compose e imagens, sem compilar c?digo.

1. Crie um usu?rio de deploy com diret?rio exclusivo, por exemplo `/opt/compara-preco`, e acesso ao Docker. Acesso ao socket Docker equivale a poder administrativo: use conta/chave restritas a esta opera??o.
2. Configure `.env.producao` nesse diret?rio, com senha de banco pr?pria, porta e bootstrap do primeiro admin conforme [implanta??o](IMPLANTACAO.md). Restrinja o arquivo ao usu?rio. Segredos de banco/bootstrap ficam no host e n?o s?o copiados pelo workflow. Desabilite bootstrap ap?s criar o admin.
3. Se os pacotes forem privados, autentique o Docker desse usu?rio no GHCR com credencial de leitura de pacotes. N?o utilize token de publica??o no servidor. Verifique as permiss?es de `~/.docker/config.json`.
4. Configure dom?nio/proxy HTTPS apontando para o frontend em loopback, firewall e backups externos. N?o exponha banco/API.
5. Obtenha a host key SSH por um canal confi?vel e confira seu fingerprint. O workflow usa `StrictHostKeyChecking=yes`; n?o coleta e confia automaticamente em `ssh-keyscan` durante o deploy.

O projeto Compose ? sempre `compara-preco-producao`; trocar o diret?rio do release n?o cria outro volume. Todos os servi?os continuam com **restart=no** e precisam ser iniciados manualmente ap?s reiniciar Docker/servidor.

## Environment producao

Crie em **Settings ? Environments ? producao**. Restrinja deployments ? main e configure revisores quando dispon?vel no plano do GitHub. O acionamento manual j? ? obrigat?rio; prote??o do Environment fornece uma revis?o adicional quando configurada.

| Tipo | Nome | Valor |
| --- | --- | --- |
| Variable | DEPLOY_HOST | DNS ou IPv4 do servidor |
| Variable | DEPLOY_USER | Usu?rio SSH de deploy |
| Variable | DEPLOY_PORT | Porta SSH, padr?o 22 |
| Variable | DEPLOY_PATH | Diret?rio absoluto, sem espa?os; exemplo `/opt/compara-preco` |
| Secret | DEPLOY_SSH_KEY | Chave privada do usu?rio de deploy |
| Secret | DEPLOY_KNOWN_HOSTS | Entrada known_hosts verificada; para porta diferente de 22, formato `[host]:porta` |

N?o coloque esses valores no YAML nem em commits. A valida??o recusa par?metros inv?lidos/ausentes antes da conex?o. Chaves tempor?rias do runner s?o removidas ao final, mesmo em falha.

## Executar deploy

Actions ? **Deploy manual** ? Run workflow ? branch main ? `versao` com SHA completo publicado. A conta do usu?rio de deploy deve j? conseguir `docker pull` das duas imagens.

O deploy:

- usa concorr?ncia exclusiva no GitHub e `flock` no servidor;
- valida composi??o, baixa imagens e confere a revis?o antes de atualizar aplica??es;
- inicia/confere PostgreSQL, cria backup custom por data/SHA e interrompe se falhar;
- aplica migra??es pela API e aguarda healthchecks;
- reinicia web para renovar o endere?o interno da API e consulta `/healthz` e `/api/auth/csrf`;
- s? grava `current-release` ap?s sa?de confirmada.

Em falha, revise logs/estado: **n?o h? rollback autom?tico de banco**. O backup ? local ao servidor; reten??o, c?pia externa e monitoramento continuam necess?rios. Para voltar vers?o, execute deploy manual do SHA anterior somente se compat?vel com o schema; caso contr?rio restaure backup em banco novo conforme guia operacional. N?o altere o hist?rico Flyway.

## Execu??o manual no host

O workflow coloca `compose.producao.yml` e `compose.registry.yml` em `releases/<SHA>`. Exemplo para inspe??o ou subida manual ap?s reiniciar Docker:

```bash
cd /opt/compara-preco
export IMAGE_PREFIX=ghcr.io/paulovargas/compara_preco
export APP_VERSION=SHA_COMPLETO
# Use o mesmo projeto e ambos os arquivos; nunca execute down -v.
docker compose -p compara-preco-producao --env-file .env.producao \
  -f "releases/$APP_VERSION/compose.producao.yml" \
  -f "releases/$APP_VERSION/compose.registry.yml" up -d --no-build --wait
```

## Valida??o desta entrega

Workflows verificados com actionlint (incluindo ShellCheck dos blocos), script remoto com `bash -n`/ShellCheck, overlay resolvido pelo Compose e conferido sem `build` e com restart=no. Simula??o `--dry-run` sobre homologa??o n?o altera containers. Aplica??o e Dockerfiles n?o foram modificados; os ?ltimos testes de aplica??o passaram com 138 cen?rios backend e 94 frontend.

A execu??o efetiva no GitHub e o deploy SSH ser?o verificados ap?s envio do commit e configura??o do Environment/servidor. Falta de servidor n?o impede preparar o pipeline; impede afirmar que um deploy remoto j? funcionou.
