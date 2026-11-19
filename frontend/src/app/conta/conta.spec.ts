import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { routes } from '../app.routes';
import { apiInterceptor } from './api.interceptor';
import { AutenticacaoService } from './autenticacao.service';
import { SessaoStore, UsuarioAtual } from './sessao.store';
import { ContaFormComponent } from './conta-form.component';
import { destinoInterno } from './conta.guard';
import { SalvarButtonComponent } from './salvar-button.component';
import { ContatoComponent } from './contato.component';
import { MinhaListaComponent } from './minha-lista.component';

const ID = '40000000-0000-0000-0000-000000000001';
const usuario: UsuarioAtual = {
  id: 'usuario-a',
  nome: 'Pessoa teste',
  email: 'pessoa@teste.local',
  perfil: 'COMPRADOR',
  ativo: true,
  criadoEm: '2026-11-18T10:00:00Z',
};
let http: HttpTestingController;
function configurar(): void {
  TestBed.configureTestingModule({
    providers: [
      provideRouter(routes),
      provideHttpClient(withInterceptors([apiInterceptor])),
      provideHttpClientTesting(),
    ],
  });
  http = TestBed.inject(HttpTestingController);
}
function csrf(token = 'token-teste'): void {
  http.expectOne('/api/auth/csrf').flush({ cabecalho: 'X-CSRF-TOKEN', token });
}

describe('Sessão e CSRF', () => {
  beforeEach(configurar);
  afterEach(() => http.verify({ ignoreCancelled: true }));

  it('restaura conta ao abrir a aplicação e compartilha consulta simultânea', () => {
    const auth = TestBed.inject(AutenticacaoService);
    let nome = '';
    auth.carregar().subscribe((u) => (nome = u!.nome));
    auth.carregar().subscribe();
    http.expectOne('/api/auth/eu').flush(usuario);
    expect(nome).toBe(usuario.nome);
    expect(auth.sessao.usuario()).toEqual(usuario);
    auth.carregar().subscribe();
    http.expectNone('/api/auth/eu');
  });
  it('trata 401 inicial como visitante e permite repetir consulta após falha de rede', () => {
    const auth = TestBed.inject(AutenticacaoService);
    auth.carregar().subscribe({ error: () => {} });
    http.expectOne('/api/auth/eu').flush({}, { status: 503, statusText: 'Error' });
    expect(auth.sessao.estado()).toBe('erro');
    auth.carregar().subscribe();
    http.expectOne('/api/auth/eu').flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(auth.sessao.usuario()).toBeNull();
    expect(auth.sessao.estado()).toBe('pronta');
  });
  it('obtém CSRF antes do login e renova para logout sem guardar credenciais localmente', () => {
    const auth = TestBed.inject(AutenticacaoService);
    const storage = vi.spyOn(Storage.prototype, 'setItem');
    auth.entrar(usuario.email, 'Senha-teste-123').subscribe();
    http.expectNone('/api/auth/login');
    csrf('primeiro');
    const login = http.expectOne('/api/auth/login');
    expect(login.request.headers.get('X-CSRF-TOKEN')).toBe('primeiro');
    login.flush(usuario);
    expect(auth.sessao.usuario()).toEqual(usuario);
    auth.sair().subscribe();
    csrf('segundo');
    const logout = http.expectOne('/api/auth/logout');
    expect(logout.request.headers.get('X-CSRF-TOKEN')).toBe('segundo');
    logout.flush(null);
    expect(auth.sessao.usuario()).toBeNull();
    expect(storage).not.toHaveBeenCalled();
    storage.mockRestore();
  });
  it('não repete uma alteração recusada pelo CSRF', () => {
    TestBed.inject(HttpClient)
      .put('/api/conta/favoritos/produtos/' + ID, {})
      .subscribe({ error: () => {} });
    csrf();
    http
      .expectOne('/api/conta/favoritos/produtos/' + ID)
      .flush({}, { status: 403, statusText: 'Forbidden' });
    http.expectNone('/api/auth/csrf');
  });
  it('limpa sessão expirada em endpoint privado, preservando falha de login como erro de credenciais', () => {
    const auth = TestBed.inject(AutenticacaoService);
    auth.sessao.resolver(usuario);
    auth.entrar('outro@teste.local', 'invalida').subscribe({ error: () => {} });
    csrf();
    http.expectOne('/api/auth/login').flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(auth.sessao.usuario()).toEqual(usuario);
    TestBed.inject(HttpClient)
      .get('/api/conta/favoritos/produtos')
      .subscribe({ error: () => {} });
    http
      .expectOne('/api/conta/favoritos/produtos')
      .flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(auth.sessao.usuario()).toBeNull();
    expect(auth.sessao.aviso()).toContain('expirou');
  });
  it('ignora resposta 401 antiga recebida depois de um novo login', () => {
    const auth = TestBed.inject(AutenticacaoService);
    auth.carregar().subscribe();
    const antiga = http.expectOne('/api/auth/eu');
    auth.entrar(usuario.email, 'Senha-teste-123').subscribe();
    csrf();
    http.expectOne('/api/auth/login').flush(usuario);
    antiga.flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(auth.sessao.usuario()).toEqual(usuario);
  });
  it('não envia CSRF para destinos externos', () => {
    TestBed.inject(HttpClient).put('https://example.test/api/teste', {}).subscribe();
    const request = http.expectOne('https://example.test/api/teste');
    expect(request.request.headers.has('X-CSRF-TOKEN')).toBe(false);
    http.expectNone('/api/auth/csrf');
    request.flush({});
  });
  it.each([
    'https://example.test',
    '//example.test',
    '/entrar',
    '/cadastro?retorno=x',
    '/\\example.test',
  ])('recusa destino de retorno inadequado: %s', (destino) => {
    expect(destinoInterno(destino)).toBe('/');
  });
});

