package br.com.comparapreco.catalogo.domain;

import java.util.HashMap;
import java.util.Map;
import br.com.comparapreco.catalogo.entity.Categoria;
import br.com.comparapreco.catalogo.entity.CondicaoProduto;
import br.com.comparapreco.catalogo.entity.Marca;
import br.com.comparapreco.catalogo.entity.Produto;
import br.com.comparapreco.catalogo.entity.VarianteProduto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VarianteProdutoTest {

    private final Produto produto = new Produto(new Categoria("Smartphones"), new Marca("Apple"),
            "Apple iPhone", "iPhone 16 Pro Max");

    @Test
    void deveNormalizarEspacosEMaiusculasSemRemoverAcentos() {
        var variante = new VarianteProduto(produto, 256, "  TITÂNIO" + (char) 9 + "Natural  ",
                CondicaoProduto.NOVO, Map.of(" RAM_GB ", " 8 ", "acabamento", " FOSCO "));

        assertThat(variante.getCor()).isEqualTo("titânio natural");
        assertThat(variante.getAtributos()).containsExactlyInAnyOrderEntriesOf(
                Map.of("ram_gb", "8", "acabamento", "fosco"));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void deveRejeitarArmazenamentoNaoPositivo(int armazenamento) {
        assertThatIllegalArgumentException().isThrownBy(() ->
                new VarianteProduto(produto, armazenamento, null, CondicaoProduto.NOVO, Map.of()));
    }

    @Test
    void devePermitirCaracteristicasNaoAplicaveisComoNulas() {
        var variante = new VarianteProduto(produto, null, null, CondicaoProduto.USADO, Map.of());
        assertThat(variante.getArmazenamentoGb()).isNull();
        assertThat(variante.getCor()).isNull();
    }

    @Test
    void deveRejeitarCorVazia() {
        assertThatIllegalArgumentException().isThrownBy(() ->
                new VarianteProduto(produto, null, "  ", CondicaoProduto.NOVO, Map.of()));
    }

    @Test
    void deveRejeitarChavesEquivalentesSemSobrescreverValores() {
        assertThatIllegalArgumentException().isThrownBy(() ->
                AtributosVariante.normalizar(Map.of("RAM_GB", "8", " ram_gb ", "16")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"cor", "CONDICAO", "armazenamento_gb", "memória", "ram gb"})
    void deveRejeitarChavesReservadasOuInvalidas(String chave) {
        assertThatIllegalArgumentException().isThrownBy(() -> AtributosVariante.normalizar(Map.of(chave, "8")));
    }

    @Test
    void deveRejeitarMapaNuloEValorNulo() {
        assertThatIllegalArgumentException().isThrownBy(() -> AtributosVariante.normalizar(null));
        Map<String, String> atributos = new HashMap<>();
        atributos.put("ram_gb", null);
        assertThatIllegalArgumentException().isThrownBy(() -> AtributosVariante.normalizar(atributos));
    }

    @Test
    void deveLimitarQuantidadeETamanhoEmBytesDosAtributos() {
        Map<String, String> atributos = new HashMap<>();
        for (int i = 0; i < 9; i++) {
            atributos.put("atributo" + i, "8");
        }
        assertThatIllegalArgumentException().isThrownBy(() -> AtributosVariante.normalizar(atributos));
        atributos.remove("atributo8");
        atributos.replaceAll((chave, valor) -> "漢".repeat(64));
        assertThatIllegalArgumentException().isThrownBy(() -> AtributosVariante.normalizar(atributos));
    }

    @Test
    void devePreservarIdentidadeQuandoMapaOriginalForAlterado() {
        Map<String, String> entrada = new HashMap<>(Map.of("ram_gb", "8"));
        var variante = new VarianteProduto(produto, 256, "preto", CondicaoProduto.NOVO, entrada);
        entrada.put("ram_gb", "16");

        assertThat(variante.getAtributos()).containsEntry("ram_gb", "8");
        assertThatThrownBy(() -> variante.getAtributos().put("ram_gb", "32"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void deveRejeitarImagemForaDeHttpOuHttps() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Produto(new Categoria("Smartphones"),
                new Marca("Apple"), "iPhone", "iPhone 16", null, "file:///etc/passwd"));
    }
}
