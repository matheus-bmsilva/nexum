package nexum.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import nexum.Cliente;
import nexum.exception.EntityNotFoundException;
import nexum.exception.ValidationException;
import nexum.service.ClienteService;
import nexum.web.HttpHelper;
import nexum.web.JsonHelper;

/**
 * Controlador REST para o ciclo de vida de Clientes (RF02).
 */
public class ClienteController implements HttpHandler {

    private final ClienteService clienteService;

    public ClienteController() {
        this(new ClienteService());
    }

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (HttpHelper.handleCors(exchange)) {
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod().toUpperCase();

        try {
            if ("GET".equals(method)) {
                if (path.matches("^/api/clientes/\\d+$")) {
                    int id = Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));
                    Cliente cliente = clienteService.buscarPorId(id);
                    HttpHelper.sendJson(exchange, 200, JsonHelper.clienteToJson(cliente));
                } else {
                    List<Cliente> clientes = clienteService.listarTodos();
                    HttpHelper.sendJson(exchange, 200, JsonHelper.clientesToJson(clientes));
                }
            } else if ("POST".equals(method)) {
                String body = HttpHelper.readRequestBody(exchange);
                Map<String, String> data = JsonHelper.parseSimpleJson(body);

                String nome = data.get("nome");
                String cpf = data.get("cpf");
                String whatsApp = data.get("whatsApp");

                Cliente novoCliente = clienteService.cadastrar(nome, cpf, whatsApp);
                HttpHelper.sendJson(exchange, 201, JsonHelper.clienteToJson(novoCliente));
            } else if ("DELETE".equals(method)) {
                if (path.matches("^/api/clientes/\\d+$")) {
                    int id = Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));
                    clienteService.deletar(id);
                    HttpHelper.sendJson(exchange, 200, "{\"success\": true}");
                } else {
                    HttpHelper.sendError(exchange, 400, "ID do cliente não informado.");
                }
            } else {
                HttpHelper.sendError(exchange, 405, "Método HTTP não permitido: " + method);
            }
        } catch (ValidationException e) {
            HttpHelper.sendError(exchange, 400, e.getMessage());
        } catch (EntityNotFoundException e) {
            HttpHelper.sendError(exchange, 404, e.getMessage());
        } catch (Exception e) {
            HttpHelper.sendError(exchange, 500, "Erro interno no servidor ao processar cliente: " + e.getMessage());
        }
    }
}
