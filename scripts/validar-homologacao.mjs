import assert from 'node:assert/strict';
import { randomUUID } from 'node:crypto';

const base = process.env.SMOKE_BASE_URL ?? 'http://localhost:8088';
if (process.env.SMOKE_PERMITIR_GRAVACAO !== 'SIM' || !process.env.SMOKE_ADMIN_EMAIL || !process.env.SMOKE_ADMIN_SENHA) {
  throw new Error('Use somente banco de homologação. Defina SMOKE_PERMITIR_GRAVACAO=SIM, SMOKE_ADMIN_EMAIL e SMOKE_ADMIN_SENHA. O roteiro cria dados e não os apaga.');
}
class Cliente {
  cookies = new Map();
  async enviar(path, method = 'GET', body = null) {
    const headers = {};
    if (!['GET', 'HEAD'].includes(method)) {
      const csrf = await this.json('/api/auth/csrf'); headers[csrf.cabecalho] = csrf.token;
    }
    if (this.cookies.size) headers.Cookie = [...this.cookies].map(([k,v]) => `${k}=${v}`).join('; ');
    if (body !== null && !(body instanceof FormData)) { headers['Content-Type'] = 'application/json'; body = JSON.stringify(body); }
    const r = await fetch(new URL(path, base), { method, headers, body, redirect: 'manual', signal: AbortSignal.timeout(20000) });
    for (const cookie of r.headers.getSetCookie()) { const parte = cookie.split(';')[0], pos = parte.indexOf('='); this.cookies.set(parte.slice(0,pos), parte.slice(pos+1)); }
    return r;
  }
  async json(path, method = 'GET', body = null, status = 200) {
    const r = await this.enviar(path, method, body); const texto = await r.text();
    assert.equal(r.status, status, `${method} ${path}: ${r.status} ${texto.slice(0,300)}`);
    return texto ? JSON.parse(texto) : null;
  }
  async login(email, senha) { return this.json('/api/auth/login', 'POST', {email, senha}); }
}
const admin = new Cliente(), fornecedor = new Cliente(), comprador = new Cliente();
await admin.login(process.env.SMOKE_ADMIN_EMAIL, process.env.SMOKE_ADMIN_SENHA);
const sufixo = randomUUID().slice(0,8), senha = `Homologacao-${randomUUID()}!`;
const emailFornecedor = `fornecedor-${sufixo}@homologacao.test`, emailComprador = `comprador-${sufixo}@homologacao.test`;
await fornecedor.json('/api/auth/cadastro', 'POST', {nome:'Fornecedor homologação',email:emailFornecedor,senha},201);
await fornecedor.login(emailFornecedor, senha);
const empresa = await fornecedor.json('/api/fornecedores', 'POST', {razaoSocial:`Homologação ${sufixo} Ltda`,nomeFantasia:`Homologação ${sufixo}`,descricao:'Empresa isolada para testes de entrega',whatsapp:'5500000000001',cidade:'São Paulo',estado:'SP'},201);
assert.equal(empresa.situacao,'PENDENTE'); assert.equal((await fornecedor.enviar('/api/auth/eu')).status,401);
await fornecedor.login(emailFornecedor,senha);
await admin.json(`/api/admin/fornecedores/${empresa.id}/situacao`,'PATCH',{situacao:'APROVADO',motivo:null,versao:empresa.versao});
const categoria = await admin.json('/api/admin/catalogo/categorias','POST',{nome:`Categoria ${sufixo}`,exigeArmazenamento:true,exigeCor:true,atributosObrigatorios:['ram_gb'],versao:0},201);
const marca = await admin.json('/api/admin/catalogo/marcas','POST',{nome:`Marca ${sufixo}`,versao:0},201);
const produto = await admin.json('/api/admin/catalogo/produtos','POST',{nome:`Telefone ${sufixo}`,modelo:`Modelo ${sufixo}`,categoriaId:categoria.id,marcaId:marca.id,versao:0},201);
const variante = await admin.json('/api/admin/catalogo/variantes','POST',{produtoId:produto.id,armazenamentoGb:256,cor:'preto',condicao:'NOVO',atributos:{ram_gb:'8'},versao:0},201);
let oferta = await fornecedor.json('/api/fornecedor/ofertas','POST',{varianteId:variante.id,preco:1200.50,estoque:5,disponivel:true},201);
assert.equal(oferta.moderacao,'PENDENTE');
await admin.json(`/api/admin/ofertas/${oferta.id}/moderacao`,'PATCH',{moderacao:'APROVADA',motivo:null,versao:oferta.versao});
let comparacao = await comprador.json(`/api/catalogo/variantes/${variante.id}/ofertas`);
assert.equal(comparacao.menorPreco,1200.5); assert.equal(comparacao.ofertas.totalElementos,1);
await comprador.json('/api/auth/cadastro','POST',{nome:'Comprador homologação',email:emailComprador,senha},201);
await comprador.login(emailComprador,senha);
await comprador.json(`/api/conta/favoritos/produtos/${produto.id}`,'PUT',null,204);
await comprador.json(`/api/conta/fornecedores-seguidos/${empresa.id}`,'PUT',null,204);
assert.equal((await comprador.json('/api/conta/favoritos/produtos')).totalElementos,1);
assert.equal((await comprador.json('/api/conta/fornecedores-seguidos')).totalElementos,1);
const contato = await comprador.json(`/api/catalogo/ofertas/${oferta.id}/contato`);
assert.match(JSON.stringify(contato),/wa\.me\/5500000000001/);
const form = new FormData(); form.append('arquivo',new Blob([`varianteId;preco;estoque;disponivel\n${variante.id};1199,90;4;true\n`],{type:'text/csv'}),'ofertas.csv');
const previa = await fornecedor.json('/api/fornecedor/importacoes','POST',form); assert.equal(previa.atualizar,1); assert.equal(previa.invalidas,0);
assert.equal((await comprador.json(`/api/catalogo/variantes/${variante.id}/ofertas`)).menorPreco,1200.5);
await fornecedor.json(`/api/fornecedor/importacoes/${previa.id}/confirmacao`,'POST');
await fornecedor.json(`/api/fornecedor/importacoes/${previa.id}/confirmacao`,'POST');
assert.equal((await comprador.json(`/api/catalogo/variantes/${variante.id}/ofertas`)).menorPreco,1199.9);
const solicitacao = await fornecedor.json('/api/fornecedor/catalogo/solicitacoes','POST',{nomeProduto:produto.nome,marca:marca.nome,modelo:produto.modelo,categoria:categoria.nome,caracteristicas:'256 GB preto novo RAM 8 GB'},201);
await admin.json(`/api/admin/solicitacoes/${solicitacao.id}/resolucao`,'PUT',{situacao:'ATENDIDA',varianteId:variante.id,motivo:'Variante correspondente validada na homologação.',versao:0},204);
assert.equal((await fornecedor.json('/api/fornecedor/catalogo/solicitacoes')).itens[0].situacao,'ATENDIDA');
const atual = await admin.json('/api/admin/fornecedores'); const empresaAtual = atual.itens.find(e=>e.id===empresa.id);
await admin.json(`/api/admin/fornecedores/${empresa.id}/situacao`,'PATCH',{situacao:'BLOQUEADO',motivo:'Bloqueio de teste para conferir retirada pública.',versao:empresaAtual.versao});
const indisponivel = await comprador.enviar(`/api/catalogo/variantes/${variante.id}/ofertas`);assert.ok([200,404].includes(indisponivel.status));
if (indisponivel.status===200) assert.equal((await indisponivel.json()).ofertas.totalElementos,0);
assert.equal((await comprador.enviar(`/api/catalogo/ofertas/${oferta.id}/contato`)).status,404);
const bloqueada = await admin.json('/api/admin/fornecedores');
const registroBloqueado = bloqueada.itens.find(e=>e.id===empresa.id);
const pendente = await admin.json(`/api/admin/fornecedores/${empresa.id}/situacao`,'PATCH',{situacao:'PENDENTE',motivo:null,versao:registroBloqueado.versao});
await admin.json(`/api/admin/fornecedores/${empresa.id}/situacao`,'PATCH',{situacao:'APROVADO',motivo:null,versao:pendente.versao});
assert.equal((await comprador.json(`/api/catalogo/variantes/${variante.id}/ofertas`)).menorPreco,1199.9);
await comprador.json('/api/auth/logout','POST',null,204); assert.equal((await comprador.enviar('/api/conta/favoritos/produtos')).status,401);
await fornecedor.json('/api/auth/logout','POST',null,204); await admin.json('/api/auth/logout','POST',null,204);
console.log('Homologação integrada aprovada: cadastro/relogin, aprovação, catálogo/variante, oferta/moderação, comparação, favoritos/seguidos, contato, importação/prévia/confirmação idempotente, solicitação/resposta, bloqueio/elegibilidade e logout. Dados criados somente no banco informado.');
