package nexum.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import nexum.Orcamento;
import nexum.StatusOrcamento;
import nexum.exception.EntityNotFoundException;
import nexum.exception.ValidationException;
import nexum.service.OrcamentoService;
import nexum.web.HttpHelper;
import nexum.web.JsonHelper;

/**
 * Controlador REST para emissão e gestão de propostas orçamentárias (RF04, RF05).
 */
public class OrcamentoController implements HttpHandler {

    private final OrcamentoService orcamentoService;

    public OrcamentoController() {
        this(new OrcamentoService());
    }

    public OrcamentoController(OrcamentoService orcamentoService) {
        this.orcamentoService = orcamentoService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (HttpHelper.handleCors(exchange)) {
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod().toUpperCase();

        try {
            if ("POST".equals(method) && path.endsWith("/status")) {
                // Atualização de status: /api/orcamentos/{id}/status
                String[] parts = path.split("/");
                int id = Integer.parseInt(parts[parts.length - 2]);
                String body = HttpHelper.readRequestBody(exchange);
                Map<String, String> data = JsonHelper.parseSimpleJson(body);
                String statusStr = data.getOrDefault("status", "APROVADO");
                StatusOrcamento status = StatusOrcamento.valueOf(statusStr);

                orcamentoService.atualizarStatus(id, status);
                HttpHelper.sendJson(exchange, 200, "{\"success\": true, \"status\": \"" + status.name() + "\"}");
            } else if ("POST".equals(method)) {
                // Emissão de novo orçamento
                String body = HttpHelper.readRequestBody(exchange);
                Map<String, String> data = JsonHelper.parseSimpleJson(body);

                int clienteId = Integer.parseInt(data.get("clienteId"));
                int servicoId = Integer.parseInt(data.get("servicoId"));
                int validade = Integer.parseInt(data.getOrDefault("validade", "15"));
                String dataEmissao = data.getOrDefault("dataEmissao", java.time.LocalDate.now().toString());

                Orcamento orcamento = orcamentoService.emitirOrcamento(clienteId, servicoId, validade, dataEmissao);
                String whatsappUrl = orcamentoService.gerarLinkWhatsApp(orcamento.getId());

                HttpHelper.sendJson(exchange, 201, JsonHelper.orcamentoToJson(orcamento, whatsappUrl));
            } else if ("GET".equals(method)) {
                if (path.matches(".*/\\d+/whatsapp$")) {
                    String[] parts = path.split("/");
                    int id = Integer.parseInt(parts[parts.length - 2]);
                    String url = orcamentoService.gerarLinkWhatsApp(id);
                    HttpHelper.sendJson(exchange, 200, "{\"whatsappUrl\": \"" + HttpHelper.escapeJson(url) + "\"}");
                } else if (path.matches("^/api/orcamentos/\\d+$")) {
                    int id = Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));
                    Orcamento orcamento = orcamentoService.buscarPorId(id);
                    String whatsappUrl = orcamentoService.gerarLinkWhatsApp(id);
                    HttpHelper.sendJson(exchange, 200, JsonHelper.orcamentoToJson(orcamento, whatsappUrl));
                } else {
                    List<Orcamento> orcamentos = orcamentoService.listarTodos();
                    HttpHelper.sendJson(exchange, 200, JsonHelper.orcamentosToJson(orcamentos));
                }
            } else {
                HttpHelper.sendError(exchange, 405, "Método HTTP não permitido: " + method);
            }
        } catch (ValidationException e) {
            HttpHelper.sendError(exchange, 400, e.getMessage());
        } catch (EntityNotFoundException e) {
            HttpHelper.sendError(exchange, 404, e.getMessage());
        } catch (Exception e) {
            HttpHelper.sendError(exchange, 500, "Erro interno ao processar orçamento: " + e.getMessage());
        }
    }
}
