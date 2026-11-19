package br.com.comparapreco.importacao.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import br.com.comparapreco.shared.exception.ErroNegocioException;
import org.apache.commons.csv.CSVFormat;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class LeitorOfertas {
    public static final List<String> CABECALHO = List.of("varianteId", "preco", "estoque", "disponivel");
    public static final int MAX_LINHAS = 500;

    public List<List<String>> ler(MultipartFile arquivo) {
        if (arquivo.isEmpty() || arquivo.getSize() > 2 * 1024 * 1024) {
            throw invalido("Envie um arquivo de até 2 MB, com pelo menos uma oferta.");
        }
        String nome = arquivo.getOriginalFilename();
        String extensao = nome == null ? "" : nome.toLowerCase(Locale.ROOT);
        try {
            byte[] bytes = arquivo.getBytes();
            List<List<String>> linhas;
            if (extensao.endsWith(".csv")) { linhas = csv(bytes); }
            else if (extensao.endsWith(".xlsx")) { linhas = xlsx(bytes); }
            else { throw invalido("Use um arquivo CSV ou XLSX."); }
            if (linhas.isEmpty() || !linhas.removeFirst().equals(CABECALHO)) {
                throw invalido("Cabeçalho esperado: varianteId;preco;estoque;disponivel.");
            }
            if (linhas.isEmpty()) { throw invalido("Preencha pelo menos uma oferta abaixo do cabeçalho."); }
            return linhas;
        } catch (ErroNegocioException e) { throw e; }
        catch (Exception e) { throw invalido("Arquivo ilegível. Use o modelo, CSV UTF-8 ou XLSX sem fórmulas."); }
    }

    private List<List<String>> csv(byte[] bytes) throws Exception {
        var decoder = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT);
        var linhas = new ArrayList<List<String>>();
        try (var reader = new InputStreamReader(new ByteArrayInputStream(bytes), decoder);
             var parser = CSVFormat.DEFAULT.builder().setDelimiter(';').setIgnoreEmptyLines(false).get().parse(reader)) {
            for (var record : parser) {
                var campos = new ArrayList<String>();
                for (String campo : record) {
                    if (campo.contains("\n") || campo.contains("\r")) { throw invalido("Cada oferta deve ocupar uma única linha do CSV."); }
                    campos.add(campo.strip());
                }
                if (linhas.isEmpty() && !campos.isEmpty()) { campos.set(0, campos.getFirst().replaceFirst("^\uFEFF", "")); }
                adicionar(linhas, campos);
            }
        }
        return linhas;
    }

    private List<List<String>> xlsx(byte[] bytes) throws Exception {
        limitarZip(bytes);
        // Mantém as proteções padrão do POI contra arquivos ZIP excessivamente comprimidos.
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            if (workbook.getNumberOfSheets() != 1 || !workbook.getExternalLinksTable().isEmpty()
                    || workbook.getPackagePart().getContentType().contains("macroEnabled")) {
                throw invalido("Use somente uma planilha XLSX sem macros nem vínculos externos.");
            }
            var sheet = workbook.getSheetAt(0);
            if (sheet.getLastRowNum() > MAX_LINHAS) { throw invalido("O limite é de 500 ofertas por arquivo."); }
            var linhas = new ArrayList<List<String>>();
            for (int i = 0; i <= sheet.getLastRowNum(); i++) {
                var row = sheet.getRow(i);
                var campos = new ArrayList<String>();
                if (row != null && row.getLastCellNum() > 4) { throw invalido("Use apenas as quatro colunas do modelo."); }
                for (int j = 0; j < 4; j++) {
                    var cell = row == null ? null : row.getCell(j);
                    String valor = cell == null ? "" : switch (cell.getCellType()) {
                        case BLANK -> "";
                        case STRING -> cell.getStringCellValue().strip();
                        case BOOLEAN -> Boolean.toString(cell.getBooleanCellValue());
                        case NUMERIC -> java.math.BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
                        default -> throw invalido("Fórmulas e células com erro não são aceitas. Substitua por valores.");
                    };
                    campos.add(valor);
                }
                adicionar(linhas, campos);
            }
            return linhas;
        }
    }

    private void limitarZip(byte[] bytes) throws java.io.IOException {
        long total = 0;
        int entradas = 0;
        try (var zip = new java.util.zip.ZipInputStream(new ByteArrayInputStream(bytes))) {
            var buffer = new byte[8192];
            while (zip.getNextEntry() != null) {
                if (++entradas > 1000) { throw invalido("Planilha com estrutura excessiva. Use o modelo."); }
                int lidos;
                while ((lidos = zip.read(buffer)) != -1) {
                    total += lidos;
                    if (total > 16 * 1024 * 1024) { throw invalido("Planilha descompactada excede 16 MB. Use o modelo."); }
                }
            }
        }
    }

    private void adicionar(List<List<String>> linhas, List<String> campos) {
        if (linhas.size() > MAX_LINHAS) { throw invalido("O limite é de 500 ofertas por arquivo."); }
        if (campos.size() > 4 || campos.stream().anyMatch(c -> c.length() > 256)) {
            throw invalido("Use quatro colunas e no máximo 256 caracteres por campo.");
        }
        linhas.add(campos);
    }

    public byte[] modelo(String formato) {
        if ("csv".equals(formato)) { return (String.join(";", CABECALHO) + "\r\n").getBytes(StandardCharsets.UTF_8); }
        if (!"xlsx".equals(formato)) { throw invalido("Formato de modelo inválido."); }
        try (var workbook = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Ofertas");
            var row = sheet.createRow(0);
            for (int i = 0; i < 4; i++) {
                row.createCell(i, CellType.STRING).setCellValue(CABECALHO.get(i));
                sheet.setColumnWidth(i, i == 0 ? 40 * 256 : 18 * 256);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (java.io.IOException e) { throw new IllegalStateException("Não foi possível gerar modelo.", e); }
    }

    public static ErroNegocioException invalido(String mensagem) {
        return new ErroNegocioException(400, "ARQUIVO_INVALIDO", mensagem);
    }
}
