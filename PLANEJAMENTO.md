# Planejamento — Compara Preço

## Objetivo e situação atual

Conectar compradores a fornecedores por meio de busca, comparação de ofertas equivalentes e contato pelo WhatsApp, conforme `descricao.md`.

O projeto partiu apenas da descrição. O Git foi inicializado e o planejamento foi registrado no commit `00b4a30`; a estrutura técnica e o domínio persistente do catálogo estão implementados e validados. Este documento orienta as entregas e deve ser atualizado a cada etapa.

## Direção técnica proposta

- Backend: Java com Spring Boot, API REST, validação de DTOs e responsabilidades separadas entre controllers, services e repositories.
- Frontend: Angular com TypeScript, componentes standalone e organização por funcionalidades.
- Persistência: PostgreSQL, migrações versionadas e dados de demonstração separados dos dados de produção.
- Execução local: Docker Compose para banco e, posteriormente, aplicação; configuração por ambiente e exemplos sem credenciais reais.
- Documentação: README em português e OpenAPI para a API.

Esta proposta substitui a sugestão preliminar de Python/SQLite e aproxima o projeto das convenções de Paulo para aplicações full stack. Versões fixadas na preparação: Java 25, Spring Boot 4.1.2, Maven Wrapper 3.10.1, Angular/CLI 21.2.26, PostgreSQL 17.7 e Springdoc 3.2.0. O lockfile do frontend registra as dependências resolvidas.

Ambiente observado: Java 11, Maven 3.8.8, Node 22.15.1, Python 3.13.15 e cliente Docker instalado. Foi encontrado e utilizado o JDK 25 já instalado; Docker Desktop e Compose foram validados com PostgreSQL saudável. No PowerShell, usar `npm.cmd` para evitar o bloqueio de `npm.ps1`, sem alterar a política do sistema.

## Escopo funcional

1. Comprador: cadastro, login, busca, filtros, comparação, favoritos de produtos e fornecedores, contato via WhatsApp.
2. Fornecedor: cadastro da empresa, aprovação, perfil, catálogo, ofertas, preços, estoque e importação CSV/XLSX.
3. Administrador: usuários, fornecedores, aprovação e bloqueio, categorias, marcas, produtos, moderação, permissões, estatísticas, relatórios e configurações gerais.
4. Entrega: interface responsiva, API, banco, testes, documentação e preparação para publicação.

Checkout, pagamentos, pedidos e coleta automática em sites externos não fazem parte do escopo inicial. A integração com WhatsApp será um link de contato com mensagem preenchida.

## Regras centrais e modelo inicial

- Produto identifica o modelo de catálogo e se relaciona com marca e categoria.
- Variante identifica as características comparáveis: modelo, armazenamento quando aplicável, cor, condição e demais atributos definidos para a categoria. Características diferentes não devem ser agrupadas automaticamente como equivalentes.
- Oferta pertence a uma variante e a um fornecedor e contém preço decimal positivo, estoque, disponibilidade, situação de moderação e data de atualização.
- A comparação considera apenas ofertas aprovadas e disponíveis de fornecedores aprovados e ativos. Ofertas são ordenadas por preço; empates recebem o mesmo destaque de menor preço.
- Preço inicial e quantidade de ofertas usam o mesmo conjunto de ofertas elegíveis da comparação.
- Um fornecedor só pode consultar e alterar seus próprios dados privados e ofertas. A API deve aplicar a autorização independentemente da interface.
- Usuários bloqueados não podem manter acesso autenticado. O bloqueio de fornecedor retira suas ofertas dos resultados públicos.
- Importações devem usar identificadores explícitos de variantes. Linhas ambíguas exigem correção; não haverá agrupamento baseado apenas na semelhança de títulos.
- Importações terão validação, prévia de erros por linha e confirmação antes de persistir. O comportamento para duplicidades e reimportação será documentado e testado.
- Entidades iniciais: usuário, perfil/permissão, fornecedor, categoria, marca, produto, variante, oferta, produto favorito, fornecedor seguido e importação.

## Etapas e commits previstos

Cada etapa termina com validação e atualização deste plano. Os commits devem conter mudanças coerentes e revisáveis; os nomes abaixo podem ser divididos quando a entrega justificar.

| Etapa | Entrega e critério de aceite | Commit previsto | Estado |
| --- | --- | --- | --- |
| 0 | Planejamento registrado, escopo e decisões iniciais explícitos | `docs: registra planejamento do projeto` | Concluída (`00b4a30`) |
| 1 | Git inicializado, stack verificada, estrutura backend/frontend, configurações de ambiente, banco local e README inicial; aplicações compilam e banco inicia | `chore: prepara estrutura e ambiente de desenvolvimento` | Concluída |
| 2 | Migrações, catálogo e variantes com restrições de integridade; dados demonstrativos reproduzíveis | `feat: modela catalogo e variantes de produtos` | Concluída |
| 3 | Cadastro, autenticação, encerramento de sessão, perfis e autorização; testes de acesso indevido e bloqueio | `feat: implementa autenticacao e permissoes` | Concluída |
| 4 | Cadastro e aprovação de fornecedor, perfil e CRUD de ofertas com isolamento por proprietário | `feat: implementa fornecedores e gerenciamento de ofertas` | Concluída |
| 5 | API de busca, filtros, paginação e comparação; testes de equivalência, empates e ofertas inelegíveis | `feat: implementa busca e comparacao de precos` | Concluída |
| 6 | Interface pública responsiva com categorias, busca, filtros e comparação conectadas à API; estados de carregamento, vazio e erro | `feat: cria experiencia de busca e comparacao` | Concluída |
| 7 | Autenticação na interface, favoritos, fornecedores seguidos e WhatsApp; persistência por usuário | `feat: adiciona conta favoritos e contato` | Concluída |
| 8 | Painel do fornecedor para empresa, catálogo, preços, estoque e disponibilidade | `feat: cria painel do fornecedor` | Concluída |
| 9 | CSV/XLSX com modelo de arquivo, limites, validação, prévia e confirmação; testes de arquivos inválidos e duplicidades | `feat: adiciona importacao de ofertas` | Concluída |
| 10 | Painel administrativo com cadastros, moderação, bloqueio, permissões, estatísticas, relatórios e configurações | `feat: cria painel administrativo` | Concluída |
| 11 | Fluxos integrados validados, documentação completa, configuração de produção, backup e roteiro de publicação | `docs: documenta validacao e publicacao` e correções específicas | Concluída localmente; publicação externa pendente |

Dependências: catálogo e autorização sustentam fornecedores e ofertas; ofertas sustentam comparação; o fluxo de comparação deve funcionar antes dos painéis completos. Importação depende das regras de variante e oferta já testadas.

## Estratégia de validação

- Backend: testes de regras de comparação, autorização, validação, bloqueio, favoritos e importação; testes de integração com PostgreSQL para persistência e contratos HTTP relevantes.
- Frontend: testes dos fluxos com comportamento relevante e build de produção; revisão visual em desktop e celular.
- Fluxo integrado: administrador aprova fornecedor; fornecedor cadastra oferta; comprador encontra a variante, compara, favorita e abre contato; bloqueio retira a oferta dos resultados.
- Segurança funcional: senhas com hash apropriado, sessões e cookies protegidos, proteção contra CSRF conforme autenticação escolhida, limites de login e importação, segredos por ambiente e erros sem detalhes internos.
- Comandos-alvo, a confirmar após criação dos projetos: `mvnw.cmd verify` no backend; `npm.cmd ci`, comando de testes definido no `package.json` e `npm.cmd run build` no frontend; `docker compose config` e execução integrada local.
- Registrar os comandos realmente executados e seus resultados. Não considerar testes planejados como executados.

