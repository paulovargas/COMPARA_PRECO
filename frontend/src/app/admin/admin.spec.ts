import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import {
  provideRouter,
  Router,
  ActivatedRoute,
  RouterStateSnapshot,
  UrlTree,
} from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { By } from '@angular/platform-browser';
import { firstValueFrom, Observable } from 'rxjs';
import { routes } from '../app.routes';
import { apiInterceptor } from '../conta/api.interceptor';
import { SessaoStore, UsuarioAtual } from '../conta/sessao.store';
import { AdminShellComponent } from './admin-shell.component';
import { AdminListaComponent } from './admin-lista.component';
import { CatalogoAdminComponent } from './catalogo-admin.component';
import { ConfiguracaoAdminComponent } from './configuracao-admin.component';
import { ReferenciaCatalogoComponent } from './referencia-catalogo.component';
import { adminGuard } from './admin.guard';

const usuario: UsuarioAtual = {
  id: 'admin',
  nome: 'Administrador',
  email: 'admin@teste.local',
  perfil: 'ADMIN',
  ativo: true,
  criadoEm: '2026-11-19T12:00:00Z',
};
const conta = { ...usuario, id: 'outro', perfil: 'COMPRADOR', versao: 3 };
const empresa = {
  id: 'empresa',
  nomeFantasia: 'Empresa teste',
  razaoSocial: 'Empresa Ltda',
  cidade: 'São Paulo',
  estado: 'SP',
  situacao: 'PENDENTE',
  versao: 2,
};
const marca = { id: 'marca', nome: 'Marca teste', versao: 4 };
const pagina = <T>(itens: T[]) => ({
  itens,
  pagina: 0,
  tamanho: 12,
  totalElementos: itens.length,
  totalPaginas: itens.length ? 1 : 0,
});
let http: HttpTestingController;
function csrf() {
  http.expectOne('/api/auth/csrf').flush({ cabecalho: 'X-CSRF-TOKEN', token: 'teste' });
}
function lista(area: string) {
  return http.expectOne((r) => r.url === `/api/admin/${area}`);
}
async function abrir(url: string) {
  const harness = await RouterTestingHarness.create();
  await harness.navigateByUrl(url, AdminShellComponent);
  return harness;
}
describe('Painel administrativo', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter(routes),
        provideHttpClient(withInterceptors([apiInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    TestBed.inject(SessaoStore).resolver(usuario);
  });
  afterEach(() => http.verify({ ignoreCancelled: true }));
  it.each(['COMPRADOR', 'FORNECEDOR', null] as const)(
    'protege administração para perfil %s',
    async (perfil) => {
      TestBed.inject(SessaoStore).resolver(perfil ? { ...usuario, perfil } : null);
      const resultado = await firstValueFrom(
        TestBed.runInInjectionContext(() =>
          adminGuard(TestBed.inject(ActivatedRoute).snapshot, {
            url: '/admin',
          } as RouterStateSnapshot),
        ) as Observable<boolean | UrlTree>,
      );
      expect(resultado instanceof UrlTree).toBe(true);
      expect(TestBed.inject(Router).serializeUrl(resultado as UrlTree)).toContain(
        perfil ? '/' : '/entrar?retorno=',
      );
    },
  );
  it('envia versão e CSRF somente após confirmar acesso e impede duplo envio', async () => {
    const harness = await abrir('/admin/usuarios');
    lista('usuarios').flush(pagina([conta]));
    harness.detectChanges();
    const component = harness.routeDebugElement!.query(By.directive(AdminListaComponent))
      .componentInstance as AdminListaComponent;
    component.editar(component.dados()!.itens[0]);
    component.form.controls.perfil.setValue('ADMIN');
    http.expectNone('/api/admin/usuarios/outro/acesso');
    component.salvar();
    component.salvar();
    csrf();
    const req = http.expectOne('/api/admin/usuarios/outro/acesso');
    expect(req.request.body).toEqual({ ativo: true, perfil: 'ADMIN', versao: 3 });
    expect(req.request.headers.get('X-CSRF-TOKEN')).toBe('teste');
    req.flush({ ...conta, perfil: 'ADMIN', versao: 4 });
    lista('usuarios').flush(pagina([]));
    expect(component.selecionado()).toBeNull();
    expect(component.sucesso()).toContain('concluída');
  });
  it('exige motivo de bloqueio e preserva edição após conflito', async () => {
    const harness = await abrir('/admin/fornecedores');
    lista('fornecedores').flush(pagina([empresa]));
    const component = harness.routeDebugElement!.query(By.directive(AdminListaComponent))
      .componentInstance as AdminListaComponent;
    component.editar(component.dados()!.itens[0]);
    component.form.controls.destino.setValue('BLOQUEADO');
    component.salvar();
    expect(component.erro()).toContain('motivo');
    component.form.controls.motivo.setValue('Cadastro em revisão');
    component.salvar();
    csrf();
    const req = http.expectOne('/api/admin/fornecedores/empresa/situacao');
    expect(req.request.body.versao).toBe(2);
    req.flush({ codigo: 'REGISTRO_DESATUALIZADO' }, { status: 409, statusText: 'Conflict' });
    expect(component.form.controls.motivo.value).toBe('Cadastro em revisão');
    expect(component.conflito()).toBe(true);
    component.salvar();
    component.cancelar();
    lista('fornecedores').flush(pagina([{ ...empresa, versao: 3 }]));
  });
  it('impede atender solicitação sem variante e manda resposta versionada', async () => {
    const harness = await abrir('/admin/solicitacoes');
    lista('solicitacoes').flush(
      pagina([
        {
          solicitacao: {
            id: 'pedido',
            nomeProduto: 'Produto',
            marca: 'Marca',
            modelo: 'Modelo',
            categoria: 'Celular',
            caracteristicas: '256 GB preto novo',
            situacao: 'PENDENTE',
          },
          fornecedor: 'Empresa',
          fornecedorId: 'empresa',
          versao: 0,
        },
      ]),
    );
    const component = harness.routeDebugElement!.query(By.directive(AdminListaComponent))
      .componentInstance as AdminListaComponent;
    component.editar(component.dados()!.itens[0]);
    component.form.controls.motivo.setValue('Disponível no catálogo');
    component.salvar();
    expect(component.erro()).toContain('variante');
    component.form.controls.varianteId.setValue('variante');
    component.salvar();
    csrf();
    const req = http.expectOne('/api/admin/solicitacoes/pedido/resolucao');
    expect(req.request.body).toEqual({
      situacao: 'ATENDIDA',
      motivo: 'Disponível no catálogo',
      varianteId: 'variante',
      versao: 0,
    });
    req.flush(null);
    lista('solicitacoes').flush(pagina([]));
  });
  it('aplica filtro da URL e cancela consulta anterior ao paginar', async () => {
    const harness = await abrir('/admin/ofertas?situacao=PENDENTE');
    const primeira = lista('ofertas');
    expect(primeira.request.params.get('moderacao')).toBe('PENDENTE');
    const component = harness.routeDebugElement!.query(By.directive(AdminListaComponent))
      .componentInstance as AdminListaComponent;
    component.pagina(1);
    await harness.fixture.whenStable();
    expect(primeira.cancelled).toBe(true);
    lista('ofertas').flush({ ...pagina([]), pagina: 1 });
  });
  it('não permite revisar o próprio acesso pela interface', async () => {
    const harness = await abrir('/admin/usuarios');
    lista('usuarios').flush(pagina([{ ...usuario, versao: 0 }]));
    harness.detectChanges();
    const button = harness.routeNativeElement!.querySelector('article button') as HTMLButtonElement;
    expect(button.disabled).toBe(true);
  });
  it('cria marca e exige confirmação separada para excluir com versão', async () => {
    const harness = await abrir('/admin/catalogo/marcas');
    lista('catalogo/marcas').flush(pagina([marca]));
    const component = harness.routeDebugElement!.query(By.directive(CatalogoAdminComponent))
      .componentInstance as CatalogoAdminComponent;
    component.editar(null);
    component.form.controls.nome.setValue('Outra marca');
    component.salvar();
    csrf();
    const post = http.expectOne('/api/admin/catalogo/marcas');
    expect(post.request.body).toEqual({ nome: 'Outra marca', versao: 0 });
    post.flush({ ...marca, id: 'nova' });
    lista('catalogo/marcas').flush(pagina([marca]));
    component.excluir.set(marca);
    http.expectNone((r) => r.method === 'DELETE');
    component.confirmarExclusao();
    csrf();
    const del = http.expectOne('/api/admin/catalogo/marcas/marca?versao=4');
    expect(del.request.method).toBe('DELETE');
    del.flush(null);
    lista('catalogo/marcas').flush(pagina([]));
  });
  it('preserva formulário do catálogo em conflito e bloqueia novo envio', async () => {
    const harness = await abrir('/admin/catalogo/marcas');
    lista('catalogo/marcas').flush(pagina([marca]));
    const component = harness.routeDebugElement!.query(By.directive(CatalogoAdminComponent))
      .componentInstance as CatalogoAdminComponent;
    component.editar(marca);
    component.form.controls.nome.setValue('Marca revisada');
    component.salvar();
    csrf();
    http
      .expectOne('/api/admin/catalogo/marcas/marca')
      .flush({ codigo: 'REGISTRO_DESATUALIZADO' }, { status: 409, statusText: 'Conflict' });
    expect(component.conflito()).toBe(true);
    expect(component.form.controls.nome.value).toBe('Marca revisada');
    component.salvar();
  });
  it('valida atributos duplicados antes de criar variante', async () => {
    const harness = await abrir('/admin/catalogo/variantes');
    lista('catalogo/variantes').flush(pagina([]));
    const component = harness.routeDebugElement!.query(By.directive(CatalogoAdminComponent))
      .componentInstance as CatalogoAdminComponent;
    component.editar(null);
    component.form.patchValue({ produtoId: 'produto', atributos: 'ram_gb=8\nram_gb=12' });
    component.salvar();
    expect(component.erro()).toContain('sem repetir');
  });
  it('seleciona referência com paginação sem limitar escolha à primeira página', () => {
    const fixture = TestBed.createComponent(ReferenciaCatalogoComponent);
    fixture.componentRef.setInput('tipo', 'marcas');
    fixture.detectChanges();
    const component = fixture.componentInstance;
    const spy = vi.fn();
    component.escolher.subscribe(spy);
    component.carregar(2);
    const req = lista('catalogo/marcas');
    expect(req.request.params.get('pagina')).toBe('2');
    req.flush({ ...pagina([marca]), pagina: 2 });
    component.selecionar(marca);
    expect(spy).toHaveBeenCalledWith('marca');
    expect(component.dados()).toBeNull();
  });
  it('mantém configuração editada após conflito até recarregar explicitamente', async () => {
    const harness = await abrir('/admin/configuracoes');
    http
      .expectOne('/api/admin/configuracoes')
      .flush({ aberturaContato: 'Mensagem inicial', versao: 2 });
    const component = harness.routeDebugElement!.query(By.directive(ConfiguracaoAdminComponent))
      .componentInstance as ConfiguracaoAdminComponent;
    component.form.controls.aberturaContato.setValue('Nova mensagem de contato');
    component.salvar();
    component.salvar();
    csrf();
    const req = http.expectOne('/api/admin/configuracoes');
    expect(req.request.body.versao).toBe(2);
    req.flush({ codigo: 'REGISTRO_DESATUALIZADO' }, { status: 409, statusText: 'Conflict' });
    expect(component.conflito()).toBe(true);
    expect(component.form.controls.aberturaContato.value).toBe('Nova mensagem de contato');
    component.carregar();
    http
      .expectOne('/api/admin/configuracoes')
      .flush({ aberturaContato: 'Mensagem atual', versao: 3 });
    expect(component.conflito()).toBe(false);
  });
  it('retira painel privado após expiração de sessão', async () => {
    const harness = await abrir('/admin/usuarios');
    lista('usuarios').flush(pagina([conta]));
    TestBed.inject(SessaoStore).expirar();
    harness.detectChanges();
    await harness.fixture.whenStable();
    expect(TestBed.inject(Router).url).toContain('/entrar');
  });
  it('volta à busca ao sair voluntariamente sem disputar com o redirecionamento do cabeçalho', async () => {
    const harness = await abrir('/admin/usuarios');
    lista('usuarios').flush(pagina([conta]));
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    TestBed.inject(SessaoStore).resolver(null);
    harness.detectChanges();
    await harness.fixture.whenStable();
    expect(navegar).toHaveBeenCalledWith(['/'], { queryParams: {} });
  });
});
