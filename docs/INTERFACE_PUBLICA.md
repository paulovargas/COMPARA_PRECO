# Interface pública

## Executar

Inicie o Docker Desktop e o banco manualmente, a partir da raiz:

```powershell
& 'C:\Program Files\Docker\Docker\resources\bin\docker-compose.exe' up -d
```

No diretório `backend`, com o JDK 25 selecionado conforme README:

```powershell
.\mvnw.cmd spring-boot:run
```

Em outro terminal, no diretório `frontend`:

```powershell
npm.cmd start -- --host 127.0.0.1
```

Abra http://127.0.0.1:4200. O proxy de desenvolvimento encaminha `/api` à API em localhost:8080. As consultas públicas dispensam login. Para hospedagem futura, configurar o servidor para encaminhar `/api` ao backend e servir `index.html` nas rotas Angular.

## Fluxo

- `/`: busca por nome, marca ou modelo; filtros em “Filtrar resultados”; resultados ordenados pelo menor preço.
- Filtros de categoria, marca, modelo completo, armazenamento, cor, condição, estado e cidade completa. O contador indica quantos filtros estão aplicados. Filtros recolhíveis mantêm a tela utilizável no celular.
- Cada cartão identifica a variante com armazenamento, cor, condição e todos os atributos. Preço inicial e quantidade correspondem aos filtros aplicados.
- “Comparar ofertas” abre `/comparar/{id}` preservando os filtros. Empresas, região, estoque, atualização e preços ficam visíveis. Todas as ofertas empatadas no mínimo recebem o destaque “Menor preço”.
- “Ver ofertas deste fornecedor” restringe a comparação ao fornecedor selecionado; “Ver todos os fornecedores” remove essa restrição. Ao voltar aos resultados, a busca também mantém esse filtro.
- “Voltar aos resultados” recupera a página da busca original. Filtros e paginação ficam na URL, permitindo recarregar, compartilhar e navegar com voltar/avançar. Alterar filtros começa na página zero.
- Falhas de conexão permitem nova tentativa. Parâmetros inválidos, comparação indisponível, ausência de ofertas, falha nas opções e imagens quebradas têm tratamento próprio. Requisições antigas são canceladas ao navegar.

Os valores usam BRL e os horários, o fuso do dispositivo. As ofertas não incluem frete ou negociação. Conta, favoritos, fornecedores seguidos e WhatsApp estão disponíveis conforme [CONTA_E_CONTATO.md](CONTA_E_CONTATO.md).

## Dados e verificação

O perfil `demo` do backend carrega somente o catálogo, sem fornecedores/ofertas aprovados. Assim, ele também pode apresentar busca vazia. Não foram inseridas ofertas fictícias no banco de desenvolvimento para a interface.

No diretório `frontend`:

```powershell
npm.cmd run format:check
npm.cmd test -- --watch=false
npm.cmd run build
```

Os testes de interface usam respostas HTTP controladas para verificar resultados, filtros, paginação, links, recuperação por URL, cancelamento de buscas antigas, empates e estados de erro/vazio. O pipeline executa esses testes antes do build. A elegibilidade e os cálculos da API já são cobertos por testes com PostgreSQL descartável.

A revisão visual foi feita no Edge com viewport desktop de 1440 × 1000 e celular de 390 × 844: busca vazia contra a API local e cenários preenchidos por interceptação de HTTP no navegador. Os dados simulados não são persistidos e não fazem parte da aplicação distribuída.
