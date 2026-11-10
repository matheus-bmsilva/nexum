package nexum.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import nexum.exception.DatabaseException;

/**
 * Fábrica de conexões JDBC com suporte a SQLite (zero custo e alta portabilidade).
 * Gerencia o ciclo de vida das conexões e ativação de restrições de chaves estrangeiras.
 */
public class ConnectionFactory {

    private static final String DEFAULT_DB_PATH = (System.getenv("DATABASE_PATH") != null && !System.getenv("DATABASE_PATH").isBlank())
            ? System.getenv("DATABASE_PATH").trim()
            : "database/nexum.db";
    private static String dbPath = DEFAULT_DB_PATH;

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new DatabaseException("Driver SQLite JDBC não encontrado no classpath.", e);
        }
    }

    private ConnectionFactory() {
        // Construtor privado para impedir instanciação direta (Padrão Utilitário / Factory)
    }

    /**
     * Permite customizar o caminho do banco de dados (útil para testes ou ambientes isolados).
     * @param newPath Caminho relativo ou absoluto do arquivo .db
     */
    public static synchronized void setDbPath(String newPath) {
        dbPath = (newPath != null && !newPath.trim().isEmpty()) ? newPath : DEFAULT_DB_PATH;
    }

    /**
     * Retorna o caminho atual do banco de dados SQLite.
     * @return Caminho do arquivo de banco
     */
    public static synchronized String getDbPath() {
        return dbPath;
    }

    /**
     * Abre e retorna uma nova conexão com o banco de dados SQLite,
     * garantindo que o diretório exista e que integridade referencial (foreign keys) esteja ativa.
     * @return Instância ativa de java.sql.Connection
     */
    public static Connection getConnection() {
        try {
            ensureDirectoryExists(dbPath);
            String url = "jdbc:sqlite:" + dbPath;
            Connection connection = DriverManager.getConnection(url);

            // Habilita suporte a chaves estrangeiras explicitamente no SQLite
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
            }

            return connection;
        } catch (SQLException e) {
            throw new DatabaseException("Falha ao abrir conexão com o banco de dados SQLite em: " + dbPath, e);
        }
    }

    private static void ensureDirectoryExists(String path) {
        File file = new File(path);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
    }
}