describe('Formulários da conta', () => {
  beforeEach(configurar);
  afterEach(() => http.verify({ ignoreCancelled: true }));
  it('protege a lista privada e preserva o retorno para visitante', async () => {
    TestBed.inject(SessaoStore).resolver(null);
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/minha-lista?tipo=fornecedores', ContaFormComponent);
    expect(TestBed.inject(Router).url).toContain('/entrar?retorno=');
    expect(harness.routeNativeElement!.textContent).toContain('Bem-vindo de volta');
  });
  it('valida cadastro e limite UTF-8 antes de enviar', async () => {
    const harness = await RouterTestingHarness.create();
    const component = await harness.navigateByUrl('/cadastro', ContaFormComponent);
    component.form.setValue({ nome: 'Pessoa', email: 'pessoa@teste.local', senha: 'á'.repeat(40) });
    component.enviar();
    harness.detectChanges();
    expect(component.form.invalid).toBe(true);
    http.expectNone('/api/auth/csrf');
    expect(harness.routeNativeElement!.textContent).toContain('A senha precisa atender');
  });
  it('cria comprador e encaminha para login sem autenticar automaticamente', async () => {
    const harness = await RouterTestingHarness.create();
    const component = await harness.navigateByUrl(
      '/cadastro?retorno=%2Fminha-lista',
      ContaFormComponent,
    );
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    component.form.setValue({
      nome: usuario.nome,
      email: usuario.email,
      senha: 'Senha-segura-123',
    });
    component.enviar();
    csrf();
    const request = http.expectOne('/api/auth/cadastro');
    expect(request.request.body.perfil).toBeUndefined();
    request.flush(usuario);
    expect(navegar).toHaveBeenCalledWith(['/entrar'], {
      queryParams: { criada: 1, retorno: '/minha-lista' },
    });
    expect(TestBed.inject(SessaoStore).usuario()).toBeNull();
    expect(component.form.controls.senha.value).toBe('');
  });
  it('mostra erro genérico de login, evita envio duplicado e permite nova tentativa', async () => {
    const harness = await RouterTestingHarness.create();
    const component = await harness.navigateByUrl('/entrar', ContaFormComponent);
    component.form.patchValue({ email: usuario.email, senha: 'Senha-invalida' });
    component.enviar();
    component.enviar();
    csrf();
    http.expectOne('/api/auth/login').flush({}, { status: 401, statusText: 'Unauthorized' });
    harness.detectChanges();
    expect(harness.routeNativeElement!.textContent).toContain('Email ou senha inválidos');
    expect(component.ocupado()).toBe(false);
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    component.enviar();
    csrf('novo');
    http.expectOne('/api/auth/login').flush(usuario);
    expect(navegar).toHaveBeenCalledWith('/');
  });
});

