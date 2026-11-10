package nexum.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import nexum.Usuario;
import nexum.exception.EntityNotFoundException;
import nexum.exception.ValidationException;
import nexum.service.UsuarioService;
import nexum.web.AuthSessionManager;
import nexum.web.HttpHelper;
import nexum.web.JsonHelper;

/**
 * Controlador REST para o módulo de autenticação e perfil do arquiteto/escritório.
 * Gerencia registro de conta, login unificado, consulta e atualização de perfil.
 */
public class AuthController implements HttpHandler {

    private final UsuarioService usuarioService;

    public AuthController() {
        this(new UsuarioService());
    }

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (HttpHelper.handleCors(exchange)) {
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod().toUpperCase();

        try {
            if ("POST".equals(method) && path.endsWith("/register")) {
                handleRegister(exchange);
            } else if ("POST".equals(method) && path.endsWith("/login")) {
                handleLogin(exchange);
            } else if ("GET".equals(method) && path.endsWith("/perfil")) {
                handleGetPerfil(exchange);
            } else if (("PUT".equals(method) || "POST".equals(method)) && path.endsWith("/perfil")) {
                handleUpdatePerfil(exchange);
            } else if ("POST".equals(method) && path.endsWith("/logout")) {
                handleLogout(exchange);
            } else {
                HttpHelper.sendError(exchange, 404, "Rota de autenticação não encontrada: " + path);
            }
        } catch (ValidationException e) {
            HttpHelper.sendError(exchange, 400, e.getMessage());
        } catch (EntityNotFoundException e) {
            HttpHelper.sendError(exchange, 404, e.getMessage());
        } catch (Exception e) {
            HttpHelper.sendError(exchange, 500, "Erro interno de autenticação: " + e.getMessage());
        }
    }

    private void handleRegister(HttpExchange exchange) throws IOException {
        String body = HttpHelper.readRequestBody(exchange);
        Map<String, String> data = JsonHelper.parseSimpleJson(body);

        String nome = data.get("nome");
        String email = data.get("email");
        String whatsApp = data.get("whatsApp");
        if (whatsApp == null) {
            whatsApp = data.get("whatsapp");
        }
        String cnpj = data.get("cnpj");
        String senha = data.get("senha");

        Usuario novoUsuario = usuarioService.cadastrar(nome, email, whatsApp, cnpj, senha);
        String token = AuthSessionManager.criarSessao(novoUsuario.getId());

        String jsonResp = "{\"token\": \"" + token + "\", \"usuario\": " + JsonHelper.usuarioToJson(novoUsuario) + "}";
        HttpHelper.sendJson(exchange, 201, jsonResp);
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        String body = HttpHelper.readRequestBody(exchange);
        Map<String, String> data = JsonHelper.parseSimpleJson(body);

        String identificador = data.get("identificador");
        if (identificador == null || identificador.isBlank()) {
            identificador = data.get("cnpj");
        }
        if (identificador == null || identificador.isBlank()) {
            identificador = data.get("email");
        }
        String senha = data.get("senha");

        Usuario usuario = usuarioService.login(identificador, senha);
        String token = AuthSessionManager.criarSessao(usuario.getId());

        String jsonResp = "{\"token\": \"" + token + "\", \"usuario\": " + JsonHelper.usuarioToJson(usuario) + "}";
        HttpHelper.sendJson(exchange, 200, jsonResp);
    }

    private void handleGetPerfil(HttpExchange exchange) throws IOException {
        int usuarioId = autenticarOuLancar(exchange);
        Usuario usuario = usuarioService.buscarPorId(usuarioId);
        HttpHelper.sendJson(exchange, 200, JsonHelper.usuarioToJson(usuario));
    }

    private void handleUpdatePerfil(HttpExchange exchange) throws IOException {
        int usuarioId = autenticarOuLancar(exchange);
        String body = HttpHelper.readRequestBody(exchange);
        Map<String, String> data = JsonHelper.parseSimpleJson(body);

        String nome = data.get("nome");
        String email = data.get("email");
        String whatsApp = data.get("whatsApp");
        if (whatsApp == null) {
            whatsApp = data.get("whatsapp");
        }
        String novaSenha = data.get("novaSenha");

        Usuario atualizado = usuarioService.atualizarPerfil(usuarioId, nome, email, whatsApp, novaSenha);
        HttpHelper.sendJson(exchange, 200, JsonHelper.usuarioToJson(atualizado));
    }

    private void handleLogout(HttpExchange exchange) throws IOException {
        Optional<String> tokenOpt = AuthSessionManager.extrairToken(exchange);
        tokenOpt.ifPresent(AuthSessionManager::encerrarSessao);
        HttpHelper.sendJson(exchange, 200, "{\"success\": true, \"mensagem\": \"Sessão encerrada com sucesso.\"}");
    }

    private int autenticarOuLancar(HttpExchange exchange) {
        Optional<String> tokenOpt = AuthSessionManager.extrairToken(exchange);
        if (tokenOpt.isEmpty()) {
            throw new ValidationException("Acesso não autorizado: token de autenticação não fornecido.");
        }

        Optional<Integer> usuarioIdOpt = AuthSessionManager.validarSessao(tokenOpt.get());
        if (usuarioIdOpt.isEmpty()) {
            throw new ValidationException("Sessão expirada ou inválida. Por favor, efetue login novamente.");
        }

        return usuarioIdOpt.get();
    }
}
