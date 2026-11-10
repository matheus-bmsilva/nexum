package nexum.web;

import com.sun.net.httpserver.HttpExchange;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerenciador de sessões e tokens de autenticação em memória.
 * Respeita a premissa de zero custo de infraestrutura e dispensa bibliotecas pesadas de terceiros.
 */
public class AuthSessionManager {

    private static final long DURATION_MILLIS = 24 * 60 * 60 * 1000L; // 24 horas

    private static final Map<String, SessionData> sessions = new ConcurrentHashMap<>();

    private record SessionData(int usuarioId, long expiraEm) {
        boolean isValida() {
            return System.currentTimeMillis() < expiraEm;
        }
    }

    private AuthSessionManager() {
    }

    /**
     * Cria e registra uma nova sessão para o usuário, retornando o token UUID gerado.
     */
    public static String criarSessao(int usuarioId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        long expiraEm = System.currentTimeMillis() + DURATION_MILLIS;
        sessions.put(token, new SessionData(usuarioId, expiraEm));
        return token;
    }

    /**
     * Valida um token de sessão e retorna o ID do usuário associado se válido.
     */
    public static Optional<Integer> validarSessao(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        SessionData session = sessions.get(token);
        if (session == null) {
            return Optional.empty();
        }

        if (!session.isValida()) {
            sessions.remove(token);
            return Optional.empty();
        }

        return Optional.of(session.usuarioId());
    }

    /**
     * Encerra a sessão removendo o token informado.
     */
    public static void encerrarSessao(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }

    /**
     * Extrai o token de autenticação dos cabeçalhos HTTP (Authorization: Bearer <token> ou X-Auth-Token).
     */
    public static Optional<String> extrairToken(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.toLowerCase().startsWith("bearer ")) {
            return Optional.of(authHeader.substring(7).trim());
        }

        String xToken = exchange.getRequestHeaders().getFirst("X-Auth-Token");
        if (xToken != null && !xToken.isBlank()) {
            return Optional.of(xToken.trim());
        }

        return Optional.empty();
    }
}
