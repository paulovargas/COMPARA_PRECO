import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { By } from '@angular/platform-browser';
import { firstValueFrom, Observable } from 'rxjs';
import { registerLocaleData } from '@angular/common';
import localePt from '@angular/common/locales/pt';
import { AppComponent } from '../app.component';
import { routes } from '../app.routes';
import { apiInterceptor } from '../conta/api.interceptor';
import { SessaoStore, UsuarioAtual } from '../conta/sessao.store';
import { Variante } from '../catalogo/catalogo.models';
import { FornecedorContext } from './fornecedor.context';
import { fornecedorGuard } from './fornecedor.guard';
import { FornecedorShellComponent } from './fornecedor-shell.component';
import { EmpresaComponent } from './empresa.component';
import { OfertaEditorComponent } from './oferta-editor.component';
import { OfertasComponent } from './ofertas.component';
import { CatalogoFornecedorComponent } from './catalogo-fornecedor.component';
import { SolicitacoesComponent } from './solicitacoes.component';
import { Empresa, OfertaFornecedor } from './fornecedor.models';
import { estoqueInteiro, mensagemOperacao, precoDecimal } from './validacao-oferta';

registerLocaleData(localePt);
const ID = '40000000-0000-0000-0000-000000000001';
const usuario: UsuarioAtual = {
  id: 'pessoa',
  nome: 'Pessoa teste',
  email: 'pessoa@teste.local',
  perfil: 'FORNECEDOR',
  ativo: true,
  criadoEm: '2026-11-18T10:00:00Z',
};
const empresa: Empresa = {
  id: ID,
  usuarioId: 'pessoa',
  razaoSocial: 'Empresa Teste Ltda',
  nomeFantasia: 'Empresa Teste',
  descricao: 'Atendimento',
  whatsapp: '5511999999999',
  cidade: 'São Paulo',
  estado: 'SP',
  situacao: 'APROVADO',
  motivoModeracao: null,
  versao: 3,
  criadoEm: '2026-11-18T10:00:00Z',
  atualizadoEm: '2026-11-18T10:00:00Z',
};
const variante: Variante = {
  id: ID,
  produtoId: ID,
  nomeProduto: 'Telefone teste',
  modelo: 'Modelo teste',
  marca: 'Marca teste',
  categoria: 'Smartphones',
  armazenamentoGb: 256,
  cor: 'preto',
  condicao: 'NOVO',
  atributos: { ram_gb: '8' },
  completa: true,
  pendencias: [],
};
const oferta: OfertaFornecedor = {
  id: ID,
  fornecedorId: ID,
  fornecedor: 'Empresa Teste',
  variante,
  preco: 6500,
  estoque: 3,
  disponivel: true,
  moderacao: 'APROVADA',
  motivoModeracao: null,
  versao: 4,
  criadoEm: '2026-11-18T10:00:00Z',
  atualizadoEm: '2026-11-18T10:00:00Z',
};
const pagina = <T>(itens: T[]) => ({
  itens,
  pagina: 0,
  tamanho: 12,
  totalElementos: itens.length,
  totalPaginas: itens.length ? 1 : 0,
});
let http: HttpTestingController;
function configurar(): void {
  TestBed.configureTestingModule({
    providers: [
      provideRouter(routes),
      provideHttpClient(withInterceptors([apiInterceptor])),
      provideHttpClientTesting(),
      FornecedorContext,
    ],
  });
  http = TestBed.inject(HttpTestingController);
  TestBed.inject(SessaoStore).resolver(usuario);
}
function csrf(): void {
  http.expectOne('/api/auth/csrf').flush({ cabecalho: 'X-CSRF-TOKEN', token: 'csrf-teste' });
}
async function abrir(url: string, situacao: Empresa['situacao'] = 'APROVADO') {
  const harness = await RouterTestingHarness.create();
  await harness.navigateByUrl(url, FornecedorShellComponent);
  http.expectOne('/api/fornecedor/perfil').flush({ ...empresa, situacao });
  harness.detectChanges();
  await harness.fixture.whenStable();
  return harness;
}

