package nexum.web;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import nexum.controller.ClienteController;
import nexum.controller.CobrancaController;
import nexum.controller.OrcamentoController;
import nexum.controller.PrecificacaoController;

/**
 * Servidor Web nativo do JDK (zero custo de infraestrutura).
 * Gerencia o ciclo de vida e o roteamento de controladores HTTP.
 */
public class NexumServer {

    private static HttpServer server;
    private static java.util.concurrent.ExecutorService executor;

    private NexumServer() {
    }

    public static synchronized void start(int port) throws IOException {
        if (server != null) {
            return;
        }

        server = HttpServer.create(new InetSocketAddress(port), 0);

        // Registro de rotas e controladores REST da API
        server.createContext("/api/clientes", new ClienteController());
        server.createContext("/api/servicos", new PrecificacaoController());
        server.createContext("/api/orcamentos", new OrcamentoController());
        server.createContext("/api/cobrancas", new CobrancaController());

        // Manipulador de recursos estáticos do Frontend (HTML, CSS, JS)
        server.createContext("/", new StaticFileController());

        // Executor com pool de threads para alta concorrência e respostas instantâneas (< 2s - RNF11)
        executor = Executors.newCachedThreadPool();
        server.setExecutor(executor);
        server.start();

        System.out.println("==================================================");
        System.out.println("  NEXUM WEB SERVER ATIVO                          ");
        System.out.println("  Porta: " + port);
        System.out.println("  URL:   http://localhost:" + port);
        System.out.println("==================================================");
    }

    public static synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        System.out.println("Servidor Nexum finalizado.");
    }
}
