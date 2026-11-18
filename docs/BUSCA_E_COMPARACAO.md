# Busca e comparação pública

Consultas GET sem login, sob `/api/catalogo`:

- `/variantes`: página de variantes com `variante`, `imagemUrl`, `menorPreco` e `quantidadeOfertas`.
- `/variantes/{id}/ofertas`: características da variante, menor preço e página de ofertas, ordenadas por preço e UUID. Cada oferta informa empresa, cidade/UF, estoque e atualização; `menorPreco=true` destaca todos os empates, inclusive em páginas diferentes.
- `/categorias` e `/marcas`: opções com `id` e `nome`, somente quando possuem ofertas elegíveis.

## Parâmetros

Busca e comparação aceitam os mesmos filtros:

| Parâmetro | Comportamento |
| --- | --- |
| texto | Trecho literal, sem distinção de maiúsculas, em nome de produto, modelo ou marca; até 100 caracteres |
| categoriaId, marcaId, fornecedorId | UUID exato |
| modelo | Nome completo, ignorando maiúsculas e espaços repetidos |
| armazenamentoGb | Inteiro positivo |
| cor | Valor completo, ignorando maiúsculas e espaços repetidos |
| condicao | NOVO, USADO ou RECONDICIONADO |
| estado | Sigla de UF em maiúsculas |
| cidade | Nome completo, ignorando maiúsculas e espaços repetidos |
| pagina | Índice a partir de 0, padrão 0, máximo 1000000 |
| tamanho | De 1 a 50, padrão 20 |

Filtros combinam com E. Acentos são significativos. `%`, `_` e aspas no texto são literais; os parâmetros nunca são interpolados no SQL. Parâmetros inválidos retornam 400.

Exemplos:

```text
GET /api/catalogo/variantes?texto=iphone&armazenamentoGb=256&condicao=NOVO&estado=SP
GET /api/catalogo/variantes/{id}/ofertas?estado=SP&tamanho=10&pagina=0
```

Mantenha os filtros ao abrir a comparação: menor preço e contagem consideram somente as ofertas que atendem aos filtros atuais. Sem filtros, consideram todas as ofertas elegíveis da variante. Uma variante com outra cor, armazenamento, condição ou atributo não participa da comparação.

A página segue `itens`, `pagina`, `tamanho`, `totalElementos` e `totalPaginas`. Página além do fim retorna itens vazios mantendo o total. Variante completa sem ofertas retorna comparação vazia, total zero e menor preço nulo. Variante inexistente ou incompleta retorna 404.

## Publicação e consistência

As consultas usam a view `oferta_elegivel`: oferta aprovada, disponível e com estoque; empresa aprovada; conta ativa com perfil FORNECEDOR; variante completa conforme requisitos da categoria. Bloqueios e mudanças de requisitos retiram ofertas na consulta seguinte. Nenhum preço ou total inclui ofertas de outra variante.

Contagem, mínimo e página usam o mesmo snapshot PostgreSQL por requisição. Requisições de páginas distintas podem refletir alterações ocorridas entre elas. Valores monetários são decimais; nenhum cálculo usa ponto flutuante. O menor preço exibido não inclui frete ou negociação e não representa checkout.

Não há expiração automática por idade da oferta nesta etapa. A data de atualização é pública; a política de validade permanece uma decisão para produção. WhatsApp, favoritos e fornecedores seguidos serão conectados na etapa 7. A interface de busca está disponível conforme `INTERFACE_PUBLICA.md`.

## Validação

No diretório `backend`, com JDK 25 e Docker Desktop disponíveis:

```powershell
.\mvnw.cmd -B verify
```

Os testes usam PostgreSQL descartável, separado do banco local. Cobrem comparação por variante, filtros combinados e individuais, preço/contagem consistentes, empates entre páginas, ordem estável, validação, conteúdo público e retirada de ofertas inelegíveis.