describe('Regras de entrada do painel', () => {
  beforeEach(configurar);
  afterEach(() => http.verify({ ignoreCancelled: true }));
  it.each(['COMPRADOR', 'FORNECEDOR', 'ADMIN'] as const)(
    'mostra a entrada adequada do painel no cabeçalho para %s',
    (perfil) => {
      TestBed.inject(SessaoStore).resolver({ ...usuario, perfil });
      const fixture = TestBed.createComponent(AppComponent);
      fixture.detectChanges();
      const link = fixture.nativeElement.querySelector('a[href="/fornecedor"]');
      if (perfil === 'ADMIN') expect(link).toBeNull();
      else
        expect(link.textContent).toContain(
          perfil === 'COMPRADOR' ? 'Quero vender' : 'Painel do fornecedor',
        );
    },
  );
  it('aceita centavos com vírgula ou ponto e rejeita precisão ou escala inválida', () => {
    expect(precoDecimal('6500,25')).toBe(6500.25);
    expect(precoDecimal('6500.25')).toBe(6500.25);
    for (const valor of ['0', '-1', '1.001', '6.500,00', '10000000000', '1e3'])
      expect(precoDecimal(valor)).toBeNull();
    expect(precoDecimal('9999999999,99')).toBe(9999999999.99);
  });
  it('aceita estoque zero e recusa frações e valores fora do limite', () => {
    expect(estoqueInteiro('0')).toBe(0);
    expect(estoqueInteiro('1000000000')).toBe(1000000000);
    for (const valor of ['-1', '1.5', '1e3', '1000000001'])
      expect(estoqueInteiro(valor)).toBeNull();
  });
  it.each(['COMPRADOR', 'ADMIN', null] as const)(
    'protege áreas próprias para o perfil %s',
    async (perfil) => {
      TestBed.inject(SessaoStore).resolver(perfil ? { ...usuario, perfil } : null);
      const resultado = await firstValueFrom(
        TestBed.runInInjectionContext(() =>
          fornecedorGuard(
            Object.assign(TestBed.inject(Router).routerState.snapshot.root, {
              data: { exigeFornecedor: true },
            }),
            { url: '/fornecedor/ofertas' } as RouterStateSnapshot,
          ),
        ) as Observable<boolean | UrlTree>,
      );
      const destino = TestBed.inject(Router).serializeUrl(resultado as UrlTree);
      expect(destino).toBe(
        perfil === 'COMPRADOR'
          ? '/fornecedor'
          : perfil === 'ADMIN'
            ? '/'
            : '/entrar?retorno=%2Ffornecedor%2Fofertas',
      );
    },
  );
});

