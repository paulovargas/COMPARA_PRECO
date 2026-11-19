# Dados de demonstração

Carga exclusiva para desenvolvimento, habilitada pelo perfil `demo`. Produtos, empresas, preços e contatos são fictícios. Nenhuma conta administrativa é criada.

## Acessar o sistema

Abra http://127.0.0.1:4200. A busca pode ser usada sem login. Todas as contas abaixo são criadas inicialmente com a senha **`ComparaDemo2026!`**:

| Email | Perfil e exemplo |
| --- | --- |
| `comprador@demo.comparapreco.test` | Comprador, com 2 favoritos e 2 fornecedores seguidos |
| `paulista@demo.comparapreco.test` | Fornecedor aprovado em São Paulo/SP, com 6 ofertas e uma solicitação de catálogo |
| `rio@demo.comparapreco.test` | Fornecedor aprovado em Niterói/RJ, com ofertas publicadas, uma sem estoque e outra indisponível |
| `minas@demo.comparapreco.test` | Fornecedor aprovado em Belo Horizonte/MG, com ofertas publicadas, pendente e bloqueada |
| `pendente@demo.comparapreco.test` | Empresa pendente, painel em consulta e perfil editável |
| `bloqueado@demo.comparapreco.test` | Empresa bloqueada, com motivo e painel somente em consulta |

A senha está armazenada com BCrypt fator 12. Essas credenciais pertencem apenas aos exemplos locais, não devem ser utilizadas em produção. Os telefones usam números fictícios com DDD 00, para não representar pessoas ou empresas reais; o link WhatsApp demonstra a montagem do contato, sem destinatário real.

## O que testar

1. **Busca e comparação:** encontre o iPhone 16 Pro Max de 256 GB, titânio natural, novo. Há 3 ofertas elegíveis; duas empatam em R$ 6.200,00 e recebem o destaque de menor preço.
2. **Variantes:** compare separadamente 512 GB, cor preta e condição usada. Preços de características diferentes não são misturados.
3. **Filtros:** teste SP, RJ e MG, além de marca, categoria, armazenamento, cor e condição.
4. **Outros produtos:** Galaxy S24 e MacBook Air M3 têm ofertas de diferentes fornecedores.
5. **Conta:** entre como comprador e consulte “Minha lista”, remova/adicione favoritos e siga fornecedores.
6. **Fornecedor:** entre como Paulista, use “Painel do fornecedor” e altere preço/estoque/disponibilidade. A busca reflete a alteração; moderação continua preservada. Ofertas novas aguardam análise.
7. **Estados privados:** Rio demonstra falta de estoque/indisponibilidade; Minas demonstra moderação pendente/bloqueada; as duas últimas contas demonstram pendência/bloqueio de empresa.
8. **Solicitação:** Paulista possui uma solicitação pendente de Galaxy S25. Um administrador pode analisar e responder pelo painel administrativo, vinculando uma variante completa ou recusando com motivo.

## Conteúdo inicial

- 2 categorias, 2 marcas, 3 produtos e 6 variantes.
- 6 contas e 5 empresas (3 aprovadas, 1 pendente, 1 bloqueada).
- 20 ofertas, sendo 14 elegíveis e 6 excluídas da busca por estoque, disponibilidade, moderação ou situação da empresa.
- 2 produtos favoritos, 2 fornecedores seguidos e 1 solicitação pendente.

## Carregar ou reutilizar

Inicie o Docker Desktop e o banco manualmente, conforme README. No diretório `backend`, com o JDK 25 selecionado:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"
```

Não execute uma segunda API na porta 8080; encerre a instância anterior antes de iniciar com o perfil. O frontend continua com `npm.cmd start -- --host 127.0.0.1`.

Os scripts `db/demo/R__catalogo_demo.sql` e `db/demo/R__ofertas_e_contas_demo.sql` usam IDs estáveis e não sobrescrevem registros existentes. Flyway aplica scripts repetíveis na primeira execução e quando seu conteúdo muda; reiniciar com o mesmo conteúdo não restaura edições ou registros excluídos. Alterações feitas durante a exploração são preservadas.

O perfil padrão não executa a carga. Desativar `demo` não remove dados já inseridos. Use somente banco dedicado de desenvolvimento/demonstração. Caso IDs/emails/nomes exclusivos já estejam ocupados por outros registros, a carga pode ser recusada pelas restrições, sem substituir esses registros. Não apague ou manipule manualmente o histórico do Flyway para forçar reaplicação.

Os testes da carga usam PostgreSQL descartável e verificam reaplicação sem duplicidade, preservação de edições, credenciais, empates e exclusão de ofertas inelegíveis.
