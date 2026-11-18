# Compara Preço

Plataforma para buscar produtos, comparar ofertas equivalentes e entrar em contato com fornecedores pelo WhatsApp. O escopo e a ordem das entregas estão no [planejamento](PLANEJAMENTO.md); os requisitos originais estão em [descricao.md](descricao.md).

## Entrega atual

API com catálogo, autenticação, permissões, fornecedores, moderação, ofertas e busca pública. A interface Angular permite buscar produtos, aplicar filtros, paginar resultados e comparar ofertas equivalentes, com destaque para todos os empates no menor preço. As telas são responsivas e conectadas à API; conta, favoritos, WhatsApp e painéis serão entregues nas próximas etapas.

Abra [http://127.0.0.1:4200](http://127.0.0.1:4200) após iniciar os serviços abaixo. A busca mostra apenas ofertas publicadas e elegíveis; um banco sem ofertas retorna o estado vazio, sem produtos fictícios. Veja o [guia da interface](docs/INTERFACE_PUBLICA.md).

## Stack

- Java 25 e Spring Boot 4.1.2.
- Maven Wrapper 3.10.1: não é necessário instalar Maven globalmente.
- Angular e Angular CLI 21.2.26, TypeScript 5.9 e Node 22.15.1.
- PostgreSQL 17.7 em Docker, Spring Data JPA e Flyway.
- Springdoc OpenAPI 3.2.0, Spring Security e Spring Session JDBC.

Versões conferidas nas fontes oficiais: [requisitos do Spring Boot](https://docs.spring.io/spring-boot/system-requirements.html), [compatibilidade do Angular](https://angular.dev/reference/versions) e [compatibilidade do Springdoc](https://springdoc.org/#what-is-the-compatibility-matrix-of-springdoc-openapi-with-spring-boot). O Node instalado é compatível com Angular 21; Angular 22 exige uma versão superior do Node. O lockfile fixa as dependências do frontend. Para atualizar dependências, a versão de npm usada na preparação foi 11.21.2, executada via `npm.cmd exec --yes --package=npm@11.21.2 -- npm install`, sem instalação global.

## Estrutura

```text
backend/                 API, configurações, migrações e testes de integração
frontend/                Aplicação Angular standalone e rotas
.github/workflows/       Validação em GitHub Actions
compose.yml              Banco de desenvolvimento com volume persistente
.env.example             Modelo de configuração local
PLANEJAMENTO.md          Etapas, critérios de aceite e decisões
```

## Pré-requisitos

JDK 25, Node compatível com Angular 21, npm e Docker Desktop iniciado com containers Linux. No Windows, use `npm.cmd`: isso evita o bloqueio de scripts PowerShell sem mudar a política da máquina.

O Java padrão desta máquina é 11. Antes dos comandos do backend, selecione o JDK 25 **somente no terminal atual**, ajustando o caminho caso necessário:

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\temurin-25.0.4.1"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version
```

## Executar localmente

### 1. Banco

Na raiz, caso ainda não exista `.env`:

```powershell
Copy-Item .env.example .env
```

Edite a senha em `.env` antes de iniciar o banco. Esse arquivo não é versionado. Na preparação deste ambiente, ele foi criado com uma senha local aleatória.

```powershell
docker compose config --quiet
docker compose up -d --wait postgres
docker compose ps
```

Se `docker compose` não estiver disponível no PATH, use o executável instalado com Docker Desktop:

```powershell
& 'C:\Program Files\Docker\Docker\resources\bin\docker-compose.exe' up -d --wait postgres
```

Os containers deste projeto usam `restart: "no"`: abrir ou reiniciar o Docker Desktop não inicia o banco automaticamente. Para subir manualmente, execute `docker compose up -d --wait postgres` na raiz ou use o botão de iniciar no Docker Desktop. A configuração também se aplica quando o container for recriado.

### 2. Backend

Após selecionar o JDK, abra um terminal na pasta `backend`:

```powershell
Set-Location backend
.\mvnw.cmd spring-boot:run
```

A aplicação lê o `.env` da pasta pai. Variáveis de ambiente têm precedência. Em ambientes externos, configure `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`; não é preciso distribuir o arquivo `.env`.

- API: <http://localhost:8080> (endpoints de autenticação e administração de usuários).
- Saúde da aplicação e do banco: <http://localhost:8080/actuator/health>.
- Swagger UI: <http://localhost:8080/swagger-ui.html>.
- Contrato OpenAPI: <http://localhost:8080/v3/api-docs>.
- [Autenticação, CSRF, endpoints e primeiro administrador](docs/AUTENTICACAO.md).
- [Cadastro de fornecedores, ofertas, moderação e exemplos](docs/FORNECEDORES_E_OFERTAS.md).

### 3. Frontend

Em outro terminal, partindo da raiz:

```powershell
Set-Location frontend
npm.cmd ci
npm.cmd start
```

Acesse <http://localhost:4200>. O proxy de desenvolvimento encaminha `/api/**` ao backend em `localhost:8080`. Ele não substitui a configuração do proxy reverso de produção.

## Configuração

| Variável | Uso | Padrão |
| --- | --- | --- |
| `POSTGRES_DB` | Banco local | `compara_preco` |
| `POSTGRES_USER` | Usuário local | `compara_preco` |
| `POSTGRES_PASSWORD` | Senha local do banco | Obrigatória |
| `POSTGRES_PORT` | Porta local do banco | `15432` |
| `DB_URL` | URL JDBC, sobrescreve configuração local | Derivada de `POSTGRES_*` |
| `DB_USERNAME` | Usuário JDBC | `POSTGRES_USER` |
| `DB_PASSWORD` | Senha JDBC | `POSTGRES_PASSWORD` |
| `SERVER_PORT` | Porta HTTP do backend | `8080` |
| `SESSION_COOKIE_SECURE` | Cookie de sessão somente em HTTPS | `false` local; use `true` em produção |
| `APP_ADMIN_HABILITADO` | Habilita criação do primeiro administrador | `false` |
| `APP_ADMIN_NOME` | Nome do primeiro administrador | `Administrador` |
| `APP_ADMIN_EMAIL` | Email do primeiro administrador | Obrigatório se habilitado |
| `APP_ADMIN_SENHA` | Senha inicial, via ambiente | Obrigatória se habilitado |
| `API_DOCS_ENABLED` | Disponibiliza OpenAPI e Swagger | `true` |

O banco é exposto apenas em `127.0.0.1`. O volume mantém os dados ao parar os containers. Alterar a senha em `.env` depois da criação do volume não altera a senha do usuário já criado no PostgreSQL. JPA valida a estrutura; somente migrações Flyway alteram o schema. O histórico Flyway usa explicitamente `compara_preco`, sem depender do `search_path` do usuário PostgreSQL. A primeira migração cria o schema `compara_preco`; a segunda cria o catálogo e suas restrições de integridade; a terceira cria usuários, sessões JDBC e contadores de acesso; a quarta cria fornecedores, ofertas, regras de categoria e a view de elegibilidade. Veja as [regras do catálogo](docs/CATALOGO.md).

## Validar

Com o Docker Desktop iniciado (não é necessário iniciar o banco do Compose para os testes):

```powershell
Set-Location backend
.\mvnw.cmd -B verify
```

No Windows, encerre antes de empacotar uma instância iniciada com `java -jar` que use o JAR de `target`, pois ela pode bloquear o arquivo. Prefira `spring-boot:run` para desenvolver.

Os testes de integração usam Testcontainers para criar um PostgreSQL temporário em uma porta dinâmica, com banco e credenciais isolados. Não utilizam o banco do Compose nem as credenciais de `DB_*`. Verificam saúde HTTP, migrações, OpenAPI, persistência, restrições de integridade e carga demo. Os testes de domínio verificam normalização e validação das variantes. Os containers temporários são removidos ao encerrar os testes e não têm reinício automático. O primeiro teste pode baixar imagens Docker e dependências Maven.

Em outro terminal, a partir da raiz:

```powershell
Set-Location frontend
npm.cmd ci
npm.cmd run format:check
npm.cmd run build
```

Ainda não há testes de comportamento do frontend: serão adicionados junto aos fluxos funcionais. O pipeline executa os testes do backend com PostgreSQL isolado via Testcontainers e verifica formatação e build do frontend. O pipeline será executado no GitHub quando houver um remoto configurado e os commits forem enviados.

## Dados de demonstração

A carga demo fica separada das migrações de estrutura. Em um banco de desenvolvimento dedicado, inicie a API com o perfil explícito, a partir de `backend`:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"
```

Ela cria 2 categorias, 2 marcas, 3 produtos e 6 variantes com IDs estáveis. Ainda não há preços ou fornecedores nesta etapa. A API com perfil padrão aplica somente a estrutura. Desativar o perfil `demo` não apaga dados já carregados; mantenha um banco exclusivo para demonstração. O script pode ser reaplicado sem duplicar registros e não sobrescreve alterações em registros existentes.

## Encerrar

Use `Ctrl+C` nos terminais das aplicações. Na raiz:

```powershell
docker compose stop
```

## Próxima entrega

Implementar busca pública, filtros, paginação e comparação de preços, conforme etapa 5 do planejamento. Publicação em servidor ainda depende da definição do destino e dos acessos.

## Busca e comparação pública

A API oferece busca por variante, filtros e comparação de preços sem login. Contratos e exemplos em [docs/BUSCA_E_COMPARACAO.md](docs/BUSCA_E_COMPARACAO.md). A interface já está conectada à API; consulte [docs/INTERFACE_PUBLICA.md](docs/INTERFACE_PUBLICA.md).