describe('Empresa e aprovação', () => {
  beforeEach(configurar);
  afterEach(() => http.verify({ ignoreCancelled: true }));
  it('valida telefone e campos antes do cadastro', () => {
    TestBed.inject(SessaoStore).resolver({ ...usuario, perfil: 'COMPRADOR' });
    const fixture = TestBed.createComponent(EmpresaComponent);
    fixture.detectChanges();
    fixture.componentInstance.salvar();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Informe um telefone válido');
    http.expectNone('/api/auth/csrf');
  });
  it('cadastra empresa e exige novo login sem assumir o novo perfil', () => {
    TestBed.inject(SessaoStore).resolver({ ...usuario, perfil: 'COMPRADOR' });
    const fixture = TestBed.createComponent(EmpresaComponent);
    fixture.detectChanges();
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture.componentInstance.form.setValue({
      razaoSocial: empresa.razaoSocial,
      nomeFantasia: empresa.nomeFantasia,
      descricao: '',
      whatsapp: '+55 (11) 99999-9999',
      cidade: 'São Paulo',
      estado: 'SP',
    });
    fixture.componentInstance.salvar();
    fixture.componentInstance.salvar();
    csrf();
    const req = http.expectOne('/api/fornecedores');
    expect(req.request.body.usuarioId).toBeUndefined();
    req.flush({ ...empresa, situacao: 'PENDENTE' });
    expect(TestBed.inject(SessaoStore).usuario()).toBeNull();
    expect(navegar).toHaveBeenCalledWith(['/entrar'], { queryParams: { retorno: '/fornecedor' } });
  });
  it('preserva edição e versão antiga após conflito e exige recarga explícita', () => {
    const contexto = TestBed.inject(FornecedorContext);
    contexto.empresa.set(empresa);
    const fixture = TestBed.createComponent(EmpresaComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component.form.controls.nomeFantasia.setValue('Nome editado');
    component.form.markAsDirty();
    contexto.empresa.set({ ...empresa, versao: 8 });
    fixture.detectChanges();
    component.salvar();
    csrf();
    const req = http.expectOne('/api/fornecedor/perfil');
    expect(req.request.body.versao).toBe(3);
    req.flush({ codigo: 'REGISTRO_DESATUALIZADO' }, { status: 409, statusText: 'Conflict' });
    expect(component.form.controls.nomeFantasia.value).toBe('Nome editado');
    expect(component.conflito()).toBe(true);
    component.salvar();
    http.expectNone('/api/auth/csrf');
    component.recarregar();
    http.expectOne('/api/fornecedor/perfil').flush({ ...empresa, versao: 8 });
    fixture.detectChanges();
    expect(component.form.controls.nomeFantasia.value).toBe(empresa.nomeFantasia);
    expect(component.conflito()).toBe(false);
  });
  it('exibe a nova pendência ao alterar a identidade de empresa aprovada', () => {
    const contexto = TestBed.inject(FornecedorContext);
    contexto.empresa.set(empresa);
    const fixture = TestBed.createComponent(EmpresaComponent);
    fixture.detectChanges();
    fixture.componentInstance.form.controls.nomeFantasia.setValue('Outro nome');
    fixture.componentInstance.salvar();
    csrf();
    http
      .expectOne('/api/fornecedor/perfil')
      .flush({ ...empresa, nomeFantasia: 'Outro nome', situacao: 'PENDENTE', versao: 4 });
    expect(contexto.aprovado()).toBe(false);
    expect(fixture.componentInstance.sucesso()).toContain('atualizados');
  });
  it('permite consulta mas impede alteração quando a empresa está bloqueada', () => {
    TestBed.inject(FornecedorContext).empresa.set({ ...empresa, situacao: 'BLOQUEADO' });
    const fixture = TestBed.createComponent(EmpresaComponent);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('fieldset').disabled).toBe(true);
    fixture.componentInstance.salvar();
    http.expectNone('/api/auth/csrf');
  });
});

