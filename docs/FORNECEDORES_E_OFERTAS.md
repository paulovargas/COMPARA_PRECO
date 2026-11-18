# Fornecedores e ofertas

A API de cadastro da empresa e gerenciamento das ofertas está conectada ao [painel do fornecedor](PAINEL_DO_FORNECEDOR.md). O catálogo compartilhado continua separado das ofertas: cada fornecedor escolhe uma variante existente e cadastra seu preço, estoque e disponibilidade.

## Fluxo inicial

1. Cadastre uma conta de comprador e faça login conforme [autenticação](AUTENTICACAO.md).
2. Obtenha um token CSRF atualizado e faça POST `/api/fornecedores`.
3. A empresa começa PENDENTE. A conta passa a FORNECEDOR e todas as sessões anteriores são invalidadas. Faça novo login e renove o CSRF.
4. Um administrador analisa a empresa em `/api/admin/fornecedores` e altera sua situação para APROVADO.
5. O fornecedor consulta `/api/fornecedor/catalogo/variantes`, escolhe uma variante completa e cria sua oferta.
6. A oferta começa PENDENTE de moderação. O administrador pode aprová-la; sua publicação também depende de estoque, disponibilidade, empresa e conta ativas.

Não é possível escolher o proprietário ou um perfil administrativo pelo JSON. Há uma empresa por conta e uma oferta por combinação de empresa e variante. Contas administrativas usam os endpoints de administração; o cadastro de empresa exige uma conta comum.

## Endpoints

Todos exigem sessão. POST, PUT, PATCH e DELETE exigem CSRF.

| Método | Caminho | Perfil e resultado |
| --- | --- | --- |
| POST | `/api/fornecedores` | COMPRADOR ou FORNECEDOR; 201 para novo cadastro, 409 se já possui empresa; novo login obrigatório após sucesso |
| GET | `/api/fornecedor/perfil` | FORNECEDOR; empresa da conta autenticada |
| PUT | `/api/fornecedor/perfil` | FORNECEDOR; atualiza empresa com versão atual |
| GET | `/api/fornecedor/catalogo/variantes?texto=&pagina=0&tamanho=20` | FORNECEDOR; busca por nome, marca ou modelo, incluindo pendências de características |
| POST | `/api/fornecedor/ofertas` | FORNECEDOR aprovado; cria oferta pendente, retorna 201 |
| GET | `/api/fornecedor/ofertas?pagina=0&tamanho=20` | FORNECEDOR; somente ofertas próprias |
| GET | `/api/fornecedor/ofertas/{id}` | FORNECEDOR; somente oferta própria |
| PUT | `/api/fornecedor/ofertas/{id}` | FORNECEDOR aprovado; altera preço, estoque e disponibilidade |
| DELETE | `/api/fornecedor/ofertas/{id}?versao=0` | FORNECEDOR aprovado; exclui oferta própria com versão atual, retorna 204 |
| GET | `/api/admin/fornecedores?situacao=PENDENTE` | ADMIN; filtro opcional de situação, paginação padrão |
| PATCH | `/api/admin/fornecedores/{id}/situacao` | ADMIN; modera empresa com versão atual |
| GET | `/api/admin/ofertas?moderacao=PENDENTE&fornecedorId=UUID` | ADMIN; filtros opcionais e paginação padrão |
| PATCH | `/api/admin/ofertas/{id}/moderacao` | ADMIN; modera oferta com versão atual |

Paginação: `pagina` começa em zero e `tamanho` vai de 1 a 50. A busca de variantes aceita até 100 caracteres e trata o texto como substring literal, sem interpretar curingas. Recursos de outra empresa retornam 404, assim como IDs inexistentes.

## Empresa

Exemplo de cadastro:

```json
{
  "razaoSocial": "Empresa Exemplo Ltda",
  "nomeFantasia": "Empresa Exemplo",
  "descricao": "Atendimento comercial",
  "whatsapp": "+55 (11) 99999-9999",
  "cidade": "São Paulo",
  "estado": "SP"
}
```

O WhatsApp é normalizado para dígitos, com 10 a 15 posições, incluindo código do país. A validação é de formato; não consulta a existência do número no WhatsApp. UF deve ser uma das siglas brasileiras válidas. Descrição é opcional; razão social, nome fantasia, cidade, UF e WhatsApp são obrigatórios.

A resposta contém ID, usuário proprietário, dados da empresa, situação, motivo de moderação, datas e `versao`. Para editar:

```json
{
  "empresa": {
    "razaoSocial": "Empresa Exemplo Ltda",
    "nomeFantasia": "Empresa Exemplo",
    "descricao": "Novo horário de atendimento",
    "whatsapp": "5511999999999",
    "cidade": "São Paulo",
    "estado": "SP"
  },
  "versao": 0
}
```

