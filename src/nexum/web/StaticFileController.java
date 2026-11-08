package nexum.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Manipulador de arquivos estáticos da interface web (HTML, CSS, JS).
 * Entrega os recursos diretamente do diretório 'web/' com os Content-Types adequados.
 */
public class StaticFileController implements HttpHandler {

    private final String baseDir;

    public StaticFileController() {
        this("web");
    }

    public StaticFileController(String baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.equals("/") || path.isBlank()) {
            path = "/index.html";
        }

        // Previne Directory Traversal
        if (path.contains("..")) {
            HttpHelper.sendError(exchange, 403, "Acesso negado.");
            return;
        }

        File file = new File(baseDir + path);
        if (!file.exists() || file.isDirectory()) {
            HttpHelper.sendError(exchange, 404, "Arquivo não encontrado: " + path);
            return;
        }

        byte[] content = java.nio.file.Files.readAllBytes(file.toPath());
        String mimeType = getMimeType(file.getName());
        exchange.getResponseHeaders().set("Content-Type", mimeType);
        exchange.sendResponseHeaders(200, content.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(content);
        }
    }

    private String getMimeType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html; charset=UTF-8";
        if (lower.endsWith(".css")) return "text/css; charset=UTF-8";
        if (lower.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (lower.endsWith(".json")) return "application/json; charset=UTF-8";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".ico")) return "image/x-icon";
        return "application/octet-stream";
    }
}