describe('Ofertas e catálogo no painel', () => {
  beforeEach(configurar);
  afterEach(() => http.verify({ ignoreCancelled: true }));
  it('cria oferta vinculada à variante selecionada sem escolher empresa ou moderação', () => {
    TestBed.inject(FornecedorContext).empresa.set(empresa);
    const fixture = TestBed.createComponent(OfertaEditorComponent);
    fixture.componentRef.setInput('variante', variante);
    fixture.detectChanges();
    fixture.componentInstance.form.setValue({ preco: '6200,25', estoque: '0', disponivel: false });
    fixture.componentInstance.salvar();
    fixture.componentInstance.salvar();
    csrf();
    const req = http.expectOne('/api/fornecedor/ofertas');
    expect(req.request.body).toEqual({
      varianteId: ID,
      preco: 6200.25,
      estoque: 0,
      disponivel: false,
    });
    req.flush(oferta);
  });
  it('recusa preço/estoque fracionários e variante incompleta', () => {
    TestBed.inject(FornecedorContext).empresa.set(empresa);
    const fixture = TestBed.createComponent(OfertaEditorComponent);
    fixture.componentRef.setInput('variante', variante);
    fixture.detectChanges();
    fixture.componentInstance.form.setValue({ preco: '1,001', estoque: '1.5', disponivel: true });
    fixture.componentInstance.salvar();
    expect(fixture.componentInstance.form.invalid).toBe(true);
    fixture.componentInstance.form.setValue({ preco: '1,00', estoque: '1', disponivel: true });
    fixture.componentRef.setInput('variante', { ...variante, completa: false });
    fixture.detectChanges();
    fixture.componentInstance.salvar();
    http.expectNone('/api/auth/csrf');
  });
  it('envia versão ao editar e mantém valores após conflito sem repetição automática', () => {
    TestBed.inject(FornecedorContext).empresa.set(empresa);
    const fixture = TestBed.createComponent(OfertaEditorComponent);
    fixture.componentRef.setInput('oferta', oferta);
    fixture.detectChanges();
    fixture.componentInstance.form.controls.preco.setValue('5999,99');
    fixture.componentInstance.salvar();
    csrf();
    const req = http.expectOne('/api/fornecedor/ofertas/' + ID);
    expect(req.request.body).toEqual({ preco: 5999.99, estoque: 3, disponivel: true, versao: 4 });
    req.flush({ codigo: 'REGISTRO_DESATUALIZADO' }, { status: 409, statusText: 'Conflict' });
    expect(fixture.componentInstance.form.controls.preco.value).toBe('5999,99');
    fixture.componentInstance.salvar();
    http.expectNone('/api/auth/csrf');
  });
  it('lista moderação e exige confirmação antes de excluir com a versão correta', async () => {
    const harness = await abrir('/fornecedor/ofertas');
    http.expectOne((r) => r.url === '/api/fornecedor/ofertas').flush(pagina([oferta]));
    harness.detectChanges();
    expect(harness.routeNativeElement!.textContent).toContain('Oferta aprovada');
    const excluir = Array.from(
      harness.routeNativeElement!.querySelectorAll<HTMLButtonElement>('button'),
    ).find((b) => b.textContent!.trim() === 'Excluir oferta')!;
    excluir.click();
    harness.detectChanges();
    http.expectNone('/api/auth/csrf');
    expect(harness.routeNativeElement!.querySelector('.confirmacao')).not.toBeNull();
    harness.routeNativeElement!.querySelector<HTMLButtonElement>('.confirmacao .perigo')!.click();
    csrf();
    const req = http.expectOne((r) => r.url === '/api/fornecedor/ofertas/' + ID);
    expect(req.request.method).toBe('DELETE');
    expect(req.request.params.get('versao')).toBe('4');
    req.flush(null);
    http.expectOne((r) => r.url === '/api/fornecedor/ofertas').flush(pagina([]));
    harness.detectChanges();
    expect(harness.routeNativeElement!.textContent).toContain('Oferta excluída');
  });
  it.each(['PENDENTE', 'BLOQUEADO'] as const)(
    'mantém ofertas em consulta para empresa %s',
    async (situacao) => {
      const harness = await abrir('/fornecedor/ofertas', situacao);
      http.expectOne((r) => r.url === '/api/fornecedor/ofertas').flush(pagina([oferta]));
      harness.detectChanges();
      expect(harness.routeNativeElement!.textContent).toContain('Telefone teste');
      expect(harness.routeNativeElement!.textContent).not.toContain('Editar oferta');
    },
  );
  it('desabilita seleção de variante incompleta e mantém texto da busca na URL', async () => {
    const harness = await abrir('/fornecedor/catalogo?texto=Modelo&pagina=0');
    const req = http.expectOne((r) => r.url === '/api/fornecedor/catalogo/variantes');
    expect(req.request.params.get('texto')).toBe('Modelo');
    req.flush(pagina([{ ...variante, completa: false, pendencias: ['cor'] }]));
    harness.detectChanges();
    const selecionar = Array.from(
      harness.routeNativeElement!.querySelectorAll<HTMLButtonElement>('button'),
    ).find((b) => b.textContent!.trim() === 'Selecionar variante')!;
    expect(selecionar.disabled).toBe(true);
  });
  it('permite repetir uma busca que falhou mesmo sem mudar a URL', async () => {
    const harness = await abrir('/fornecedor/catalogo?texto=Modelo&pagina=0');
    http
      .expectOne((r) => r.url === '/api/fornecedor/catalogo/variantes')
      .flush({}, { status: 503, statusText: 'Error' });
    harness.detectChanges();
    const component = harness.routeDebugElement!.query(By.directive(CatalogoFornecedorComponent))
      .componentInstance as CatalogoFornecedorComponent;
    component.buscar();
    await harness.fixture.whenStable();
    http.expectOne((r) => r.url === '/api/fornecedor/catalogo/variantes').flush(pagina([variante]));
    harness.detectChanges();
    expect(harness.routeNativeElement!.textContent).toContain('Telefone teste');
  });
  it('cancela consulta antiga ao navegar para outra página', async () => {
    const harness = await abrir('/fornecedor/ofertas');
    const antiga = http.expectOne((r) => r.url === '/api/fornecedor/ofertas');
    const component = harness.routeDebugElement!.query(By.directive(OfertasComponent))
      .componentInstance as OfertasComponent;
    component.pagina(1);
    await harness.fixture.whenStable();
    expect(antiga.cancelled).toBe(true);
    const atual = http.expectOne((r) => r.url === '/api/fornecedor/ofertas');
    expect(atual.request.params.get('pagina')).toBe('1');
    atual.flush(pagina([]));
  });
  it('retira o painel quando a sessão expira', async () => {
    const harness = await abrir('/fornecedor/ofertas');
    http
      .expectOne((r) => r.url === '/api/fornecedor/ofertas')
      .flush({}, { status: 401, statusText: 'Unauthorized' });
    await harness.fixture.whenStable();
    expect(TestBed.inject(Router).url).toContain('/entrar?retorno=');
  });
});