## Organização de Git e evolução

- Criar `.gitignore` antes do primeiro commit; excluir segredos, dependências, builds e dados locais.
- Preservar `descricao.md` como referência original.
- Inspecionar o diff antes de cada commit e registrar apenas os arquivos da entrega correspondente.
- Atualizar etapas, decisões, verificações e próximos passos neste documento.
- Push e publicação dependem de destino configurado e autorização correspondente; não fazem parte de um commit local.

## Decisões e dependências em aberto

- Stack verificada: usar o JDK 25 já instalado, selecionado por terminal; Angular 21 é compatível com Node 22.15.1. Não alterar as ferramentas globais da máquina.
- Detalhar atributos de categorias além de smartphones ao implementar variantes.
- Definir regras de prazo de atualização das ofertas e tratamento de ofertas antigas antes da liberação em produção.
- Definir métricas, formatos de relatórios e configurações administrativas durante o detalhamento do painel.
- Para publicação efetiva: definir servidor, domínio, acesso, TLS, armazenamento de imagens, estratégia de backup e credenciais por ambiente. Preparar os arquivos localmente enquanto esses dados não estiverem disponíveis.

## Próxima entrega

Preparação local concluída. Próximo passo: publicação externa após definição de servidor, domínio, TLS e acessos. Paulo confirmou nesta etapa que ainda não tem destino definido e solicitou preparar localmente; nenhuma publicação externa foi autorizada ou realizada.

## Registro da etapa 1 — 17/11/2026

- Git inicializado em `main`; primeiro commit contém o planejamento e as convenções de arquivos.
- Backend gerado pelo Spring Initializr oficial; frontend gerado pelo Angular CLI.
- PostgreSQL iniciado com Docker Compose, dados persistentes e verificação de saúde. A porta 5432 foi recusada pelo Windows; usamos `127.0.0.1:15432`.
- Primeira migração cria o schema da aplicação; tabelas de catálogo ficam para a etapa 2.
- `.env` local criado com senha aleatória e ignorado pelo Git. Backend lê esse arquivo ou variáveis externas.
- Página de apresentação informa que a plataforma está em preparação; ainda não há busca ou ofertas simuladas.
- README e pipeline preparados. O pipeline remoto ainda não foi executado.
- Backend: `mvnw.cmd -B verify` passou após os ajustes finais, com 2 testes, nenhuma falha e nenhum teste ignorado. A API também respondeu em execução normal: saúde `UP` e OpenAPI 3.1.0 com título da aplicação.
- Instalação inicial do frontend encontrou erro interno `edgesOut` no npm 11.5.1. O lockfile foi gerado com npm 11.21.2 executado localmente. Depois, `npm.cmd ci --no-fund --no-audit` passou também com o npm instalado, confirmando reprodução pelo lockfile.
- Frontend: `npm.cmd run format:check` e `npm.cmd run build` passaram. Bundle inicial de aproximadamente 205 kB, sem alertas de orçamento. Ainda não há testes de comportamento da interface; eles acompanharão as funcionalidades.
- Compose: configuração válida e PostgreSQL saudável. Nenhuma alteração em Java ou npm globais foi necessária.
- Arquivos revisados com `git diff --cached --check`; credenciais locais e artefatos de build ficam fora do versionamento.

## Ajuste de execução local — 17/11/2026

- Por solicitação de Paulo, os containers do projeto devem iniciar somente por ação manual, sem subir automaticamente ao abrir o Docker Desktop.
- Compose configurado com `restart: "no"`; política aplicada também ao PostgreSQL existente via `docker update --restart=no`. A alteração preserva a execução atual e os dados.
- Para futuros serviços adicionados ao Compose, manter a política de inicialização manual.
- Validação: configuração Compose e inspeção da política do container existente.

## Execução da etapa 2 — 17/11/2026

- Catálogo organizado por funcionalidade, com entidades e repositories de categoria, marca, produto e variante.
- Produto único por marca e modelo normalizado; a variante distingue armazenamento opcional, cor opcional, condição e atributos adicionais normalizados em JSONB. Campos ausentes participam da unicidade, evitando duplicidades também com valores nulos.
- Atributos adicionais são pares textuais explícitos e limitados em tamanho. Campos centrais não podem ser repetidos no mapa. A definição de atributos obrigatórios por categoria será detalhada antes dos cadastros administrativos.
- Testes com Testcontainers criam um PostgreSQL temporário, sem modificar o banco local da aplicação. Esses containers são iniciados pelo comando de teste e removidos após sua execução, sem reinício automático.
- Dados demonstrativos em localização Flyway separada, habilitada somente pelo perfil `demo`.
- Aceite: migração validada pelo Hibernate, persistência dos relacionamentos e JSONB, rejeição de duplicatas normalizadas e dados inválidos, distinção entre variantes e carga demo idempotente.

## Validação da etapa 2 — 17/11/2026

- `mvnw.cmd -B verify`: BUILD SUCCESS, 36 testes, nenhuma falha, erro ou teste ignorado.
- Domínio: normalização de espaços e maiúsculas, armazenamento válido, chaves de atributos, limites de tamanho, imutabilidade dos atributos e URL de imagem.
- Persistência: relacionamentos, JSONB, duplicatas de categorias/marcas/modelos/variantes, nulos na identidade, diferenças entre produtos e características, integridade referencial e rejeição de dados inválidos por SQL direto.
- Carga demo: 2 categorias, 2 marcas, 3 produtos e 6 variantes, reaplicada duas vezes sem duplicação dentro de teste transacional. Perfil padrão sem registros demo.
- Testcontainers inicia um PostgreSQL descartável e remove seus containers ao término. O banco local não foi usado para testes.
- Pipeline atualizado para usar o isolamento dos testes, sem serviço PostgreSQL fixo. Execução remota continua pendente de envio ao GitHub.
- Empacotamento inicialmente encontrou o JAR bloqueado pela API anterior no Windows. Instância encerrada e verificação final concluída. Execução local passa a usar `spring-boot:run`.
- README e `docs/CATALOGO.md` documentam estrutura, regras, limitações e perfil demo. A interface pública será conectada ao catálogo nas etapas de busca e comparação.
- Execução local confirmada após reinício: saúde `UP`, migrações V1 e V2 aplicadas com sucesso no PostgreSQL de desenvolvimento.

## Correção do schema do Flyway — 17/11/2026

- A inspeção local identificou histórico inicial em `public` e histórico com V1/V2 em `compara_preco`, devido à resolução implícita de schema pelo `search_path`.
- Configuração passa a fixar `spring.flyway.default-schema=compara_preco`, que já contém a V2 aplicada. Os históricos existentes são preservados, sem cópia, exclusão ou renomeação manual.
- Testes isolados passam a usar o mesmo nome de usuário do banco local e verificam V1/V2 no schema explícito, sem criar um segundo histórico em `public`.
- A alternativa de mover o histórico foi recusada pela revisão automática e não foi executada. A correção adotada dispensa essa alteração.
- Regressão validada com `mvnw.cmd -B verify`: 36 testes passaram e pacote gerado. Instalação nova mantém V1/V2 apenas no histórico de `compara_preco`; o histórico local existente contém V1/V2 com sucesso.

