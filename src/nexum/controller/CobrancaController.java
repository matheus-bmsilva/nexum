package nexum.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import nexum.Cobranca;
import nexum.exception.EntityNotFoundException;
import nexum.exception.ValidationException;
import nexum.service.CobrancaService;
import nexum.web.HttpHelper;
import nexum.web.JsonHelper;

/**
 * Controlador REST para cobranças, fluxo de recebíveis e liquidação de pagamentos (RF06, RF07).
 */
public class CobrancaController implements HttpHandler {

    private final CobrancaService cobrancaService;

    public CobrancaController() {
        this(new CobrancaService());
    }

    public CobrancaController(CobrancaService cobrancaService) {
        this.cobrancaService = cobrancaService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (HttpHelper.handleCors(exchange)) {
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod().toUpperCase();

        try {
            if ("POST".equals(method) && path.endsWith("/pagar")) {
                // Registrar pagamento: /api/cobrancas/{id}/pagar
                String[] parts = path.split("/");
                int id = Integer.parseInt(parts[parts.length - 2]);
                String body = HttpHelper.readRequestBody(exchange);
                Map<String, String> data = JsonHelper.parseSimpleJson(body);
                String dataPagamento = data.get("dataPagamento");

                cobrancaService.registrarPagamento(id, dataPagamento);
                HttpHelper.sendJson(exchange, 200, "{\"success\": true, \"mensagem\": \"Pagamento registrado com sucesso!\"}");
            } else if ("POST".equals(method)) {
                // Gerar cobrança
                String body = HttpHelper.readRequestBody(exchange);
                Map<String, String> data = JsonHelper.parseSimpleJson(body);

                int orcamentoId = Integer.parseInt(data.get("orcamentoId"));
                String dataVencimento = data.get("dataVencimento");

                Cobranca cobranca = cobrancaService.gerarCobrancaParaOrcamento(orcamentoId, dataVencimento);
                String whatsappUrl = cobrancaService.gerarLinkWhatsAppCobranca(cobranca.getId());

                HttpHelper.sendJson(exchange, 201, JsonHelper.cobrancaToJson(cobranca, whatsappUrl));
            } else if ("GET".equals(method)) {
                if (path.endsWith("/extrato")) {
                    CobrancaService.ExtratoFinanceiro extrato = cobrancaService.obterExtratoFinanceiro();
                    HttpHelper.sendJson(exchange, 200, JsonHelper.extratoToJson(extrato));
                } else if (path.matches(".*/\\d+/whatsapp$")) {
                    String[] parts = path.split("/");
                    int id = Integer.parseInt(parts[parts.length - 2]);
                    String url = cobrancaService.gerarLinkWhatsAppCobranca(id);
                    HttpHelper.sendJson(exchange, 200, "{\"whatsappUrl\": \"" + HttpHelper.escapeJson(url) + "\"}");
                } else if (path.matches("^/api/cobrancas/\\d+$")) {
                    int id = Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));
                    Cobranca cobranca = cobrancaService.buscarPorId(id);
                    String whatsappUrl = cobrancaService.gerarLinkWhatsAppCobranca(id);
                    HttpHelper.sendJson(exchange, 200, JsonHelper.cobrancaToJson(cobranca, whatsappUrl));
                } else {
                    List<Cobranca> cobrancas = cobrancaService.listarTodas();
                    HttpHelper.sendJson(exchange, 200, JsonHelper.cobrancasToJson(cobrancas));
                }
            } else if ("DELETE".equals(method)) {
                if (path.matches("^/api/cobrancas/\\d+$")) {
                    int id = Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));
                    cobrancaService.deletar(id);
                    HttpHelper.sendJson(exchange, 200, "{\"success\": true, \"mensagem\": \"Cobrança excluída com sucesso!\"}");
                } else {
                    HttpHelper.sendError(exchange, 400, "ID da cobrança não informado.");
                }
            } else {
                HttpHelper.sendError(exchange, 405, "Método HTTP não permitido: " + method);
            }
        } catch (ValidationException e) {
            HttpHelper.sendError(exchange, 400, e.getMessage());
        } catch (EntityNotFoundException e) {
            HttpHelper.sendError(exchange, 404, e.getMessage());
        } catch (Exception e) {
            HttpHelper.sendError(exchange, 500, "Erro interno ao processar cobrança: " + e.getMessage());
        }
    }
}