describe('Solicitações de catálogo', () => {
  beforeEach(configurar);
  afterEach(() => http.verify({ ignoreCancelled: true }));
  it('envia dados descritivos e lista a solicitação pendente, sem criar produto direto', async () => {
    const harness = await abrir('/fornecedor/solicitacoes');
    http.expectOne((r) => r.url === '/api/fornecedor/catalogo/solicitacoes').flush(pagina([]));
    harness.detectChanges();
    const component = harness.routeDebugElement!.query(By.directive(SolicitacoesComponent))
      .componentInstance as SolicitacoesComponent;
    const dados = {
      nomeProduto: 'Novo produto',
      marca: 'Marca teste',
      modelo: 'Modelo novo',
      categoria: 'Smartphones',
      caracteristicas: '256 GB, preto, novo, RAM 8 GB',
    };
    component.form.setValue(dados);
    component.enviar();
    component.enviar();
    csrf();
    const req = http.expectOne('/api/fornecedor/catalogo/solicitacoes');
    expect(req.request.body).toEqual(dados);
    req.flush({
      ...dados,
      id: ID,
      situacao: 'PENDENTE',
      motivo: null,
      varianteId: null,
      criadoEm: '2026-11-18T10:00:00Z',
    });
    http.expectOne((r) => r.url === '/api/fornecedor/catalogo/solicitacoes').flush(pagina([]));
    expect(component.sucesso()).toContain('enviada');
    expect(component.form.controls.modelo.value).toBe('');
  });
  it('mantém formulário após duplicidade e informa limite de pendentes', async () => {
    const harness = await abrir('/fornecedor/solicitacoes');
    http.expectOne((r) => r.url === '/api/fornecedor/catalogo/solicitacoes').flush(pagina([]));
    const component = harness.routeDebugElement!.query(By.directive(SolicitacoesComponent))
      .componentInstance as SolicitacoesComponent;
    component.form.setValue({
      nomeProduto: 'Novo produto',
      marca: 'Marca teste',
      modelo: 'Modelo novo',
      categoria: 'Smartphones',
      caracteristicas: '256 GB, preto, novo, RAM 8 GB',
    });
    component.enviar();
    csrf();
    http
      .expectOne('/api/fornecedor/catalogo/solicitacoes')
      .flush({ codigo: 'SOLICITACAO_DUPLICADA' }, { status: 409, statusText: 'Conflict' });
    expect(component.form.controls.modelo.value).toBe('Modelo novo');
    expect(component.erro()).toContain('Já existe');
    expect(
      mensagemOperacao(
        new HttpErrorResponse({ status: 409, error: { codigo: 'LIMITE_SOLICITACOES' } }),
      ),
    ).toContain('10 solicitações');
  });
});