Alterações de contato e descrição preservam a aprovação. Mudanças de razão social ou nome fantasia, além de capitalização, fazem uma empresa aprovada voltar a PENDENTE. Suas ofertas deixam de ser elegíveis até nova aprovação. Empresa bloqueada pode consultar perfil e ofertas, mas não editar nem criar/excluir ofertas. Bloquear a empresa não bloqueia automaticamente a conta: ela permanece disponível para consultar a situação, se estiver ativa.

Transições de empresa: PENDENTE → APROVADO ou BLOQUEADO; APROVADO → BLOQUEADO; BLOQUEADO → PENDENTE. Aprovação direta de bloqueado é recusada: deve voltar à análise. Exemplo administrativo:

```json
{"situacao":"BLOQUEADO","motivo":"Cadastro precisa de revisão","versao":1}
```

## Ofertas e concorrência

Exemplo de criação, com ID retornado pelo catálogo:

```json
{"varianteId":"40000000-0000-0000-0000-000000000001","preco":6500.00,"estoque":5,"disponivel":true}
```

O ID acima pertence ao catálogo do perfil `demo`. Use um ID existente no ambiente atual. Para atualizar:

```json
{"preco":6400.00,"estoque":3,"disponivel":true,"versao":0}
```

Preço usa BigDecimal, é positivo, tem no máximo 2 casas decimais e valor máximo de R$ 9.999.999.999,99. O banco também rejeita valores com casas extras, sem arredondá-los silenciosamente. Estoque é inteiro de 0 a 1.000.000.000; disponibilidade é booleana obrigatória. Estoque ou versão fracionários são rejeitados.

A resposta inclui produto e variante, fornecedor, preço, estoque, disponibilidade, moderação, motivo, datas e versão. Edição não troca fornecedor ou variante nem altera moderação. Para outra variante, crie outra oferta. Alterações de preço/estoque preservam o estado da moderação, inclusive BLOQUEADA. Oferta com estoque zero pode ser mantida no painel, mas não é publicada.

Use a versão recebida na leitura para PUT, DELETE e moderação. Dados desatualizados retornam 409 `REGISTRO_DESATUALIZADO`. Atualize a tela antes de tentar novamente. JPA também protege atualizações simultâneas com versão, e as operações de escrita bloqueiam a empresa antes da oferta, coordenando alterações de estado.

Moderação de oferta: PENDENTE → APROVADA ou BLOQUEADA; APROVADA → BLOQUEADA; BLOQUEADA → PENDENTE. Bloqueio exige motivo. Uma oferta só pode ser aprovada com empresa aprovada, conta ativa e variante completa. Exemplo:

```json
{"moderacao":"APROVADA","versao":0}
```

## Catálogo e elegibilidade

Categorias definem se armazenamento, cor e determinadas chaves de atributos são obrigatórios. O catálogo devolve `completa` e `pendencias` para orientar a seleção. Criar ou aprovar uma oferta de variante incompleta retorna 409 `VARIANTE_INCOMPLETA`.

As categorias demonstrativas de smartphones exigem armazenamento, cor e `ram_gb`; notebooks também exigem `tela_polegadas`. As regras podem ser configuradas por categoria; o CRUD administrativo do catálogo será entregue na etapa 10. Esta API do fornecedor consulta o catálogo central, sem alterar produtos compartilhados com outras empresas. O fornecedor pode enviar solicitações de inclusão/revisão pelo painel, com limite de 10 pendentes e histórico próprio. O atendimento administrativo será implementado na etapa 10.

A view `compara_preco.oferta_elegivel` reúne somente ofertas com moderação APROVADA, disponíveis, com estoque positivo, empresa APROVADO, proprietário ativo com perfil FORNECEDOR e variante que ainda atende às exigências da categoria. Bloquear a empresa ou a conta, mudar o perfil ou tornar a variante incompleta retira a oferta desse conjunto imediatamente após a confirmação da alteração, sem reescrever preços ou aprovações anteriores.

A busca e a comparação públicas usam essa mesma fonte para preços e contagens, conforme [BUSCA_E_COMPARACAO.md](BUSCA_E_COMPARACAO.md).

## Validação

`mvnw.cmd -B verify` executa a suíte em PostgreSQL temporário. Os cenários cobrem cadastro com novo login, duplicidades, perfis, aprovação/reanálise, identidade, WhatsApp/UF, isolamento entre empresas, versão desatualizada, exclusão, variantes incompletas, preço/estoque inválidos, moderação preservada e elegibilidade após bloqueios. Restrições de preço e unicidade também são testadas por acesso direto ao banco.
