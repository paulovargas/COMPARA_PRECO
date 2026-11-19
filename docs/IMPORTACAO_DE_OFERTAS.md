# Importação de ofertas

Entre com uma conta de fornecedor aprovado e abra **Painel do fornecedor → Importar ofertas** em <http://127.0.0.1:4200/fornecedor/importacao>.

1. Baixe o modelo CSV ou XLSX na tela.
2. No catálogo do fornecedor, copie o **Código para importação** da variante com as características exatas do produto vendido.
3. Preencha uma oferta por linha e gere a prévia. Nenhuma oferta é gravada nesta etapa.
4. Confira produtos, características, ações, preços, estoque e erros. Corrija o arquivo e envie novamente se necessário.
5. Clique em **Confirmar importação** para aplicar o lote inteiro. Consulte o resultado em **Ofertas**.

## Formato

CSV UTF-8, com ou sem BOM, separado por ponto e vírgula. Cada oferta ocupa uma linha, sem campos multilinha. XLSX deve conter uma única planilha, sem fórmulas, células com erro, macros ou vínculos externos. XLS antigo e outros formatos são recusados. Tamanho máximo: 2 MB, até 500 ofertas, quatro colunas e 256 caracteres por campo. Estrutura XLSX descompactada limitada a 16 MB e 1.000 entradas, além das proteções padrão do Apache POI.

Cabeçalho obrigatório e na mesma ordem:

```csv
varianteId;preco;estoque;disponivel
```

| Coluna | Regra |
| --- | --- |
| varianteId | UUID completo copiado do catálogo. A variante precisa existir e estar completa. |
| preco | Maior que zero e até 9999999999,99. Até duas casas decimais, vírgula ou ponto, sem separador de milhares ou notação científica. |
| estoque | Inteiro entre 0 e 1000000000. |
| disponivel | Texto `true` ou `false`; no XLSX também aceita célula booleana. |

XLSX aceita células numéricas para preço e estoque; coloque códigos UUID como texto. Para controlar a precisão decimal, use preço como texto. A prévia mostra o valor efetivamente lido; nunca avalia fórmulas. Modelos têm somente o cabeçalho para evitar inserir preços de exemplo por engano.

## Atualizações, duplicatas e conflitos

- A chave é **empresa autenticada + variante**. O arquivo não escolhe outra empresa, proprietário nem situação de moderação.
- **CRIAR**: nova oferta, inicialmente pendente de moderação.
- **ATUALIZAR**: altera preço, estoque e disponibilidade da oferta existente, preservando a moderação atual.
- **IGNORAR**: todos os valores são iguais; preserva versão e data de atualização.
- **INVALIDA**: erros listados por linha. Qualquer erro impede o lote inteiro. Toda ocorrência de variante repetida no arquivo é marcada como inválida.
- Prévia válida por 30 minutos; até 10 pendentes por empresa. O link com `?previa=...` permite reabrir após recarregar, sempre com a mesma conta. Prévias vencidas são removidas quando a empresa envia outro arquivo, incluindo resultados já aplicados após esse prazo; não constituem histórico permanente.
- Confirmação revalida aprovação da empresa, existência/completude das variantes e identidade/versão das ofertas e versão da variante. Criação, remoção, edição ou moderação concorrente da oferta exige uma nova prévia.
- Tudo é aplicado em uma transação. Conflitos não deixam parte do lote gravada. A empresa é bloqueada primeiro, seguindo a ordem do CRUD e da moderação.
- Repetir a confirmação de uma prévia aplicada ainda disponível devolve o mesmo resultado, sem gravar novamente. Em falha de conexão, é possível tentar confirmar a mesma prévia. Não há repetição automática de mutações.
- Reimportar o mesmo arquivo gera outra prévia: valores já iguais são ignorados.

## API

Todos os endpoints exigem perfil FORNECEDOR. Envio e confirmação exigem CSRF de `/api/auth/csrf`; cookies de sessão seguem a autenticação existente.

| Método | Endpoint | Uso |
| --- | --- | --- |
| GET | `/api/fornecedor/importacoes/modelo/csv` | Modelo CSV |
| GET | `/api/fornecedor/importacoes/modelo/xlsx` | Modelo XLSX |
| POST | `/api/fornecedor/importacoes` | Multipart com campo `arquivo`; retorna prévia |
| GET | `/api/fornecedor/importacoes/{id}` | Consulta somente a própria prévia |
| POST | `/api/fornecedor/importacoes/{id}/confirmacao` | Confirmação integral e idempotente |

Arquivo inválido retorna 400; tamanho excedido, 413; ausência de sessão, 401; perfil/bloqueio, 403; prévia de outro proprietário/inexistente, 404; conflito/limite/lote inválido, 409; prévia expirada, 410. Erros de conteúdo por linha são retornados na prévia HTTP 200 e impedem confirmação.

## Validação local

```powershell
Set-Location backend
.\mvnw.cmd -B verify
Set-Location ../frontend
npm.cmd run format:check
npm.cmd test -- --watch=false
npm.cmd run build
```

Testes de integração usam PostgreSQL descartável via Testcontainers. Não execute testes de gravação no banco local de demonstração.

Dependências de leitura: [Apache POI](https://poi.apache.org/download.html) e [Apache Commons CSV](https://commons.apache.org/csv/download_csv.cgi), com versões fixas no `pom.xml`.
