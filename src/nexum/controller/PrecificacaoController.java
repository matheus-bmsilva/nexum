package nexum.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import nexum.ItemEtapa;
import nexum.Padrao;
import nexum.Servico;
import nexum.ServicoPorEtapa;
import nexum.ServicoPorHora;
import nexum.ServicoPorMetroQuadrado;
import nexum.ServicoPorPercentualObra;
import nexum.exception.EntityNotFoundException;
import nexum.exception.ValidationException;
import nexum.repository.sqlite.ServicoFactory;
import nexum.service.PrecificacaoService;
import nexum.web.HttpHelper;
import nexum.web.JsonHelper;

/**
 * Controlador REST para cálculo e precificação polimórfica de serviços arquitetônicos (RF03).
 */
public class PrecificacaoController implements HttpHandler {

    private final PrecificacaoService precificacaoService;

    public PrecificacaoController() {
        this(new PrecificacaoService());
    }

    public PrecificacaoController(PrecificacaoService precificacaoService) {
        this.precificacaoService = precificacaoService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (HttpHelper.handleCors(exchange)) {
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod().toUpperCase();

        try {
            if ("POST".equals(method) && path.endsWith("/calcular")) {
                // Simulação de cálculo em tempo real sem persistência
                processarCalculo(exchange);
            } else if ("POST".equals(method)) {
                // Criação e persistência de serviço
                processarCriacaoServico(exchange);
            } else if ("GET".equals(method)) {
                if (path.matches(".*/cliente/\\d+$")) {
                    int clienteId = Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));
                    List<Servico> servicos = precificacaoService.listarPorCliente(clienteId);
                    HttpHelper.sendJson(exchange, 200, JsonHelper.servicosToJson(servicos));
                } else if (path.matches(".*/\\d+$")) {
                    int id = Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));
                    Servico servico = precificacaoService.buscarPorId(id);
                    HttpHelper.sendJson(exchange, 200, JsonHelper.servicoToJson(servico));
                } else {
                    List<Servico> servicos = precificacaoService.listarTodos();
                    HttpHelper.sendJson(exchange, 200, JsonHelper.servicosToJson(servicos));
                }
            } else {
                HttpHelper.sendError(exchange, 405, "Método HTTP não permitido: " + method);
            }
        } catch (ValidationException e) {
            HttpHelper.sendError(exchange, 400, e.getMessage());
        } catch (EntityNotFoundException e) {
            HttpHelper.sendError(exchange, 404, e.getMessage());
        } catch (Exception e) {
            HttpHelper.sendError(exchange, 500, "Erro interno ao processar serviço: " + e.getMessage());
        }
    }

    private void processarCalculo(HttpExchange exchange) throws IOException {
        String body = HttpHelper.readRequestBody(exchange);
        Map<String, String> data = JsonHelper.parseSimpleJson(body);

        String tipo = data.getOrDefault("tipo", ServicoFactory.TIPO_METRO_QUADRADO);
        double rbt12 = parseDouble(data.get("rbt12"), 0.0);
        double imposto = parseDouble(data.get("imposto"), PrecificacaoService.IMPOSTO_PADRAO_MEI);

        Servico servicoTransient = switch (tipo) {
            case ServicoFactory.TIPO_METRO_QUADRADO -> {
                double area = parseDouble(data.get("area"), 0.0);
                double valorM2 = parseDouble(data.get("valorMetroQuadrado"), 0.0);
                yield new ServicoPorMetroQuadrado(0, "Simulação", "", Padrao.MEDIO, null, imposto, area, valorM2);
            }
            case ServicoFactory.TIPO_ETAPA -> {
                List<ItemEtapa> itens = JsonHelper.parseItensEtapa(body);
                yield new ServicoPorEtapa(0, "Simulação", "", Padrao.MEDIO, null, imposto, itens);
            }
            case ServicoFactory.TIPO_HORA -> {
                double horas = parseDouble(data.get("horasTrabalhadas"), 0.0);
                double valorHora = parseDouble(data.get("valorHora"), 0.0);
                yield new ServicoPorHora(0, "Simulação", "", Padrao.MEDIO, null, imposto, horas, valorHora);
            }
            case ServicoFactory.TIPO_PERCENTUAL_OBRA -> {
                double custo = parseDouble(data.get("custoTotalObra"), 0.0);
                double perc = parseDouble(data.get("percentual"), 0.0);
                yield new ServicoPorPercentualObra(0, "Simulação", "", Padrao.MEDIO, null, imposto, custo, perc);
            }
            default -> throw new ValidationException("Tipo de serviço inválido para cálculo: " + tipo);
        };

        PrecificacaoService.MemoriaCalculo memoria = precificacaoService.calcularMemoria(servicoTransient, rbt12);
        HttpHelper.sendJson(exchange, 200, JsonHelper.memoriaToJson(memoria));
    }

    private void processarCriacaoServico(HttpExchange exchange) throws IOException {
        String body = HttpHelper.readRequestBody(exchange);
        Map<String, String> data = JsonHelper.parseSimpleJson(body);

        int clienteId = parseInt(data.get("clienteId"), 0);
        String tipo = data.getOrDefault("tipo", ServicoFactory.TIPO_METRO_QUADRADO);
        String descricao = data.get("descricao");
        String dataStr = data.get("data");
        String padraoStr = data.getOrDefault("padrao", "MEDIO");
        Padrao padrao;
        try {
            padrao = Padrao.valueOf(padraoStr);
        } catch (Exception e) {
            padrao = Padrao.MEDIO;
        }
        double rbt12 = parseDouble(data.get("rbt12"), 0.0);
        double imposto = parseDouble(data.get("imposto"), 0.0);
        if (rbt12 > 0) {
            imposto = precificacaoService.calcularAliquotaSimples(rbt12);
        } else if (imposto <= 0) {
            imposto = PrecificacaoService.IMPOSTO_PADRAO_MEI;
        }

        Servico salvo = switch (tipo) {
            case ServicoFactory.TIPO_METRO_QUADRADO -> {
                double area = parseDouble(data.get("area"), 0.0);
                double valorM2 = parseDouble(data.get("valorMetroQuadrado"), 0.0);
                yield precificacaoService.criarServicoMetroQuadrado(clienteId, descricao, dataStr, padrao, imposto, area, valorM2);
            }
            case ServicoFactory.TIPO_ETAPA -> {
                List<ItemEtapa> itens = JsonHelper.parseItensEtapa(body);
                yield precificacaoService.criarServicoEtapa(clienteId, descricao, dataStr, padrao, imposto, itens);
            }
            case ServicoFactory.TIPO_HORA -> {
                double horas = parseDouble(data.get("horasTrabalhadas"), 0.0);
                double valorHora = parseDouble(data.get("valorHora"), 0.0);
                yield precificacaoService.criarServicoHora(clienteId, descricao, dataStr, padrao, imposto, horas, valorHora);
            }
            case ServicoFactory.TIPO_PERCENTUAL_OBRA -> {
                double custo = parseDouble(data.get("custoTotalObra"), 0.0);
                double perc = parseDouble(data.get("percentual"), 0.0);
                yield precificacaoService.criarServicoPercentualObra(clienteId, descricao, dataStr, padrao, imposto, custo, perc);
            }
            default -> throw new ValidationException("Tipo de serviço não suportado: " + tipo);
        };

        HttpHelper.sendJson(exchange, 201, JsonHelper.servicoToJson(salvo));
    }

    private static double parseDouble(String str, double def) {
        if (str == null || str.isBlank()) return def;
        try {
            return Double.parseDouble(str.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static int parseInt(String str, int def) {
        if (str == null || str.isBlank()) return def;
        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