## Execução da etapa 3 — 17/11/2026

- Autenticação de API com Spring Security e sessões JDBC persistidas no PostgreSQL. Cookie `SESSION` HttpOnly, SameSite=Lax, expiração por inatividade de 30 minutos e flag Secure configurável para HTTPS.
- CSRF por sessão: obter token em `GET /api/auth/csrf`, enviar o cabeçalho informado nos POST/PATCH e renovar o token após login/logout. Login rotaciona o identificador da sessão e o token CSRF.
- Endpoints: cadastro público de comprador, login JSON, logout e usuário atual. Perfis COMPRADOR/FORNECEDOR/ADMIN; o cadastro público não aceita escolha de perfil.
- Administração inicial por configuração explícita de ambiente, sem senha padrão. API administrativa mínima para consultar usuários e bloquear/desbloquear contas; proibido bloquear a própria conta. Gestão completa de permissões permanece na etapa 10.
- Autorizações por rota e método. Sessões guardam apenas identidade, sem senha/hash, e conferem estado e perfil no banco a cada requisição. Bloqueio invalida sessões persistidas e impede reaproveitamento.
- Limites persistidos e atômicos: login por IP e email em janela de 10 minutos; cadastro por IP em janela de 1 hora. Chaves armazenadas como SHA-256 e limpeza periódica de registros antigos.
- Senhas de 12 a 64 caracteres, até 72 bytes UTF-8, com BCrypt fator 12 e identificador de algoritmo para evolução. DTOs de credenciais não expõem conteúdo em toString.
- Contratos tipados e erros JSON para validação, credenciais inválidas, acesso indevido, conflitos e limite de tentativas. Interface de autenticação segue prevista na etapa 7.
- Aceite: cadastro válido/duplicado/inválido, não elevação de perfil, hash e segredo não expostos, login inválido/bloqueado, rotação de sessão/CSRF, logout, expiração, persistência de sessão, acesso por perfil e bloqueio de sessão já aberta. Testes em PostgreSQL temporário.

## Validação da etapa 3 — 17/11/2026

- `mvnw.cmd -B verify`: BUILD SUCCESS, 56 testes, sem falhas, erros ou testes ignorados. São 20 cenários HTTP/serviço de autenticação e os 36 testes anteriores.
- Cadastro normalizado, email duplicado, campos inválidos, limite UTF-8 da senha, hash BCrypt e não elevação de perfil verificados. Hash oculto também na serialização direta da entidade.
- CSRF obrigatório e vinculado à sessão; login rotaciona sessão/token; cookie HttpOnly/SameSite; contexto JDBC persiste apenas identidade e credenciais nulas.
- Logout, rejeição de cookie antigo, expiração no servidor e acesso administrativo por perfil verificados com HTTP real.
- Bloqueio de conta remove sessões; leitura de estado/perfil a cada requisição revoga acesso mesmo quando o banco é alterado fora do serviço de bloqueio. Desbloqueio exige novo login.
- Limites de cadastro/login, renovação de janela e concorrência por email validados; contadores confirmados antes de responder 429.
- Bootstrap administrativo idempotente e rejeição de promoção implícita de comprador verificados. Nenhuma conta administrativa é criada por padrão.
- Documentação em `docs/AUTENTICACAO.md`, exemplos de ambiente e Swagger atualizados, incluindo cabeçalho CSRF e logout tratado por filtro. Pipeline remoto ainda não executado.
- Frontend não foi alterado nesta etapa; telas de autenticação permanecem na etapa 7.
- Execução local confirmada: API com saúde UP, V3 aplicada com sucesso, token CSRF emitido sem exibir seu valor e logout presente no OpenAPI. PostgreSQL mantém `restart=no`.

## Execução da etapa 4 — 17/11/2026

- Uma empresa por usuário. Cadastro autenticado por comprador, com razão social, nome fantasia, descrição, WhatsApp com código do país, cidade e UF. Situação inicial PENDENTE. Perfil passa a FORNECEDOR por operação do servidor e exige novo login.
- Empresa PENDENTE pode editar o perfil; BLOQUEADO tem somente consulta. Ofertas só podem ser cadastradas/alteradas/excluídas por empresa APROVADO com conta ativa.
- Fluxo administrativo: PENDENTE → APROVADO ou BLOQUEADO; APROVADO → BLOQUEADO; BLOQUEADO → PENDENTE para nova análise. Bloqueio exige motivo. Alterar razão social ou nome fantasia de empresa aprovada exige reaprovação.
- Oferta vincula fornecedor e variante do catálogo central, com unicidade dessa combinação. Preço decimal positivo, até 2 casas e 10 dígitos inteiros; estoque não negativo; disponibilidade explícita; datas de criação/atualização e versão para concorrência.
- Criação de oferta começa PENDENTE de moderação. Preço/estoque/disponibilidade podem ser atualizados sem remover aprovação ou bloqueio existentes. Fornecedor não altera empresa, variante ou moderação pelo payload.
- API administrativa mínima aprova/bloqueia ofertas; desbloqueio retorna a PENDENTE. Excluir é operação do proprietário, com versão atual. Recursos de outro fornecedor retornam 404.
- Características obrigatórias por categoria passam a ser configuráveis: armazenamento, cor e chaves de atributos. Ofertas e aprovações rejeitam variantes incompletas. Catálogo de variantes pode ser consultado pelo fornecedor; gestão do catálogo global permanece na etapa 10.
- Uma view de elegibilidade centraliza ofertas aprovadas, disponíveis, com estoque, empresa aprovada, proprietário ativo/FORNECEDOR e variante completa. A etapa 5 usará essa mesma regra para preços e contagens públicos.
- Transações e bloqueio da empresa coordenam alterações de estado e ofertas; versões impedem sobrescrita de dados desatualizados. DTOs não aceitam escolha de proprietário.
- Aceite: cadastro/duplicidade/relogin, aprovação/bloqueio/reanálise, perfil e identidade, WhatsApp/UF válidos, isolamento entre proprietários, preço/estoque/versão inválidos, variante inexistente/incompleta, moderação e elegibilidade após bloqueios. Testes em PostgreSQL temporário.

## Validação da etapa 4 — 17/11/2026

- `mvnw.cmd -B verify`: BUILD SUCCESS, 78 testes, nenhuma falha, erro ou teste ignorado; 22 cenários novos de fornecedores/ofertas e 56 cenários anteriores.
- Cadastro vinculado ao usuário autenticado, perfil definido pelo servidor, invalidação das sessões e novo login, duplicidade e validação de WhatsApp/UF verificados por HTTP real.
- Aprovação, bloqueio com motivo, reanálise, reaprovação por mudança de identidade e controle de versões validados.
- CRUD de oferta testa isolamento entre duas empresas, tentativa de injetar outro proprietário/moderação/variante, exclusão, duplicata e versão desatualizada.
- Preço e estoque inválidos, estoque/versão fracionários e motivos com caractere nulo são rejeitados. Restrições de preço e unicidade foram testadas também por SQL direto.
- Criação recusa variante inexistente ou incompleta; catálogo informa pendências de armazenamento, cor e atributos exigidos.
- Moderação é preservada ao editar preço/estoque. A view de elegibilidade foi testada após bloqueio de empresa/conta, troca de perfil, falta de estoque, indisponibilidade, moderação pendente e mudança dos requisitos da categoria; preços elegíveis são ordenados.
- Documentação em `docs/FORNECEDORES_E_OFERTAS.md`, README e regras do catálogo atualizadas. Interface do fornecedor permanece na etapa 8 e administração completa na etapa 10.
- O fluxo para o fornecedor solicitar inclusão de produto/variante ausente deve ser detalhado com o painel e a gestão do catálogo (etapas 8/10); o CRUD atual de ofertas seleciona variantes existentes do catálogo central.
- Execução local confirmada: saúde UP, V4 aplicada, view de elegibilidade consultável e endpoints de cadastro/ofertas no OpenAPI. PostgreSQL preserva `restart=no`.

