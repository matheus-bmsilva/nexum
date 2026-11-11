package nexum.web;

import com.sun.net.httpserver.HttpExchange;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import nexum.database.ConnectionFactory;

/**
 * Gerenciador de sessões e tokens de autenticação com persistência em banco de dados SQLite
 * e cache de alta performance em memória.
 * Respeita a premissa de zero custo e garante a permanência dos logins e dados mesmo após
 * recarregamentos e reinicializações da aplicação.
 */
public class AuthSessionManager {

    private static final long DURATION_MILLIS = 30L * 24 * 60 * 60 * 1000L; // 30 dias de validade

    private static final Map<String, SessionData> memoryCache = new ConcurrentHashMap<>();

    private record SessionData(int usuarioId, long expiraEm) {
        boolean isValida() {
            return System.currentTimeMillis() < expiraEm;
        }
    }

    private AuthSessionManager() {
    }

    /**
     * Cria e registra uma nova sessão persistente para o usuário, retornando o token UUID gerado.
     */
    public static String criarSessao(int usuarioId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        long expiraEm = System.currentTimeMillis() + DURATION_MILLIS;

        // Persiste fisicamente no banco de dados (PostgreSQL / SQLite)
        String sql = "INSERT INTO sessoes (token, usuario_id, expira_em) VALUES (?, ?, ?) " +
                     "ON CONFLICT (token) DO UPDATE SET usuario_id = EXCLUDED.usuario_id, expira_em = EXCLUDED.expira_em";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, token);
            stmt.setInt(2, usuarioId);
            stmt.setLong(3, expiraEm);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Aviso: falha ao persistir sessão no banco de dados: " + e.getMessage());
        }

        memoryCache.put(token, new SessionData(usuarioId, expiraEm));
        return token;
    }

    /**
     * Valida um token de sessão, buscando no cache em memória ou no SQLite caso o servidor tenha reiniciado.
     */
    public static Optional<Integer> validarSessao(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        // 1. Verifica cache rápido
        SessionData cached = memoryCache.get(token);
        if (cached != null) {
            if (cached.isValida()) {
                return Optional.of(cached.usuarioId());
            } else {
                memoryCache.remove(token);
                removerSessaoBanco(token);
                return Optional.empty();
            }
        }

        // 2. Fallback resiliente no SQLite (caso o servidor tenha sido reiniciado)
        String sql = "SELECT usuario_id, expira_em FROM sessoes WHERE token = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, token);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int usuarioId = rs.getInt("usuario_id");
                    long expiraEm = rs.getLong("expira_em");
                    if (System.currentTimeMillis() < expiraEm) {
                        memoryCache.put(token, new SessionData(usuarioId, expiraEm));
                        return Optional.of(usuarioId);
                    } else {
                        removerSessaoBanco(token);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Aviso: falha ao verificar sessão no banco SQLite: " + e.getMessage());
        }

        return Optional.empty();
    }

    /**
     * Encerra a sessão removendo o token do cache e do banco SQLite.
     */
    public static void encerrarSessao(String token) {
        if (token != null) {
            memoryCache.remove(token);
            removerSessaoBanco(token);
        }
    }

    private static void removerSessaoBanco(String token) {
        String sql = "DELETE FROM sessoes WHERE token = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, token);
            stmt.executeUpdate();
        } catch (SQLException ignored) {
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
