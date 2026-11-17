# Planejamento — Compara Preço

## Objetivo e situação atual

Conectar compradores a fornecedores por meio de busca, comparação de ofertas equivalentes e contato pelo WhatsApp, conforme `descricao.md`.

O repositório contém apenas a descrição. Nenhuma implementação foi iniciada e o diretório ainda não possui Git. Este documento orienta as entregas e deve ser atualizado a cada etapa.

## Direção técnica proposta

- Backend: Java com Spring Boot, API REST, validação de DTOs e responsabilidades separadas entre controllers, services e repositories.
- Frontend: Angular com TypeScript, componentes standalone e organização por funcionalidades.
- Persistência: PostgreSQL, migrações versionadas e dados de demonstração separados dos dados de produção.
- Execução local: Docker Compose para banco e, posteriormente, aplicação; configuração por ambiente e exemplos sem credenciais reais.
- Documentação: README em português e OpenAPI para a API.

Esta proposta substitui a sugestão preliminar de Python/SQLite e aproxima o projeto das convenções de Paulo para aplicações full stack. As versões e dependências serão verificadas e fixadas na preparação técnica, antes da geração dos projetos.

Ambiente observado: Java 11, Maven 3.8.8, Node 22.15.1, Python 3.13.15 e cliente Docker instalado. Precisamos verificar disponibilidade de JDK compatível com a versão escolhida de Spring Boot e funcionamento do Docker. No PowerShell, usar `npm.cmd` para evitar o bloqueio de `npm.ps1`, sem alterar a política do sistema.

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
| 0 | Planejamento registrado, escopo e decisões iniciais explícitos | `docs: registra planejamento do projeto` | Documento criado |
| 1 | Git inicializado, stack verificada, estrutura backend/frontend, configurações de ambiente, banco local e README inicial; aplicações compilam e banco inicia | `chore: prepara estrutura e ambiente de desenvolvimento` | Pendente |
| 2 | Migrações, catálogo e variantes com restrições de integridade; dados demonstrativos reproduzíveis | `feat: modela catalogo e variantes de produtos` | Pendente |
| 3 | Cadastro, autenticação, encerramento de sessão, perfis e autorização; testes de acesso indevido e bloqueio | `feat: implementa autenticacao e permissoes` | Pendente |
| 4 | Cadastro e aprovação de fornecedor, perfil e CRUD de ofertas com isolamento por proprietário | `feat: implementa fornecedores e gerenciamento de ofertas` | Pendente |
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

- Confirmar versões da stack e ferramentas compatíveis na etapa 1.
- Detalhar atributos de categorias além de smartphones ao implementar variantes.
- Definir regras de prazo de atualização das ofertas e tratamento de ofertas antigas antes da liberação em produção.
- Definir métricas, formatos de relatórios e configurações administrativas durante o detalhamento do painel.
- Para publicação efetiva: definir servidor, domínio, acesso, TLS, armazenamento de imagens, estratégia de backup e credenciais por ambiente. Preparar os arquivos localmente enquanto esses dados não estiverem disponíveis.

## Próxima entrega

Etapa 1: verificar e fixar a stack, preparar Git e a estrutura de desenvolvimento, documentar a execução e validar a base antes de implementar regras de negócio.