## Execução da etapa 5 — 18/11/2026

- Criar GET públicos `/api/catalogo/variantes`, `/api/catalogo/variantes/{id}/ofertas`, `/api/catalogo/categorias` e `/api/catalogo/marcas`.
- Buscar por nome, marca e modelo com texto literal; filtrar categoria, marca, modelo, armazenamento, cor, condição, fornecedor, estado e cidade. Paginação limitada a 50 itens, ordenação por menor preço e UUID para desempate estável.
- Agregar preço mínimo e quantidade diretamente da view de elegibilidade, aplicando os mesmos filtros na comparação. Variantes distintas nunca compartilham preços; empate destaca todas as ofertas mais baratas, inclusive entre páginas.
- Expor somente dados públicos da empresa, preço, estoque e atualização. Contato WhatsApp permanece na etapa 7.
- Usar consultas parametrizadas e transação de leitura com snapshot consistente para contagem, dados e mínimo. Validar contratos públicos, filtros, paginação, empates e retirada imediata de ofertas inelegíveis com PostgreSQL temporário.
- Entrega prevista em um commit funcional, com documentação e `mvnw.cmd -B verify`.

## Validação da etapa 5 — 18/11/2026

- `mvnw.cmd -B verify` e validação final `mvnw.cmd -B -o verify`: BUILD SUCCESS, 93 testes, sem falhas, erros ou testes ignorados. São 15 cenários adicionais de busca/comparação e os 78 anteriores.
- Consultas públicas por nome/marca/modelo, filtros combinados e individuais, preços mínimos e totais consistentes com fornecedor/região, distinção entre variantes e tentativa de interpretar texto como SQL verificadas por HTTP.
- Empates destacados nas duas páginas, ordem estável, página além do fim, ausência de ofertas, variante inexistente e parâmetros inválidos verificados.
- Busca, comparação e opções retiram ofertas após indisponibilidade, estoque zero, moderação pendente/bloqueada, bloqueio da empresa, conta inativa/perfil alterado e requisitos de categoria incompletos.
- DTO de oferta pública não expõe email, usuário proprietário, razão social, motivo de moderação, versão ou telefone. O contato será implementado na etapa 7.
- Consultas usam agregação/paginação no PostgreSQL e carregamento conjunto das variantes da página; transação REPEATABLE_READ mantém mínimo, total e resultados no mesmo snapshot.
- API local iniciada e validada: saúde UP, busca pública responde e OpenAPI documenta os endpoints e os 12 filtros. Banco local permanece sem ofertas publicadas; dados dos testes ficam exclusivamente no PostgreSQL descartável.
- Container local iniciado manualmente para conferência e política `restart=no` confirmada. Frontend não foi alterado nesta entrega; conexão da interface é a próxima etapa.
- Contratos documentados em `docs/BUSCA_E_COMPARACAO.md`, README atualizado e diff verificado. Nenhum push ou publicação realizado.

## Execução da etapa 6 — 18/11/2026

- Substituir a apresentação inicial pela busca pública Angular; criar páginas de busca e comparação por variante, serviço HTTP e contratos tipados, mantendo a identidade visual existente.
- Filtros: texto, categoria, marca, modelo, armazenamento, cor, condição, UF e cidade. A comparação permite selecionar um fornecedor e retornar à busca filtrada. Estado e paginação ficam na URL para recarga, compartilhamento e navegação voltar/avançar.
- Preservar filtros na comparação e recuperar a página original ao voltar. Cancelar requisições anteriores para impedir resultados antigos após navegação rápida.
- Mostrar características completas da variante, preços BRL, quantidade de ofertas, empresa/região/estoque/atualização e todos os empates no menor preço. Nenhum botão de conta/WhatsApp sem funcionalidade nesta etapa.
- Implementar formulários acessíveis, filtros recolhíveis no celular, carregamento, resultado vazio, erro com nova tentativa e validação de parâmetros. Não inserir ofertas fictícias no banco local.
- Validar com testes de HTTP e navegação/renderização, `npm.cmd run format:check`, `npm.cmd test -- --watch=false`, build de produção e revisão visual local em desktop/celular. Atualizar CI para executar testes do frontend e registrar um commit funcional.

## Validação da etapa 6 — 18/11/2026

- Interface pública Angular conectada à API, com rotas de busca e comparação carregadas sob demanda, contratos tipados, serviço HTTP, formulário reativo e componentes de características/paginação.
- Filtros e paginação preservados na URL; voltar à busca recupera a página original. Alterações de filtros reiniciam a paginação e requisições anteriores são canceladas. Seleção de fornecedor ocorre na comparação, sem exigir identificadores digitados pelo visitante.
- Estados de carregamento, vazio, erro com nova tentativa, parâmetro inválido, variante indisponível, falha nas opções e imagem quebrada implementados. Categorias/marcas selecionadas sem ofertas continuam identificáveis no formulário.
- `npm.cmd run format:check`: passou. `npm.cmd test -- --watch=false`: 18 testes passaram. `npm.cmd run build`: passou sem avisos; bundle inicial aproximadamente 280 kB, catálogo carregado separadamente com aproximadamente 63 kB. Limites de tamanho preservados.
- Testes verificam consultas e escape de parâmetros, renderização BRL/características, links filtrados, paginação por botões, limites, restauração por URL, cancelamento, validação, retentativa, empates, comparação vazia/404, opções indisponíveis e imagem quebrada. CI atualizado para executar testes do frontend; execução remota continua pendente.
- Revisão visual no Edge: desktop 1440×1000 e celular 390×844, sem overflow horizontal na busca, comparação e filtros. Navegação por filtros, comparação, empates, retorno e erro de armazenamento verificados no navegador. Labels ajustados para associação explícita aos controles.
- Navegador integrado indisponível por falha de inicialização do ambiente. Revisão concluída com Edge headless e Playwright temporário, sem adicionar dependências ao projeto. Capturas e roteiro de QA ficam em diretório local ignorado pelo Git.
- Busca vazia validada contra a API real via proxy; cenários preenchidos usaram interceptação HTTP somente no navegador. Nenhuma oferta fictícia inserida no banco. API e frontend disponíveis localmente; Docker preserva inicialização manual.
- README, guia `docs/INTERFACE_PUBLICA.md` e documentação da busca atualizados. Backend não alterado nesta etapa; seus 93 testes passaram na etapa 5. Nenhum push ou publicação realizado.

## Execução da etapa 7 — 18/11/2026

