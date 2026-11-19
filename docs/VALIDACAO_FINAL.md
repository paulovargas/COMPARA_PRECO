# Validação local final — 19/11/2026

Preparação local concluída; publicação externa não realizada. Paulo informou que servidor/domínio ainda não foram definidos e solicitou preparação local.

| Critério | Evidência local |
| --- | --- |
| Catálogo e identidade de variantes | Testes de domínio/persistência, CRUD administrativo e variante explícita no roteiro integrado |
| Busca, filtros, comparação e empates | Testes de API e interface; revisão pública em Edge e distribuição Nginx |
| Conta, sessão, CSRF e perfis | Suíte HTTP real, guardas Angular, cookies Secure/HttpOnly/Lax na distribuição |
| Favoritos, seguidos e contato | Roteiro HTTP contra Docker, incluindo contato indisponível após bloqueio; sem abrir WhatsApp |
| Fornecedor e aprovação | Cadastro com sessão invalidada, novo login e aprovação administrativa no banco isolado |
| Oferta, moderação e bloqueio | Criação pendente, aprovação, comparação, bloqueio retirando publicação e reaprovação |
| CSV/XLSX | Suítes de importação; CSV multipart integrado com prévia, confirmação e repetição idempotente |
| Solicitação de catálogo | Solicitação, resposta com variante completa e consulta no histórico |
| Administração | APIs/guardas, permissões, concorrência, referências em uso, métricas, CSV por UF e configuração |
| Logout e expiração | 94 testes Angular, com saída voluntária corrigida no fornecedor/Minha lista; Edge administrativo |
| Distribuição | API e Nginx sem root, filesystem somente leitura, healthchecks saudáveis, banco/API sem portas públicas |
| Operação | Backup custom, restauração em banco novo, contagens coincidentes e recusa de sobrescrita |

## Comandos executados

- Backend: `mvnw.cmd -B -o verify` — BUILD SUCCESS, 138 testes, zero falhas/erros/ignorados.
- Frontend: `npm.cmd run format`, `npm.cmd test -- --watch=false`, `npm.cmd run build` e `npm.cmd run format:check` — 94 testes, build e formatação aprovados.
- Docker: configuração silenciosa do Compose, builds das imagens e `up -d --wait` no projeto separado `compara-preco-homologacao`. Um erro de snapshot/cache do Docker Desktop foi contornado reconstruindo a imagem da API sem cache; a reconstrução e a subida foram concluídas. Nenhum cache global ou volume existente foi apagado.
- `node scripts/validar-homologacao.mjs`, com liberação explícita de escrita e credenciais por ambiente — percurso integrado aprovado. Apenas o banco de homologação recebeu esses registros.
- `scripts/backup.ps1` e `scripts/restaurar.ps1` executados sobre homologação. Original e restaurado: 5 contas, 2 ofertas e 1 oferta elegível. Tentativa de repetir backup no mesmo destino foi recusada, preservando o arquivo anterior.
- Edge/Playwright temporário, sem dependência nova no frontend: 1440×1000 e 390×844. Busca/comparação reais, recarga de rota profunda, login/recarga/logout administrativo, cookies, headers e bloqueio de endpoints internos aprovados. Capturas inspecionadas; sem overflow horizontal.
- Bootstrap desabilitado após criação do admin da homologação, senha removida do ambiente de execução e API/frontend reiniciados. Nova revisão no navegador aprovou login, persistência da conta e distribuição.

## Separação de ambientes

O banco original preserva 7 contas, 5 empresas, 3 produtos, 6 variantes, 20 ofertas e 14 elegíveis. A homologação usa outro projeto/volume; não aplicou seed demo. Containers mantêm `restart=no`. A distribuição local está em <http://localhost:8088>, com acesso administrativo próprio; desenvolvimento continua em <http://localhost:4200>.

Credenciais, arquivos de ambiente, backups, capturas e logs locais não são versionados. O pipeline foi atualizado para também construir imagens depois das validações, mas não foi executado remotamente; nenhum push, registry, servidor ou DNS foi utilizado.

## Pendências para publicação externa

Servidor e domínio, proxy TLS/certificado/renovação, configuração de origem confiável e limites por IP, credenciais exclusivas, política de backups/retenção externa, monitoramento e verificação após publicar. O [guia de implantação](IMPLANTACAO.md) descreve preparação e recuperação. Não há checkout, pagamentos ou coleta automática em sites externos no escopo.
