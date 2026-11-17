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
| 5 | API de busca, filtros, paginação e comparação; testes de equivalência, empates e ofertas inelegíveis | `feat: implementa busca e comparacao de precos` | Pendente |
| 6 | Interface pública responsiva com categorias, busca, filtros e comparação conectadas à API; estados de carregamento, vazio e erro | `feat: cria experiencia de busca e comparacao` | Pendente |
| 7 | Autenticação na interface, favoritos, fornecedores seguidos e WhatsApp; persistência por usuário | `feat: adiciona conta favoritos e contato` | Pendente |
| 8 | Painel do fornecedor para empresa, catálogo, preços, estoque e disponibilidade | `feat: cria painel do fornecedor` | Pendente |
| 9 | CSV/XLSX com modelo de arquivo, limites, validação, prévia e confirmação; testes de arquivos inválidos e duplicidades | `feat: adiciona importacao de ofertas` | Pendente |
| 10 | Painel administrativo com cadastros, moderação, bloqueio, permissões, estatísticas, relatórios e configurações | `feat: cria painel administrativo` | Pendente |
| 11 | Fluxos integrados validados, documentação completa, configuração de produção, backup e roteiro de publicação | `docs: documenta validacao e publicacao` e correções específicas | Pendente |

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

Etapa 5: implementar busca pública, filtros, paginação e comparação por variante, usando a mesma fonte de ofertas elegíveis para preços e contagens.

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