- Implementar cadastro/login/logout Angular com sessão por cookie, restauração ao abrir a aplicação, retorno interno à página de origem, formulários validados e proteção CSRF obtida antes das mutações. Não armazenar credenciais/tokens no localStorage nem repetir mutações automaticamente.
- Criar migração e API privada `/api/conta` de favoritos por produto e fornecedores seguidos. PUT/DELETE idempotentes, chaves compostas por proprietário/recurso, paginação, consulta de marcação e isolamento pelo usuário autenticado, sem proprietário informado no payload.
- Favoritar exige produto com oferta elegível; seguir exige fornecedor aprovado com conta ativa. Itens já salvos permanecem listados quando indisponíveis e podem ser removidos. A lista de produtos leva à busca das suas variantes, sem agrupar preços de características diferentes.
- Integrar botões de favoritar/seguir ao catálogo e comparação e criar página Minha lista. Visitantes são encaminhados ao login preservando destino; mutações exigem sessão e CSRF. Bloqueio/expiração removem o acesso aos dados privados.
- Criar contato público por oferta com consulta atual de elegibilidade, mensagem com características da variante e link wa.me corretamente codificado. A interface obtém o link e permite abrir a conversa por ação explícita; sem envio automático de mensagem.
- Validar backend com PostgreSQL descartável (isolamento, duplicação, CSRF, bloqueios, listas indisponíveis e contato); frontend com testes de sessão, navegação, formulários e ações. Executar build/formatação/testes e revisão visual local. Registrar documentação e commits coerentes, sem push/publicação.

## Validação da etapa 7 — 18/11/2026

- Migração V5 cria favoritos por produto e fornecedores seguidos, com chaves compostas, integridade referencial e índices de paginação. API privada aplica o proprietário da sessão, sem escolha pelo payload, e operações PUT/DELETE idempotentes.
- Listas paginadas preservam itens temporariamente indisponíveis, informam disponibilidade atual e permitem remoção. Produtos favoritos abrem a busca por modelo/marca para escolher a variante, sem comparar características diferentes.
- Contato público consulta a view de elegibilidade e retorna link wa.me com mensagem UTF-8 contendo características da variante, sem envio automático. Interface consulta o contato e permite abrir o WhatsApp por ação explícita em nova aba protegida.
- Angular implementa cadastro, login, logout, restauração da sessão, retorno interno, validação, página Minha lista, ações de favorito/seguir e tratamento de falhas. CSRF é obtido antes das mutações; senhas/tokens não são guardados no localStorage. Alterações não são repetidas automaticamente.
- Respostas antigas não sobrescrevem login recente ou marcações alteradas em outro botão. A expiração limpa a identidade e retira a lista privada da tela, redirecionando para login.
- Backend: `mvnw.cmd -B -o verify` passou com 103 testes, sem falhas/erros/ignorados; 10 cenários adicionais verificam isolamento, proprietário, idempotência, indisponibilidade, paginação, sessão/CSRF, bloqueio e contato. Os 93 cenários anteriores continuam passando.
- Frontend: `npm.cmd run format:check`, `npm.cmd test -- --watch=false` e `npm.cmd run build` passaram. São 44 testes, incluindo 26 cenários adicionais de sessão, CSRF, formulários, retorno, listas, sincronização, contato e expiração. Bundle inicial aproximadamente 288 kB, sem avisos de orçamento.
- Revisão visual no Edge em 1440×1000 e 390×844: cadastro/login/logout, CSRF, favoritos/seguidos, retorno, recarga, contato e sessão expirada passaram. Lista e cadastro sem overflow horizontal no celular. Capturas locais revisadas; dados preenchidos foram simulados apenas nas respostas do navegador, sem criar contas/ofertas no banco e sem abrir WhatsApp.
- Migração V5 aplicada com sucesso no banco local e API reiniciada após identificar a instância anterior deste projeto na porta 8080. Saúde UP, endpoints presentes no OpenAPI e lista sem autenticação retorna 401. PostgreSQL saudável e `restart=no` preservado.
- README, guias de interface/autenticação/busca e `docs/CONTA_E_CONTATO.md` atualizados. Frontend disponível em http://127.0.0.1:4200. Pipeline remoto não executado; nenhum push ou publicação realizado.

## Execução da etapa 8 — 18/11/2026

- Criar painel Angular com cadastro/perfil da empresa, ofertas, catálogo de variantes e solicitações de inclusão. Compradores acessam o cadastro; fornecedores acessam as áreas próprias; administradores mantêm seu fluxo separado. A API continua aplicando autorização.
- Cadastro de empresa invalida a sessão e encaminha ao novo login. Exibir situação/motivo, regras de reaprovação por identidade e estados de carregamento/erro. Perfil bloqueado somente em consulta.
- Selecionar variante completa do catálogo, criar oferta e editar preço/estoque/disponibilidade. Excluir com confirmação na tela e versão atual. Informar moderação sem permitir edição pelo fornecedor. Validar moeda decimal e estoque inteiro sem arredondamento silencioso; conflitos preservam dados digitados e exigem recarga explícita.
- Preservar texto/paginação na URL e cancelar consultas antigas. Manter versão recebida da API, feedback por operação e bloqueio de envio duplicado. Retirar o painel privado ao expirar a sessão.
- Para variante ausente, implementar solicitação com nome, marca, modelo, categoria e características textuais, vinculada à empresa aprovada. Limitar a 10 pendentes por empresa, impedir duplicação pendente sob bloqueio transacional e listar somente solicitações próprias. A análise, criação/vinculação da variante e resposta administrativa serão implementadas na etapa 10; nenhum produto compartilhado é criado automaticamente.
- Validar API nova em PostgreSQL descartável e testes Angular dos formulários, perfis, situações, ofertas, exclusão/conflito e solicitações. Executar formatação/build/testes, revisar desktop/celular no Edge e atualizar documentação antes do commit local.

## Validação da etapa 8 — 18/11/2026

- Painel acessível pelo cabeçalho: “Quero vender” para compradores e “Painel do fornecedor” para fornecedores. Rotas privadas de empresa, ofertas, catálogo e solicitações; guardas por perfil, com autorização também na API.
- Cadastro da empresa exige novo login; perfil permite edição conforme a situação. Banner informa pendência/bloqueio/motivo. Alteração de identidade informa reaprovação; leitura de situação preserva campos já editados.
- CRUD de ofertas conectado à API, com variante selecionada, validação de moeda/estoque, disponibilidade, moderação somente consultável, confirmação de exclusão e versões. Conflitos mantêm os dados digitados e impedem novo envio até recarga explícita. Consultas anteriores são canceladas ao paginar; expiração retira o painel privado da tela.
- Migração V6 e API de solicitações implementadas. Fornecedor aprovado envia dados descritivos; empresa da sessão define o proprietário. Até 10 pendentes, sem duplicação pendente normalizada, protegidos por bloqueio transacional da empresa. Históricos isolados e paginados. Inclusão/vinculação de variante e resposta administrativa continuam na etapa 10; não há criação automática de produto/oferta.
- Backend: `mvnw.cmd -B -o verify` passou com 112 testes, sem falhas/erros/ignorados. Os 9 cenários novos incluem isolamento, payload sem escolha de proprietário/situação, duplicidade, limites sob concorrência, aprovação, CSRF, validação e paginação. Os 103 testes anteriores continuam passando.
- Frontend: `npm.cmd run format:check`, `npm.cmd test -- --watch=false` e `npm.cmd run build` passaram. São 69 testes, incluindo 25 cenários adicionais de cabeçalho/perfis, cadastro/relogin, aprovação/bloqueio, valores decimais, versões/conflito, confirmação de exclusão, cancelamento de consultas, catálogo e solicitações. Bundle inicial aproximadamente 304 kB, sem avisos de orçamento.
- Edge revisado em 1440×1000 e 390×844: entrada pelo cabeçalho, cadastro/novo login, atualização de situação, variante incompleta, criação/validação/edição de oferta, conflito/recarga, exclusão confirmada, solicitação, bloqueio e expiração. Capturas inspecionadas; sem overflow horizontal nos formulários e histórico móveis.
- Bootstrap anônimo validado contra a API real; dados preenchidos da revisão foram simulados somente no navegador. Nenhuma empresa/oferta/solicitação de demonstração inserida no banco local.
- Execução local confirmada: V6 aplicada, saúde UP, frontend HTTP 200, solicitações presentes no OpenAPI e acesso sem login retorna 401. PostgreSQL saudável com `restart=no` preservado.
- README e guias atualizados, incluindo `docs/PAINEL_DO_FORNECEDOR.md`. Nenhum push, publicação ou execução remota do pipeline realizado.

