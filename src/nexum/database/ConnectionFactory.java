package nexum.database;

import java.io.File;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import nexum.exception.DatabaseException;

/**
 * Fábrica de conexões JDBC compatível com PostgreSQL (Supabase) e SQLite.
 * Lê dinamicamente as credenciais e URL via variáveis de ambiente (DB_URL, DB_USER, DB_PASSWORD).
 * Remove a dependência estrita do SQLite local, permitindo deploy em nuvem (Render, Supabase, Railway)
 * com PostgreSQL nativo ou fallback resiliente para SQLite local caso nenhuma variável seja configurada.
 */
public class ConnectionFactory {

    private static String customUrl = null;
    private static String customUser = null;
    private static String customPassword = null;

    static {
        // Registra os drivers JDBC disponíveis
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            // Driver PostgreSQL não encontrado no classpath
        }
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            // Driver SQLite não encontrado no classpath
        }
    }

    private ConnectionFactory() {
        // Construtor privado para impedir instanciação direta
    }

    public record ConnectionConfig(String url, String user, String password) {}

    /**
     * Permite configurar manualmente a URL e credenciais de conexão (útil para testes ou cenários customizados).
     */
    public static synchronized void setConnectionConfig(String url, String user, String password) {
        customUrl = url;
        customUser = user;
        customPassword = password;
    }

    /**
     * Define o caminho do banco SQLite local (mantido para compatibilidade com testes automatizados).
     */
    public static synchronized void setDbPath(String newPath) {
        if (newPath == null || newPath.isBlank()) {
            customUrl = null;
        } else {
            customUrl = "jdbc:sqlite:" + normalizePath(newPath);
        }
        customUser = null;
        customPassword = null;
    }

    /**
     * Retorna o caminho ou URL atual da conexão.
     */
    public static synchronized String getDbPath() {
        ConnectionConfig config = getEffectiveConfig();
        return config.url();
    }

    /**
     * Obtém as configurações efetivas de conexão, avaliando configurações manuais,
     * variáveis de ambiente do sistema e fallback para SQLite.
     */
    public static ConnectionConfig getEffectiveConfig() {
        if (customUrl != null && !customUrl.isBlank()) {
            return new ConnectionConfig(customUrl, customUser, customPassword);
        }

        // 1. Variáveis de ambiente principais
        String envUrl = System.getenv("DB_URL");
        if (envUrl == null || envUrl.isBlank()) {
            envUrl = System.getenv("DATABASE_URL");
        }

        String envUser = System.getenv("DB_USER");
        String envPassword = System.getenv("DB_PASSWORD");

        if (envUrl != null && !envUrl.isBlank()) {
            String trimmed = envUrl.trim();
            // Trata URLs no formato postgres://user:pass@host:port/dbname ou postgresql:// (padrão Supabase/Render/Railway)
            if (trimmed.startsWith("postgres://") || (trimmed.startsWith("postgresql://") && !trimmed.startsWith("jdbc:"))) {
                return parsePostgreSqlUri(trimmed, envUser, envPassword);
            }
            if (trimmed.startsWith("jdbc:postgres://")) {
                trimmed = "jdbc:postgresql://" + trimmed.substring("jdbc:postgres://".length());
            }
            if (trimmed.startsWith("jdbc:postgresql://") && !trimmed.contains("sslmode=")
                    && !trimmed.contains("localhost") && !trimmed.contains("127.0.0.1")) {
                trimmed += (trimmed.contains("?") ? "&" : "?") + "sslmode=require";
            }
            return new ConnectionConfig(trimmed, envUser, envPassword);
        }

        // 2. Fallback para SQLite local caso não haja configuração PostgreSQL
        String defaultSqlitePath = System.getenv("DATABASE_PATH");
        if (defaultSqlitePath == null || defaultSqlitePath.isBlank()) {
            defaultSqlitePath = "database/nexum.db";
        }
        return new ConnectionConfig("jdbc:sqlite:" + normalizePath(defaultSqlitePath), null, null);
    }

    /**
     * Verifica se a conexão atual é com um banco PostgreSQL.
     */
    public static boolean isPostgres() {
        ConnectionConfig config = getEffectiveConfig();
        return config.url() != null && config.url().toLowerCase().contains("postgres");
    }

    /**
     * Abre e retorna uma nova conexão com o banco de dados configurado (PostgreSQL ou SQLite).
     */
    public static Connection getConnection() {
        ConnectionConfig config = getEffectiveConfig();
        try {
            Connection connection;
            if (config.url().startsWith("jdbc:sqlite:")) {
                String path = config.url().substring("jdbc:sqlite:".length());
                ensureDirectoryExists(path);
                connection = DriverManager.getConnection(config.url());
                connection.setAutoCommit(true);
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("PRAGMA foreign_keys = ON;");
                    stmt.execute("PRAGMA journal_mode = WAL;");
                    stmt.execute("PRAGMA synchronous = NORMAL;");
                    stmt.execute("PRAGMA busy_timeout = 5000;");
                }
            } else {
                if (config.user() != null && !config.user().isBlank()) {
                    connection = DriverManager.getConnection(
                        config.url(), config.user(), config.password() != null ? config.password() : ""
                    );
                } else {
                    connection = DriverManager.getConnection(config.url());
                }
                connection.setAutoCommit(true);
            }
            return connection;
        } catch (SQLException e) {
            throw new DatabaseException("Falha ao abrir conexão com o banco de dados: " + mascararUrl(config.url()), e);
        }
    }

    private static ConnectionConfig parsePostgreSqlUri(String rawUri, String defaultUser, String defaultPassword) {
        try {
            URI uri = new URI(rawUri);
            String userInfo = uri.getUserInfo();
            String user = defaultUser;
            String password = defaultPassword;

            if (userInfo != null && !userInfo.isBlank()) {
                String[] parts = userInfo.split(":", 2);
                if (user == null || user.isBlank()) {
                    user = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
                }
                if ((password == null || password.isBlank()) && parts.length > 1) {
                    password = URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
                }
            }

            int port = uri.getPort() > 0 ? uri.getPort() : 5432;
            String path = uri.getPath();
            if (path != null && path.startsWith("/")) {
                path = path.substring(1);
            }

            String query = uri.getQuery();
            String host = uri.getHost() != null ? uri.getHost() : "localhost";
            String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + path;

            boolean isLocal = host.equalsIgnoreCase("localhost") || host.equals("127.0.0.1");
            if (query != null && !query.isBlank()) {
                jdbcUrl += "?" + query;
                if (!query.contains("sslmode=") && !isLocal) {
                    jdbcUrl += "&sslmode=require";
                }
            } else if (!isLocal) {
                jdbcUrl += "?sslmode=require";
            }

            return new ConnectionConfig(jdbcUrl, user, password);
        } catch (Exception e) {
            // Em caso de erro de parsing de URI, tenta prefixar jdbc:
            String jdbcUrl = rawUri.replaceFirst("^postgres://", "jdbc:postgresql://");
            return new ConnectionConfig(jdbcUrl, defaultUser, defaultPassword);
        }
    }

    private static String mascararUrl(String url) {
        if (url == null) return "null";
        // Oculta senha se presente no formato :senha@
        return url.replaceAll(":[^:@/]+@", ":***@");
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            path = "database/nexum.db";
        }
        File f = new File(path.trim());
        return f.getAbsolutePath();
    }

    private static void ensureDirectoryExists(String path) {
        File file = new File(path);
        File parent = file.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
    }
}