describe('Favoritos, seguidos e contato', () => {
  beforeEach(configurar);
  afterEach(() => http.verify({ ignoreCancelled: true }));
  async function botao(logado = true) {
    TestBed.inject(SessaoStore).resolver(logado ? usuario : null);
    const fixture = TestBed.createComponent(SalvarButtonComponent);
    fixture.componentRef.setInput('id', ID);
    fixture.componentRef.setInput('tipo', 'produtos');
    fixture.detectChanges();
    await fixture.whenStable();
    return fixture;
  }
  it('encaminha visitante ao login sem tentar salvar', async () => {
    const fixture = await botao(false);
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture.nativeElement.querySelector('button').click();
    expect(navegar).toHaveBeenCalledWith(['/entrar'], { queryParams: { retorno: '/' } });
    http.expectNone('/api/auth/csrf');
  });
  it('salva e remove com CSRF e evita envio duplicado', async () => {
    const fixture = await botao();
    http.expectOne('/api/conta/favoritos/produtos/' + ID).flush({ salvo: false });
    fixture.detectChanges();
    fixture.componentInstance.alterar();
    fixture.componentInstance.alterar();
    csrf();
    const salvar = http.expectOne('/api/conta/favoritos/produtos/' + ID);
    expect(salvar.request.method).toBe('PUT');
    salvar.flush(null);
    fixture.detectChanges();
    expect(fixture.componentInstance.salvo()).toBe(true);
    fixture.componentInstance.alterar();
    csrf();
    const remover = http.expectOne('/api/conta/favoritos/produtos/' + ID);
    expect(remover.request.method).toBe('DELETE');
    remover.flush(null);
    expect(fixture.componentInstance.salvo()).toBe(false);
  });
  it('sincroniza botões do mesmo produto sem resposta antiga sobrescrever o favorito', async () => {
    const primeiro = await botao();
    const consultaA = http.expectOne('/api/conta/favoritos/produtos/' + ID);
    const segundo = TestBed.createComponent(SalvarButtonComponent);
    segundo.componentRef.setInput('id', ID);
    segundo.componentRef.setInput('tipo', 'produtos');
    segundo.detectChanges();
    await segundo.whenStable();
    const consultaB = http.expectOne('/api/conta/favoritos/produtos/' + ID);
    consultaA.flush({ salvo: false });
    primeiro.componentInstance.alterar();
    csrf();
    http.expectOne('/api/conta/favoritos/produtos/' + ID).flush(null);
    consultaB.flush({ salvo: false });
    expect(primeiro.componentInstance.salvo()).toBe(true);
    expect(segundo.componentInstance.salvo()).toBe(true);
  });
  it('preserva o estado anterior ao falhar e permite nova tentativa', async () => {
    const fixture = await botao();
    http.expectOne('/api/conta/favoritos/produtos/' + ID).flush({ salvo: true });
    fixture.componentInstance.alterar();
    csrf();
    http
      .expectOne('/api/conta/favoritos/produtos/' + ID)
      .flush({}, { status: 500, statusText: 'Error' });
    expect(fixture.componentInstance.salvo()).toBe(true);
    expect(fixture.componentInstance.erro()).toContain('Não foi possível alterar');
  });
  it('consulta novamente quando a leitura da marcação falha, sem mutação acidental', async () => {
    const fixture = await botao();
    http
      .expectOne('/api/conta/favoritos/produtos/' + ID)
      .flush({}, { status: 500, statusText: 'Error' });
    fixture.componentInstance.alterar();
    http.expectOne('/api/conta/favoritos/produtos/' + ID).flush({ salvo: true });
    expect(fixture.componentInstance.salvo()).toBe(true);
    http.expectNone('/api/auth/csrf');
  });
  it('libera link WhatsApp apenas após consulta, com nova aba e proteção do opener', () => {
    const fixture = TestBed.createComponent(ContatoComponent);
    fixture.componentRef.setInput('ofertaId', ID);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('a')).toBeNull();
    fixture.componentInstance.consultar();
    http
      .expectOne('/api/catalogo/ofertas/' + ID + '/contato')
      .flush({ url: 'https://wa.me/5511999999999?text=Ol%C3%A1' });
    fixture.detectChanges();
    const link = fixture.nativeElement.querySelector('a');
    expect(link.target).toBe('_blank');
    expect(link.rel).toContain('noopener');
    expect(link.href).toContain('https://wa.me/');
  });
  it('recusa link de contato inesperado e explica oferta indisponível', () => {
    const fixture = TestBed.createComponent(ContatoComponent);
    fixture.componentRef.setInput('ofertaId', ID);
    fixture.detectChanges();
    fixture.componentInstance.consultar();
    http
      .expectOne('/api/catalogo/ofertas/' + ID + '/contato')
      .flush({ url: 'https://example.test' });
    expect(fixture.componentInstance.url()).toBe('');
    fixture.componentInstance.consultar();
    http
      .expectOne('/api/catalogo/ofertas/' + ID + '/contato')
      .flush({}, { status: 404, statusText: 'Not Found' });
    expect(fixture.componentInstance.erro()).toContain('não está mais disponível');
  });
  it('lista favoritos indisponíveis e recarrega após remoção', async () => {
    TestBed.inject(SessaoStore).resolver(usuario);
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/minha-lista', MinhaListaComponent);
    http
      .expectOne((r) => r.url === '/api/conta/favoritos/produtos')
      .flush({
        itens: [
          {
            produtoId: ID,
            nome: 'Produto salvo',
            modelo: 'Modelo',
            marcaId: ID,
            marca: 'Marca',
            disponivel: false,
          },
        ],
        pagina: 0,
        tamanho: 12,
        totalElementos: 1,
        totalPaginas: 1,
      });
    harness.detectChanges();
    await harness.fixture.whenStable();
    http.expectOne('/api/conta/favoritos/produtos/' + ID).flush({ salvo: true });
    harness.detectChanges();
    expect(harness.routeNativeElement!.textContent).toContain('Sem ofertas disponíveis');
    harness
      .routeNativeElement!.querySelector<HTMLButtonElement>('app-salvar-button button')!
      .click();
    csrf();
    http.expectOne('/api/conta/favoritos/produtos/' + ID).flush(null);
    http
      .expectOne((r) => r.url === '/api/conta/favoritos/produtos')
      .flush({ itens: [], pagina: 0, tamanho: 12, totalElementos: 0, totalPaginas: 0 });
    harness.detectChanges();
    expect(harness.routeNativeElement!.textContent).toContain('Nenhum produto nesta página');
  });
  it('retira a lista privada da tela e pede login quando a sessão expira', async () => {
    TestBed.inject(SessaoStore).resolver(usuario);
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/minha-lista', MinhaListaComponent);
    http
      .expectOne((r) => r.url === '/api/conta/favoritos/produtos')
      .flush({}, { status: 401, statusText: 'Unauthorized' });
    await harness.fixture.whenStable();
    harness.detectChanges();
    expect(TestBed.inject(Router).url).toContain('/entrar?retorno=');
    expect(TestBed.inject(SessaoStore).usuario()).toBeNull();
  });

  it('carrega lista de fornecedores com disponibilidade e acesso às ofertas', async () => {
    TestBed.inject(SessaoStore).resolver(usuario);
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/minha-lista?tipo=fornecedores', MinhaListaComponent);
    http
      .expectOne((r) => r.url === '/api/conta/fornecedores-seguidos')
      .flush({
        itens: [
          {
            fornecedorId: ID,
            nome: 'Empresa seguida',
            cidade: 'São Paulo',
            estado: 'SP',
            disponivel: true,
          },
        ],
        pagina: 0,
        tamanho: 12,
        totalElementos: 1,
        totalPaginas: 1,
      });
    harness.detectChanges();
    await harness.fixture.whenStable();
    http.expectOne('/api/conta/fornecedores-seguidos/' + ID).flush({ salvo: true });
    harness.detectChanges();
    expect(harness.routeNativeElement!.textContent).toContain('Empresa seguida');
    expect(harness.routeNativeElement!.querySelector<HTMLAnchorElement>('.item a')!.href).toContain(
      'fornecedorId=',
    );
  });
  it('volta à busca ao sair voluntariamente da lista privada', async () => {
    TestBed.inject(SessaoStore).resolver(usuario);
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/minha-lista', MinhaListaComponent);
    http
      .expectOne((r) => r.url === '/api/conta/favoritos/produtos')
      .flush({ itens: [], pagina: 0, tamanho: 12, totalElementos: 0, totalPaginas: 0 });
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    TestBed.inject(SessaoStore).resolver(null);
    harness.detectChanges();
    await harness.fixture.whenStable();
    expect(navegar).toHaveBeenCalledWith(['/'], { queryParams: {} });
  });
});