## Dados demonstrativos completos — 18/11/2026

- Por solicitação de Paulo, popular o banco local para explorar os fluxos já entregues, preservando registros existentes.
- Estender somente o perfil explícito `demo`: manter o catálogo de 3 produtos/6 variantes e adicionar comprador, 5 fornecedores (3 aprovados, 1 pendente, 1 bloqueado), 20 ofertas (14 elegíveis), favoritos, fornecedores seguidos e uma solicitação pendente.
- Usar IDs estáveis e inserções que não sobrescrevem alterações. Credenciais demonstrativas documentadas, com BCrypt; emails/telefones fictícios. Não criar administrador nem habilitar seed no perfil padrão.
- Verificar idempotência, preservação de alterações e elegibilidade em PostgreSQL descartável antes de carregar o banco local. Reiniciar a API com perfil demo e confirmar busca, comparação, empates e login das contas de exemplo.
- Revisar a interface com dados reais da carga, documentar acessos e registrar commit próprio. A próxima etapa funcional continua sendo importação CSV/XLSX.

## Validação dos dados demonstrativos — 18/11/2026

- Perfil `demo` ampliado com carga repetível de contas, fornecedores, ofertas e listas. Mantém somente inserções por IDs estáveis, sem sobrescrever registros existentes. Perfil padrão continua sem carga automática e nenhum administrador foi criado.
- `mvnw.cmd -B -o verify`: BUILD SUCCESS, 114 testes, sem falhas/erros/ignorados. Os dois cenários novos verificam carga repetida, contas/hash, favoritos/seguidos, 14 ofertas elegíveis, empate e preservação de preço/estoque/nome editados.
- Banco local inicialmente vazio, conforme contagem prévia. API reiniciada explicitamente com `-Dspring-boot.run.profiles=demo`; Flyway aplicou as cargas no schema correto. Contagens reais: 6 contas, 5 empresas, 3 produtos, 6 variantes, 20 ofertas, 14 elegíveis, 2 favoritos, 2 fornecedores seguidos e 1 solicitação pendente.
- Revisão no Edge contra API/banco reais, sem interceptação: busca com 6 variantes; comparação com 3 fornecedores e 2 empates; filtro SP; login de comprador e fornecedores aprovado/pendente/bloqueado; favoritos mantidos após recarga; painel Paulista com 6 ofertas e perfil bloqueado sem edição. Nenhum dado comercial foi alterado durante a revisão.
- Capturas de desktop/celular inspecionadas; PostgreSQL saudável, `restart=no` preservado. API permanece executando com perfil demo e interface disponível em http://127.0.0.1:4200.
- Emails/telefones são fictícios; credenciais e roteiro estão em `docs/DADOS_DEMONSTRACAO.md`. README e guias corrigidos para refletir a carga completa. Frontend não foi alterado; seus 69 testes passaram na etapa anterior. Nenhum push ou publicação realizado.

## Execução da etapa 9 — 19/11/2026

- Importar CSV UTF-8 separado por ponto e vírgula e XLSX com uma planilha. Modelo com colunas `varianteId;preco;estoque;disponivel`, identificação explícita da variante, preço decimal sem milhares, estoque inteiro e disponibilidade true/false.
- Limitar arquivos a 2 MB e 500 linhas; recusar fórmulas, formatos diferentes e cabeçalhos incorretos. Validar cada linha, variante completa, duplicidade no arquivo e limites já usados nas ofertas.
- Persistir prévia própria por 30 minutos, com ações CRIAR/ATUALIZAR/IGNORAR e erros por linha. Confirmar apenas lote integralmente válido, com aprovação atual da empresa e versões revalidadas. Bloquear empresa antes das ofertas; aplicar tudo em uma transação. Confirmação repetida retorna resultado aplicado, sem duplicar gravações.
- Reimportação mantém valores iguais sem alterar versão; atualizações preservam moderação; novas ofertas ficam pendentes. Conflitos exigem nova prévia. Não identificar variante por aproximação textual.
- Criar tela privada com modelos CSV/XLSX, seleção do arquivo, prévia, erros e confirmação explícita. Mostrar código para importação no catálogo. Preservar exemplos do banco local.
- Validar parser e transações em PostgreSQL descartável, autorização/CSRF/isolamento/conflitos/idempotência e interface Angular. Executar verify, formatação, testes e build; documentar formato e registrar commit local coerente.

## Validação da etapa 9 — 19/11/2026

- Importação CSV UTF-8/semicolon e XLSX, modelos autenticados, códigos explícitos no catálogo e tela privada com upload, prévia persistida, erros por linha, confirmação e resultado. Links permitem restaurar a prévia após recarga.
- Limites de 2 MB, 500 ofertas, quatro colunas e 256 caracteres por campo. XLSX exige uma planilha, sem fórmulas/macros/vínculos externos; estrutura descompactada limitada a 16 MB/1.000 entradas, mantendo proteções do POI. CSV exige uma linha por oferta.
- Migração V7 guarda prévias por empresa. Aprovação atual, isolamento por proprietário, até 10 pendentes e validade de 30 minutos. Limpeza de registros vencidos ao preparar novo arquivo; resultados não constituem histórico permanente.
- Lote integralmente válido é aplicado em transação, com empresa bloqueada antes de ofertas e revalidação de identidade/versão/completude. Duplicatas são inválidas, novos registros ficam pendentes, atualizações preservam moderação e valores iguais não alteram versão/data. Repetição da confirmação disponível retorna o resultado sem duplicar.
- Backend: `mvnw.cmd -B -o verify` passou com 125 testes, zero falhas/erros/ignorados. Onze cenários novos verificam criação somente ao confirmar, CSV/XLSX, fórmulas, formatos/cabeçalho/limites, duplicatas, valores, moderação, reimportação, rollback integral, conflitos, isolamento, sessão/CSRF, bloqueio, expiração e confirmação simultânea. Testes usam PostgreSQL descartável.
- Frontend: `npm.cmd run format:check`, `npm.cmd test -- --watch=false` e `npm.cmd run build` passaram. São 77 testes, oito novos para FormData/CSRF, confirmação separada, bloqueio de duplo envio, erros por linha, conflito/nova prévia, falha de conexão, formatos/tamanho, situação da empresa e restauração pela URL.
- Edge em desktop 1440×1000 e celular 390×844: upload, erros, confirmação explícita, conflito, nova prévia, conclusão/recarga, código da variante, bloqueio e expiração revisados. Capturas inspecionadas e nenhuma rolagem horizontal. Mutações dessa revisão foram simuladas nas respostas HTTP do navegador.
- API iniciada com perfil demo, V7 aplicada e saúde UP. Conta demo Paulista validada na interface contra API real: login, acesso à importação, modelos CSV/XLSX e códigos no catálogo. Somente consultas/login/logout; nenhuma importação/oferta local criada. Contagens preservadas: 6 usuários, 5 empresas, 20 ofertas e 14 elegíveis; zero prévias locais.
- Docker Desktop estava desligado e foi iniciado para os testes; banco do projeto iniciado manualmente, saudável e com `restart=no`. Frontend disponível em http://localhost:4200 e API em http://localhost:8080.
- Guia `docs/IMPORTACAO_DE_OFERTAS.md`, README, painel e planejamento atualizados. Dependências fixas Apache POI 6.0.0 e Commons CSV 1.14.1, conferidas nas fontes oficiais. Nenhum push, publicação ou pipeline remoto executado.

