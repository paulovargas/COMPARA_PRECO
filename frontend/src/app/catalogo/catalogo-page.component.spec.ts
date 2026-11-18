import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { registerLocaleData } from '@angular/common';
import localePt from '@angular/common/locales/pt';
import { routes } from '../app.routes';
import { CatalogoPageComponent } from './catalogo-page.component';
import { CatalogoService } from './catalogo.service';
import { FiltrosComponent } from './filtros.component';
import { By } from '@angular/platform-browser';
import { Comparacao, Pagina, VarianteBusca } from './catalogo.models';

registerLocaleData(localePt);
const ID = '40000000-0000-0000-0000-000000000001';
const FORNECEDOR = '50000000-0000-0000-0000-000000000001';
const variante = {
  id: ID,
  produtoId: ID,
  nomeProduto: 'Telefone de teste',
  modelo: 'Modelo X',
  marca: 'Marca X',
  categoria: 'Smartphones',
  armazenamentoGb: 256,
  cor: 'preto',
  condicao: 'NOVO' as const,
  atributos: { ram_gb: '8' },
  completa: true,
  pendencias: [],
};
const pagina: Pagina<VarianteBusca> = {
  itens: [
    {
      variante,
      imagemUrl: 'https://example.test/foto.jpg',
      menorPreco: 6200,
      quantidadeOfertas: 2,
    },
  ],
  pagina: 0,
  tamanho: 12,
  totalElementos: 1,
  totalPaginas: 1,
};
const comparacao: Comparacao = {
  variante,
  menorPreco: 6200,
  ofertas: {
    itens: [1, 2].map((i) => ({
      id: String(i),
      fornecedorId: FORNECEDOR,
      fornecedor: `Empresa ${i}`,
      cidade: 'São Paulo',
      estado: 'SP',
      preco: 6200,
      estoque: 3,
      atualizadoEm: '2026-11-18T10:00:00Z',
      menorPreco: true,
    })),
    pagina: 0,
    tamanho: 12,
    totalElementos: 2,
    totalPaginas: 1,
  },
};

