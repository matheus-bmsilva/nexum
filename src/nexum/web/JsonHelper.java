package nexum.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import nexum.Cliente;
import nexum.Cobranca;
import nexum.Etapa;
import nexum.ItemEtapa;
import nexum.Orcamento;
import nexum.Servico;
import nexum.ServicoPorEtapa;
import nexum.ServicoPorHora;
import nexum.ServicoPorMetroQuadrado;
import nexum.ServicoPorPercentualObra;
import nexum.Usuario;
import nexum.repository.sqlite.ServicoFactory;
import nexum.service.CobrancaService;
import nexum.service.PrecificacaoService;

/**
 * Serializador e analisador JSON autônomo e de zero custo (sem dependências externas).
 * Fornece mapeamento de DTOs e entidades para a API REST.
 */
public class JsonHelper {

    private JsonHelper() {
    }

    public static Map<String, String> parseSimpleJson(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null || json.trim().isEmpty()) {
            return map;
        }

        // Padrão que captura "chave" : "valor" ou "chave" : 123.45 ou "chave" : true/false
        Pattern pattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(\"[^\"]*\"|[^,}]+)");
        Matcher matcher = pattern.matcher(json);
        while (matcher.find()) {
            String key = matcher.group(1).trim();
            String rawVal = matcher.group(2).trim();
            if (rawVal.startsWith("\"") && rawVal.endsWith("\"")) {
                rawVal = rawVal.substring(1, rawVal.length() - 1);
            }
            map.put(key, rawVal);
        }
        return map;
    }

    public static List<ItemEtapa> parseItensEtapa(String json) {
        List<ItemEtapa> itens = new ArrayList<>();
        if (json == null || !json.contains("itensEtapas")) {
            return itens;
        }

        // Extrai cada objeto dentro do array de itensEtapas: { "etapa": "...", "valor": 123 }
        Pattern itemPattern = Pattern.compile("\\{[^\\}]*\"etapa\"\\s*:\\s*\"([^\"]+)\"[^\\}]*\"valor\"\\s*:\\s*([0-9.]+)[^\\}]*\\}");
        Matcher matcher = itemPattern.matcher(json);
        while (matcher.find()) {
            try {
                Etapa etapa = Etapa.valueOf(matcher.group(1).trim());
                double valor = Double.parseDouble(matcher.group(2).trim());
                itens.add(new ItemEtapa(etapa, valor));
            } catch (Exception ignored) {
            }
        }
        return itens;
    }

    public static String clienteToJson(Cliente c) {
        if (c == null) return "null";
        return String.format(
            "{\"id\": %d, \"nome\": \"%s\", \"cpf\": \"%s\", \"whatsApp\": \"%s\"}",
            c.getId(),
            HttpHelper.escapeJson(c.getNome()),
            HttpHelper.escapeJson(c.getCpf()),
            HttpHelper.escapeJson(c.getWhatsApp())
        );
    }

    public static String clientesToJson(List<Cliente> clientes) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < clientes.size(); i++) {
            sb.append(clienteToJson(clientes.get(i)));
            if (i < clientes.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    public static String memoriaToJson(PrecificacaoService.MemoriaCalculo m) {
        if (m == null) return "{}";
        return String.format(Locale.US,
            "{\"tipoServico\": \"%s\", \"valorBase\": %.2f, \"aliquotaImposto\": %.4f, \"valorImposto\": %.2f, \"valorTotalFinal\": %.2f, \"faixaSimples\": %d, \"rbt12\": %.2f, \"parcelaADeduzir\": %.2f, \"descricaoFaixa\": \"%s\"}",
            m.tipoServico(), m.valorBase(), m.aliquotaImposto(), m.valorImposto(), m.valorTotalFinal(),
            m.faixaSimples(), m.rbt12(), m.parcelaADeduzir(), HttpHelper.escapeJson(m.descricaoFaixa() != null ? m.descricaoFaixa() : "")
        );
    }

    public static String servicoToJson(Servico s) {
        if (s == null) return "null";
        String tipo = ServicoFactory.obterTipoServico(s);
        double valorBase = s.calcularValor();
        double valorTotal = valorBase * (1.0 + s.getImposto());

        StringBuilder sb = new StringBuilder("{");
        sb.append(String.format(Locale.US, "\"id\": %d, \"tipo\": \"%s\", \"descricao\": \"%s\", \"data\": \"%s\", \"padrao\": \"%s\", \"imposto\": %.4f, \"valorBase\": %.2f, \"valorTotal\": %.2f",
            s.getId(),
            tipo,
            HttpHelper.escapeJson(s.getDescricao()),
            HttpHelper.escapeJson(s.getData()),
            s.getPadrao() != null ? s.getPadrao().name() : "MEDIO",
            s.getImposto(),
            valorBase,
            valorTotal
        ));

        if (s.getCliente() != null) {
            sb.append(", \"cliente\": ").append(clienteToJson(s.getCliente()));
        }

        if (s instanceof ServicoPorMetroQuadrado m2) {
            sb.append(String.format(Locale.US, ", \"area\": %.2f, \"valorMetroQuadrado\": %.2f", m2.getArea(), m2.getValorMetroQuadrado()));
        } else if (s instanceof ServicoPorHora hora) {
            sb.append(String.format(Locale.US, ", \"horasTrabalhadas\": %.2f, \"valorHora\": %.2f", hora.getHorasTrabalhadas(), hora.getValorHora()));
        } else if (s instanceof ServicoPorPercentualObra perc) {
            sb.append(String.format(Locale.US, ", \"custoTotalObra\": %.2f, \"percentual\": %.4f", perc.getCustoTotalObra(), perc.getPercentual()));
        } else if (s instanceof ServicoPorEtapa etapa) {
            sb.append(", \"itensEtapas\": [");
            List<ItemEtapa> itens = etapa.getItensEtapas();
            if (itens != null) {
                for (int i = 0; i < itens.size(); i++) {
                    ItemEtapa it = itens.get(i);
                    sb.append(String.format(Locale.US, "{\"etapa\": \"%s\", \"valor\": %.2f}", it.getEtapa().name(), it.getValor()));
                    if (i < itens.size() - 1) sb.append(",");
                }
            }
            sb.append("]");
        }

        sb.append("}");
        return sb.toString();
    }

    public static String servicosToJson(List<Servico> servicos) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < servicos.size(); i++) {
            sb.append(servicoToJson(servicos.get(i)));
            if (i < servicos.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    public static String orcamentoToJson(Orcamento o, String whatsappUrl) {
        if (o == null) return "null";
        StringBuilder sb = new StringBuilder("{");
        sb.append(String.format(Locale.US,
            "\"id\": %d, \"dataEmissao\": \"%s\", \"validade\": %d, \"valorTotal\": %.2f, \"statusOrcamento\": \"%s\"",
            o.getId(),
            HttpHelper.escapeJson(o.getDataEmissao()),
            o.getValidade(),
            o.getValorTotal(),
            o.getStatusOrcamento() != null ? o.getStatusOrcamento().name() : "PENDENTE"
        ));

        if (o.getCliente() != null) {
            sb.append(", \"cliente\": ").append(clienteToJson(o.getCliente()));
        }
        if (o.getServico() != null) {
            sb.append(", \"servico\": ").append(servicoToJson(o.getServico()));
        }
        if (whatsappUrl != null) {
            sb.append(", \"whatsappUrl\": \"").append(HttpHelper.escapeJson(whatsappUrl)).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    public static String orcamentosToJson(List<Orcamento> orcamentos) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < orcamentos.size(); i++) {
            sb.append(orcamentoToJson(orcamentos.get(i), null));
            if (i < orcamentos.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    public static String cobrancaToJson(Cobranca c, String whatsappUrl) {
        if (c == null) return "null";
        StringBuilder sb = new StringBuilder("{");
        sb.append(String.format(Locale.US,
            "\"id\": %d, \"valor\": %.2f, \"dataVencimento\": \"%s\", \"dataPagamento\": %s, \"statusCobranca\": \"%s\"",
            c.getId(),
            c.getValor(),
            HttpHelper.escapeJson(c.getDataVencimento()),
            c.getDataPagamento() != null ? "\"" + HttpHelper.escapeJson(c.getDataPagamento()) + "\"" : "null",
            c.getStatusCobranca() != null ? c.getStatusCobranca().name() : "EM_ABERTO"
        ));

        if (c.getOrcamento() != null) {
            sb.append(", \"orcamento\": ").append(orcamentoToJson(c.getOrcamento(), null));
        }
        if (whatsappUrl != null) {
            sb.append(", \"whatsappUrl\": \"").append(HttpHelper.escapeJson(whatsappUrl)).append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    public static String cobrancasToJson(List<Cobranca> cobrancas) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < cobrancas.size(); i++) {
            sb.append(cobrancaToJson(cobrancas.get(i), null));
            if (i < cobrancas.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    public static String extratoToJson(CobrancaService.ExtratoFinanceiro e) {
        if (e == null) return "{}";
        return String.format(Locale.US,
            "{\"totalFaturado\": %.2f, \"totalEmAberto\": %.2f, \"totalCobrancas\": %d, \"cobrancasPagas\": %d, \"cobrancasEmAberto\": %d}",
            e.totalFaturado(), e.totalEmAberto(), e.totalCobrancas(), e.cobrancasPagas(), e.cobrancasEmAberto()
        );
    }

    public static String usuarioToJson(Usuario u) {
        if (u == null) return "null";
        return String.format(Locale.US,
            "{\"id\": %d, \"nome\": \"%s\", \"email\": \"%s\", \"whatsApp\": \"%s\", \"cnpj\": \"%s\"}",
            u.getId(),
            HttpHelper.escapeJson(u.getNome()),
            HttpHelper.escapeJson(u.getEmail() != null ? u.getEmail() : ""),
            HttpHelper.escapeJson(u.getWhatsApp() != null ? u.getWhatsApp() : ""),
            HttpHelper.escapeJson(u.getCnpj() != null ? u.getCnpj() : "")
        );
    }
}