## Execução da etapa 10 — 19/11/2026

- Entregar painel privado ADMIN com usuários/permissões, fornecedores, ofertas, catálogo, solicitações, indicadores, relatório e configuração de contato. Reutilizar sessão, CSRF, paginação e versões; remover dados privados ao perder sessão/perfil.
- Primeiro fluxo: usuários com ativação/bloqueio e mudança de perfil versionadas, confirmação explícita e invalidação de sessões. Proibir autobloqueio e alteração do próprio perfil; atribuir FORNECEDOR somente com empresa cadastrada. Moderação mantém transições e versões existentes e exige motivo de bloqueio.
- Catálogo: CRUD de categorias/marcas/produtos/variantes, validação de identidade/características, imagem HTTP(S), paginação e controle otimista. Impedir exclusão de registros referenciados. Impedir mudança de identidade de variante já ofertada e mudanças estruturais de categoria/produto em uso; permitir correção de apresentação. Serializar operações administrativas de catálogo para preservar as verificações entre administradores.
- Solicitações: listar pendentes/finalizadas com fornecedor, características e versão; atender vinculando variante completa existente, ou recusar com motivo. Revisar correspondência explicitamente, sem aproximação automática. Finalização única com versão; resposta aparece no histórico do fornecedor.
- Indicadores são fotografia do banco: contas ativas, empresas por situação, ofertas por moderação/elegibilidade, produtos/variantes e solicitações pendentes. Relatório CSV agregado por UF, sem dados pessoais, sem inventar vendas/cliques. Configuração inicial: texto de abertura da mensagem de contato WhatsApp, com validação e versão, mantendo características e link construídos pelo sistema.
- Criar migração de versões do catálogo e configuração. Não criar admin com senha fixa; manter bootstrap por ambiente documentado e usar dados simulados no navegador para ações de QA. Preservar todos os exemplos locais.
- Validar API em PostgreSQL descartável (ADMIN/CSRF, perfis/sessões, versões, referências, moderação, resolução, métricas/configuração), componentes Angular e Edge desktop/celular. Executar verify, format:check, testes e build. Registrar commits coerentes e guias; nenhum push/publicação.

## Validação das APIs da etapa 10 — 19/11/2026

- Migração V8 adiciona versões às quatro entidades do catálogo e configuração única de abertura do contato. APIs administrativas de CRUD, resolução, acesso, indicadores, relatório e configuração implementadas.
- Alterações de acesso invalidam sessões, conferem versão, protegem o próprio administrador e serializam/revalidam o autor. Atribuição do perfil fornecedor exige empresa existente. O endpoint legado de situação permanece compatível.
- Catálogo preserva referências e identidade em uso; escrita administrativa serializada, controle otimista e bloqueios de variante compatíveis com criação/importação de ofertas. Prévia de importação captura também a versão da variante.
- Solicitações só são finalizadas uma vez, com resposta e versão; atendimento exige variante completa. Indicadores/CSV agregam dados reais e configuração altera somente a abertura da mensagem, mantendo elegibilidade e composição das características.
- `mvnw.cmd -B -o verify`: BUILD SUCCESS, 138 testes, zero falhas/erros/ignorados. Após ajustes finais de campos descritivos do seletor e imports, `mvnw.cmd -B -o -Dtest=AdministracaoIntegrationTest verify` também passou com os 13 testes administrativos. Cenários incluem autorização/CSRF, versões, unicidade/referências, preservação da identidade, sessão/perfis, bloqueios concorrentes mantendo administrador ativo, respostas no histórico, métricas/configuração e alteração da variante após prévia.
- Primeiro teste detectou parâmetro SQL opcional sem tipo na lista de solicitações; corrigido para VARCHAR explícito e revalidado. Todos os testes de autenticação, fornecedor, catálogo e importação existentes passaram na suíte completa.
- API local reiniciada com demo, V8 aplicada e saúde UP. Primeiro administrador local criado pelo bootstrap com senha aleatória em arquivo ignorado; nenhum administrador adicionado às cargas demo e nenhuma senha incluída no código. Os seis usuários demo, cinco empresas, três produtos, seis variantes, vinte ofertas, quatorze elegíveis e uma solicitação foram preservados.

## Validação da interface da etapa 10 — 19/11/2026

- Painel Angular em `/admin`, acessível pelo cabeçalho somente para ADMIN, com guardas e retirada dos dados privados ao expirar sessão/mudar perfil. Navegação para visão geral, usuários, fornecedores, ofertas, catálogo, solicitações e configuração.
- Listas paginadas, filtros por situação, revisão antes de confirmar, motivo de bloqueio, versões e conflitos que preservam o formulário até descarte/recarga. Proteção contra envio duplicado e confirmação explícita para exclusão. Seletores de referência paginados, com nome/características dos registros e sem limite artificial à primeira página.
- Indicadores reais, CSV agregado por estado e configuração versionada do contato implementados. Solicitações finalizadas pela API aparecem no histórico do fornecedor; atendimento seleciona variante completa explicitamente, sem criar ofertas.
- Frontend: `npm.cmd run format:check`, `npm.cmd test -- --watch=false` e `npm.cmd run build` passaram, com 92 testes. São 15 casos adicionais de acesso por perfil, CSRF/versões, confirmação, motivos, filtros/cancelamento, conflito/recarga, catálogo/atributos/exclusão, referências paginadas, configuração e navegação em expiração/logout. Bundle inicial aproximadamente 308 kB, sem avisos de orçamento.
- Edge desktop 1440×1000 e celular 390×844: indicadores, perfis, moderação, motivos/conflitos, catálogo/seletores/exclusão, resolução de solicitação e configuração/recarga/expiração revisados com respostas HTTP simuladas. Capturas inspecionadas e sem overflow horizontal.
- Revisão adicional contra API/banco reais: login do administrador local, 7 contas/14 ofertas elegíveis, usuários, fornecedores, ofertas, catálogo/variantes descritivas, solicitação demo, configuração e CSV por UF. Nenhuma mutação comercial: apenas consultas e login/logout. Conflito entre redirecionamentos de logout foi encontrado nesta revisão, corrigido e coberto por teste; expiração mantém retorno ao login e saída voluntária volta à busca.
- API saudável e serviços disponíveis em http://localhost:8080 e http://localhost:4200. PostgreSQL saudável com `restart=no`. Preservados 5 fornecedores, 3 produtos, 6 variantes, 20 ofertas, 14 elegíveis e 1 solicitação; o único acréscimo local foi o administrador inicial via bootstrap, com senha aleatória e credenciais em arquivo ignorado pelo Git.
- README e guias atualizados, incluindo `docs/ADMINISTRACAO.md`. Backend registrado separadamente em `d6aff23`; esta entrega da interface fecha a etapa 10. Nenhum push, publicação ou pipeline remoto realizado. Próxima etapa: validação integrada e preparação para publicação.