describe('Catálogo público', () => {
  let http: HttpTestingController;
  let harness: RouterTestingHarness;
  const busca = '/api/catalogo/variantes';

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [provideRouter(routes), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    harness = await RouterTestingHarness.create();
  });
  afterEach(() => http.verify({ ignoreCancelled: true }));

  async function abrir(url = '/'): Promise<CatalogoPageComponent> {
    const component = await harness.navigateByUrl(url, CatalogoPageComponent);
    http.expectOne('/api/catalogo/categorias').flush([]);
    http.expectOne('/api/catalogo/marcas').flush([]);
    return component;
  }
  function responderBusca(dados = pagina): void {
    http.expectOne((r) => r.url === busca).flush(dados);
    harness.detectChanges();
  }
  function texto(): string {
    return harness.routeNativeElement!.textContent!;
  }

  it('mostra carregamento e depois resultados em reais com características e link filtrado', async () => {
    await abrir('/?texto=Telefone&estado=SP&pagina=2');
    expect(texto()).toContain('Buscando as melhores opções');
    const request = http.expectOne((r) => r.url === busca);
    expect(request.request.params.get('texto')).toBe('Telefone');
    expect(request.request.params.get('estado')).toBe('SP');
    request.flush({ ...pagina, pagina: 2, totalElementos: 25, totalPaginas: 3 });
    harness.detectChanges();
    expect(texto()).toContain('Telefone de teste');
    expect(texto()).toContain('6.200,00');
    expect(texto()).toContain('256 GB');
    expect(texto()).toContain('RAM (GB): 8');
    const link = harness.routeNativeElement!.querySelector<HTMLAnchorElement>('.produto a')!;
    expect(link.getAttribute('href')).toContain('estado=SP');
    expect(link.getAttribute('href')).toContain('buscaPagina=2');
  });

  it('pagina pelos botões mantendo os filtros e desabilita os limites', async () => {
    await abrir('/?estado=SP');
    responderBusca({ ...pagina, totalElementos: 13, totalPaginas: 2 });
    const botoes = Array.from(
      harness.routeNativeElement!.querySelectorAll<HTMLButtonElement>('app-paginacao button'),
    );
    expect(botoes[0].disabled).toBe(true);
    botoes[1].click();
    await harness.fixture.whenStable();
    const request = http.expectOne((r) => r.url === busca);
    expect(request.request.params.get('pagina')).toBe('1');
    expect(request.request.params.get('estado')).toBe('SP');
    request.flush({ ...pagina, pagina: 1, totalElementos: 13, totalPaginas: 2 });
    harness.detectChanges();
    const ultima =
      harness.routeNativeElement!.querySelectorAll<HTMLButtonElement>('app-paginacao button');
    expect(ultima[1].disabled).toBe(true);
  });

  it('apresenta busca vazia sem inventar produtos', async () => {
    await abrir();
    responderBusca({ ...pagina, itens: [], totalElementos: 0, totalPaginas: 0 });
    expect(texto()).toContain('Nenhuma oferta por aqui');
    expect(harness.routeNativeElement!.querySelectorAll('.produto').length).toBe(0);
  });

  it('permite tentar novamente após falha de conexão', async () => {
    const component = await abrir();
    http.expectOne((r) => r.url === busca).flush({}, { status: 503, statusText: 'Unavailable' });
    harness.detectChanges();
    expect(texto()).toContain('Não foi possível carregar as ofertas');
    component.tentarNovamente();
    responderBusca();
    expect(texto()).toContain('Telefone de teste');
  });

  it('cancela a busca antiga quando os filtros mudam', async () => {
    const component = await abrir('/?texto=antigo');
    const antiga = http.expectOne((r) => r.url === busca);
    component.aplicar({ texto: 'novo' });
    await harness.fixture.whenStable();
    expect(antiga.cancelled).toBe(true);
    const nova = http.expectOne((r) => r.url === busca);
    expect(nova.request.params.get('texto')).toBe('novo');
    nova.flush(pagina);
    harness.detectChanges();
    expect(texto()).toContain('Telefone de teste');
  });

  it('restaura filtros ao navegar pela URL e reinicia a página ao aplicar um filtro', async () => {
    const component = await abrir('/?texto=Telefone&cor=preto&pagina=3');
    responderBusca();
    const filtros = harness.routeDebugElement!.query(By.directive(FiltrosComponent))
      .componentInstance as FiltrosComponent;
    expect(filtros.form.controls.cor.value).toBe('preto');
    component.aplicar({ cor: 'azul' });
    await harness.fixture.whenStable();
    const request = http.expectOne((r) => r.url === busca);
    expect(request.request.params.get('pagina')).toBe('0');
    expect(request.request.params.get('cor')).toBe('azul');
    expect(TestBed.inject(Router).url).toContain('texto=Telefone');
    request.flush(pagina);
    await harness.navigateByUrl('/?texto=restaurado&pagina=1');
    responderBusca();
    expect(filtros.form.controls.texto.value).toBe('restaurado');
  });

  it('recusa armazenamento fracionário no formulário', async () => {
    await abrir();
    responderBusca();
    const filtros = harness.routeDebugElement!.query(By.directive(FiltrosComponent))
      .componentInstance as FiltrosComponent;
    filtros.form.controls.armazenamentoGb.setValue('1.5');
    filtros.buscar();
    harness.detectChanges();
    expect(texto()).toContain('Informe um número inteiro positivo');
    http.expectNone((r) => r.url === busca);
  });

  it.each([
    'pagina=-1',
    'pagina=1.5',
    'tamanho=51',
    'marcaId=invalido',
    'condicao=OUTRA',
    'estado=XX',
  ])('recusa parâmetro inválido na URL: %s', async (filtro) => {
    await abrir('/?' + filtro);
    expect(texto()).toContain('Os filtros do endereço são inválidos');
    http.expectNone((r) => r.url === busca);
  });

  it('mostra todos os empates e permite filtrar por fornecedor preservando a volta', async () => {
    const component = await abrir(`/comparar/${ID}?estado=SP&buscaPagina=2`);
    http.expectOne((r) => r.url.endsWith('/ofertas')).flush(comparacao);
    harness.detectChanges();
    expect(harness.routeNativeElement!.querySelectorAll('.selo').length).toBe(2);
    expect(texto()).toContain('São Paulo');
    expect(texto()).toContain('3 unidades em estoque');
    expect(texto()).toContain('Atualizado em');
    expect(component.parametrosVoltar()).toEqual({ estado: 'SP', pagina: 2 });
    component.fornecedor(FORNECEDOR);
    await harness.fixture.whenStable();
    const request = http.expectOne((r) => r.url.endsWith('/ofertas'));
    expect(request.request.params.get('fornecedorId')).toBe(FORNECEDOR);
    expect(request.request.params.get('estado')).toBe('SP');
    request.flush(comparacao);
  });

  it('explica variante indisponível e comparação sem ofertas', async () => {
    const component = await abrir(`/comparar/${ID}`);
    http
      .expectOne((r) => r.url.endsWith('/ofertas'))
      .flush({}, { status: 404, statusText: 'Not Found' });
    harness.detectChanges();
    expect(texto()).toContain('Este produto não está disponível');
    component.tentarNovamente();
    http
      .expectOne((r) => r.url.endsWith('/ofertas'))
      .flush({
        ...comparacao,
        menorPreco: null,
        ofertas: { ...comparacao.ofertas, itens: [], totalElementos: 0, totalPaginas: 0 },
      });
    harness.detectChanges();
    expect(texto()).toContain('Nenhuma oferta disponível nesta seleção');
    expect(texto()).not.toContain('Menor preço encontrado');
  });

  it('mantém busca utilizável quando as opções falham e permite recarregá-las', async () => {
    const component = await harness.navigateByUrl('/', CatalogoPageComponent);
    http.expectOne('/api/catalogo/categorias').flush({}, { status: 500, statusText: 'Error' });
    http.match('/api/catalogo/marcas');
    responderBusca();
    expect(texto()).toContain('Não foi possível carregar categorias');
    component.carregarOpcoes();
    http.expectOne('/api/catalogo/categorias').flush([]);
    http.expectOne('/api/catalogo/marcas').flush([]);
    harness.detectChanges();
    expect(texto()).not.toContain('Não foi possível carregar categorias');
  });

  it('remove imagem quebrada e mostra identificação da categoria', async () => {
    await abrir();
    responderBusca();
    harness.routeNativeElement!.querySelector('img')!.dispatchEvent(new Event('error'));
    harness.detectChanges();
    expect(harness.routeNativeElement!.querySelector('img')).toBeNull();
    expect(texto()).toContain('Smartphones');
  });

  it('envia somente parâmetros de API e escapa texto sem acrescentar filtros', () => {
    TestBed.inject(CatalogoService)
      .buscar({ texto: 'A&B + C', cidade: 'São Paulo', pagina: 0, tamanho: 12 })
      .subscribe();
    const request = http.expectOne((r) => r.url === busca);
    expect(request.request.params.get('texto')).toBe('A&B + C');
    expect(request.request.params.keys().sort()).toEqual(['cidade', 'pagina', 'tamanho', 'texto']);
    expect(request.request.urlWithParams).toContain('A%26B%20%2B%20C');
    request.flush(pagina);
  });
});
