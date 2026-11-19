import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { apiInterceptor } from '../conta/api.interceptor';
import { SessaoStore, UsuarioAtual } from '../conta/sessao.store';
import { Variante } from '../catalogo/catalogo.models';
import { Empresa } from './fornecedor.models';
import { FornecedorContext } from './fornecedor.context';
import { ImportacaoComponent } from './importacao.component';
import { Importacao } from './importacao.models';

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

const previa: Importacao = {
  id: ID,
  situacao: 'PREVIA',
  expiraEm: '2026-11-19T15:00:00Z',
  criar: 1,
  atualizar: 0,
  ignorar: 0,
  invalidas: 0,
  linhas: [
    {
      numero: 2,
      varianteId: ID,
      preco: '200,50',
      estoque: '2',
      disponivel: 'true',
      variante,
      acao: 'CRIAR',
      erros: [],
    },
  ],
};
describe('Importação de ofertas', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([apiInterceptor])),
        provideHttpClientTesting(),
        FornecedorContext,
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: convertToParamMap({}) } },
        },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    TestBed.inject(SessaoStore).resolver(usuario);
    TestBed.inject(FornecedorContext).empresa.set(empresa);
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  });
  afterEach(() => http.verify());
  function abrir() {
    const fixture = TestBed.createComponent(ImportacaoComponent);
    fixture.detectChanges();
    return fixture;
  }
  function selecionar(
    component: ImportacaoComponent,
    arquivo = new File(['exemplo'], 'ofertas.csv'),
  ) {
    component.selecionar({ target: { files: [arquivo] } } as unknown as Event);
  }
  function csrf() {
    http.expectOne('/api/auth/csrf').flush({ cabecalho: 'X-CSRF-TOKEN', token: 'teste' });
  }
  it('envia FormData com CSRF e exige confirmação separada', () => {
    const fixture = abrir();
    const component = fixture.componentInstance;
    selecionar(component);
    component.preparar();
    component.preparar();
    csrf();
    const req = http.expectOne('/api/fornecedor/importacoes');
    expect(req.request.body).toBeInstanceOf(FormData);
    expect(req.request.headers.get('X-CSRF-TOKEN')).toBe('teste');
    expect(req.request.body.get('arquivo').name).toBe('ofertas.csv');
    req.flush(previa);
    expect(component.previa()?.situacao).toBe('PREVIA');
    http.expectNone(`/api/fornecedor/importacoes/${ID}/confirmacao`);
    component.confirmar();
    component.confirmar();
    csrf();
    http
      .expectOne(`/api/fornecedor/importacoes/${ID}/confirmacao`)
      .flush({ ...previa, situacao: 'APLICADA' });
    component.confirmar();
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Importação concluída');
  });
  it('mostra erros por linha e impede confirmação de lote inválido', () => {
    const fixture = abrir();
    const component = fixture.componentInstance;
    component.previa.set({
      ...previa,
      invalidas: 1,
      linhas: [
        { ...previa.linhas[0], acao: 'INVALIDA', erros: ['Variante repetida neste arquivo.'] },
      ],
    });
    fixture.detectChanges();
    component.confirmar();
    expect(fixture.nativeElement.textContent).toContain('Variante repetida neste arquivo.');
    const botao = [...fixture.nativeElement.querySelectorAll('button')].find((b: any) =>
      b.textContent.includes('Confirmar'),
    ) as HTMLButtonElement;
    expect(botao.disabled).toBe(true);
  });
  it('mantém prévia após conflito e exige gerar outra', () => {
    const fixture = abrir();
    const component = fixture.componentInstance;
    component.previa.set(previa);
    component.confirmar();
    csrf();
    http
      .expectOne(`/api/fornecedor/importacoes/${ID}/confirmacao`)
      .flush(
        { codigo: 'IMPORTACAO_DESATUALIZADA', mensagem: 'Uma oferta mudou. Gere nova prévia.' },
        { status: 409, statusText: 'Conflict' },
      );
    expect(component.previa()).toEqual(previa);
    expect(component.bloqueada()).toBe(true);
    component.confirmar();
    expect(component.erro()).toContain('nova prévia');
  });
  it('permite repetir confirmação após falha de conexão com proteção idempotente da API', () => {
    const component = abrir().componentInstance;
    component.previa.set(previa);
    component.confirmar();
    csrf();
    http
      .expectOne(`/api/fornecedor/importacoes/${ID}/confirmacao`)
      .error(new ProgressEvent('error'));
    expect(component.bloqueada()).toBe(false);
    component.confirmar();
    csrf();
    http
      .expectOne(`/api/fornecedor/importacoes/${ID}/confirmacao`)
      .flush({ ...previa, situacao: 'APLICADA' });
    expect(component.previa()?.situacao).toBe('APLICADA');
  });
  it('recusa formato ou tamanho inválido antes do envio e limpa a prévia anterior', () => {
    const component = abrir().componentInstance;
    component.previa.set(previa);
    selecionar(component, new File(['abc'], 'ofertas.xls'));
    component.preparar();
    expect(component.arquivo()).toBeNull();
    expect(component.previa()).toBeNull();
    selecionar(component, new File([new Uint8Array(2 * 1024 * 1024 + 1)], 'ofertas.csv'));
    expect(component.arquivo()).toBeNull();
    expect(component.erro()).toContain('2 MB');
  });
  it.each(['PENDENTE', 'BLOQUEADO'] as const)(
    'impede envio e confirmação para empresa %s',
    (situacao) => {
      TestBed.inject(FornecedorContext).empresa.set({ ...empresa, situacao });
      const component = abrir().componentInstance;
      selecionar(component);
      component.preparar();
      component.previa.set(previa);
      component.confirmar();
      expect(component.ocupado()).toBe(false);
    },
  );
  it('restaura a prévia pela URL ao recarregar a página', () => {
    Object.assign(TestBed.inject(ActivatedRoute).snapshot, {
      queryParamMap: convertToParamMap({ previa: ID }),
    });
    const fixture = abrir();
    http.expectOne(`/api/fornecedor/importacoes/${ID}`).flush(previa);
    expect(fixture.componentInstance.previa()?.id).toBe(ID);
  });
});