## Execução da etapa 11 — 19/11/2026

- Conferir requisitos e validar o percurso integrado comprador/fornecedor/administrador, incluindo comparação, favoritos, contato, aprovação, moderação, importação e resolução de solicitação. Usar banco de homologação isolado, sem alterar exemplos locais.
- Revisar logout e expiração nos painéis e na lista privada. Corrigir a navegação de saída voluntária mantendo retorno ao login para expiração; testar os comportamentos.
- Preparar imagens Docker a partir dos artefatos validados, API Java sem root e frontend Angular servido por Nginx sem root. Contextos de build restritos, sem segredos/fontes desnecessárias. Profile de produção sem Swagger público e cookies Secure.
- Criar Compose de produção independente: banco sem porta publicada, API interna, frontend em loopback para proxy TLS externo, volume próprio, healthchecks, limites de logs e restart=no para todos. Validar build e subida manual local, sem publicar imagens nem servidor.
- Documentar configuração, primeiro admin, domínio/TLS, atualização/reversão, backup e restauração. Testar backup/restore em banco isolado. Nunca remover volumes existentes.
- Executar regressão backend/frontend, build/formatação, revisão Edge desktop/celular sobre a distribuição final e registrar critérios de aceite e pendências. Publicação externa depende de destino/acessos ainda não informados; não marcar publicação como concluída.
- Commits previstos: correções dos fluxos integrados; infraestrutura e documentação de implantação validada.

## Validação dos fluxos integrados — 19/11/2026

- Logout voluntário das áreas do fornecedor e Minha lista agora retorna à busca; expiração mantém retorno ao login e remove os dados privados. Comportamento alinhado ao painel administrativo e coberto por dois testes adicionais.
- `mvnw.cmd -B -o verify`: BUILD SUCCESS, 138 testes, zero falhas/erros/ignorados. Frontend: formatação, 94 testes e build passaram, sem avisos de orçamento.
- Roteiro HTTP integrado executado contra distribuição Docker em banco próprio: cadastro e novo login após cadastro da empresa, aprovação, catálogo/variante, oferta/moderação, comparação, favoritos/seguidos, contato, importação sem gravação antes da confirmação, confirmação repetida idempotente, solicitação atendida, bloqueio retirando oferta/contato e reaprovação recuperando publicação. Nenhum WhatsApp aberto ou mensagem enviada.
- Banco de desenvolvimento preservado: 7 contas (incluindo admin local), 5 empresas, 20 ofertas e 14 elegíveis. Homologação usa projeto/volume independentes, sem carga demo.

## Validação da implantação local — 19/11/2026

- Dockerfiles com contextos restritos, API Java 25 e Nginx sem root, filesystem somente leitura e healthchecks. Imagens oficiais com versões fixas, conferidas no registry. Compose de produção independente, volume próprio, banco/API internos e Nginx somente em loopback; restart=no em todos os serviços.
- Perfil producao sem documentação Swagger/OpenAPI pública, sem seed demo e com cookies Secure. Pipeline preparado para construir as imagens após testes/build, sem publicação de imagens.
- Distribuição validada em localhost:8088, incluindo roteiro HTTP integrado, Edge desktop/celular e recarga de rotas profundas. Cookies Secure/HttpOnly/Lax, headers, health e bloqueio de endpoints internos conferidos. Bootstrap inicial removido do ambiente após criação do admin; reinício e login revalidados.
- Backup/restore executados de verdade em homologação: banco original e novo restaurado com 5 contas, 2 ofertas e 1 elegível. Backup existente recusado; scripts não apagam/sobrescrevem banco ativo. Nenhum volume ou dado do ambiente original removido.
- Documentação de configuração, TLS externo, atualização/reversão, backup/restauração e roteiro integrado em docs/IMPLANTACAO.md; critérios/evidências em docs/VALIDACAO_FINAL.md. README atualizado, incluindo correção da antiga próxima etapa.
- Alteração de logout registrada em 440ee06. Infraestrutura e documentação fecham a etapa local; publicação externa continua pendente de servidor/domínio, conforme resposta explícita de Paulo. Nenhum push, registry, DNS ou servidor externo utilizado.

## Evolução CI/CD — 19/11/2026

- Escolha de Paulo: CI automático, imagens no GHCR automaticamente após validação da main e deploy manual. Repositório remoto já configurado: paulovargas/COMPARA_PRECO.
- Ampliar workflow existente com artefatos dos builds validados, publicação dependente de ambos os jobs, tags por SHA completo e permissões de pacote somente no job de publicação. Pull requests não publicam nem acessam credenciais de deploy.
- Criar workflow manual de deploy para SHA publicado, restrito à main e Environment producao. SSH com host key previamente cadastrada, parâmetros validados e credenciais via Secrets/Variables. Nenhum deploy é disparado nesta entrega; servidor ainda não definido.
- Usar Compose de registry sem build no servidor, imagens da mesma versão para API/web, bootstrap/segredos mantidos no host, backup prévio obrigatório e healthchecks. Preservar volume e restart=no. Não fazer rollback automático de schema.
- Validar workflows com actionlint, shell do deploy e composição de registry localmente. Reutilizar os testes/builds já aprovados; executar verificações adicionais quando exigidas pelas alterações. Documentar setup, permissões, proteção da main, publicação, deploy e recuperação.
- Commit previsto: ci: configura publicacao no GHCR e deploy manual. Sem push, alteração de Secrets/Settings ou execução remota automática nesta sessão.

## Validação da evolução CI/CD — 19/11/2026

- CI existente ampliado com artefatos das imagens realmente construídas após Maven/frontend. Publicação automática apenas na main após ambos aprovados, usando GITHUB_TOKEN com packages:write somente no job de publicação. Nomes GHCR em minúsculas, tags SHA completo e labels de origem/revisão, sem latest.
- Deploy manual preparado, restrito à main e Environment producao. SSH usa chave temporária, known_hosts validado e parâmetros conferidos; não há coleta automática de host key. Overlay de registry remove build, mantém volume/projeto e restart=no.
- Script remoto serializa com flock, baixa/confere revisão das imagens, produz backup prévio com permissões restritas, aguarda healthchecks e verifica proxy/API antes de registrar current-release. Falhas não disparam downgrade automático de schema; recuperação documentada.
- Actions atuais consultadas nas releases oficiais, resolução dos SHAs conferida e inputs/runtime Node24 verificados nos manifests oficiais. Workflows aprovados por actionlint 1.7.11 com ShellCheck; script aprovado por bash -n e ShellCheck. Casos negativos recusaram SHA inválido, diretório raiz e ambiente ausente antes de qualquer deploy.
- Compose de registry resolvido localmente: api/web sem build, todas as políticas restart=no preservadas. Dry-run sobre homologação indicou serviços saudáveis sem modificar containers. Nenhuma alteração de código da aplicação; não repetidos os 138 testes backend/94 frontend já aprovados na etapa anterior.
- README e docs/CI_CD.md atualizados com setup de Actions, GHCR, proteção de main, Environment, Secrets/Variables e operação/recuperação. Commit local preparado; sem push, publicação de pacote, mudança de Settings/Secrets ou conexão SSH nesta entrega. Execução remota ainda depende de envio do commit e configuração do servidor.
