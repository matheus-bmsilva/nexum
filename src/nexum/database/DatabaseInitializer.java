package nexum.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import nexum.exception.DatabaseException;

/**
 * Inicializador de esquema do banco de dados relacional (DDL) compatível com PostgreSQL e SQLite.
 * Garante a criação idempotente de todas as tabelas, índices e restrições referenciais.
 */
public class DatabaseInitializer {

    private DatabaseInitializer() {
        // Construtor privado para impedir instanciação
    }

    /**
     * Executa a criação das tabelas e índices necessários caso ainda não existam.
     */
    public static void initialize() {
        try (Connection conn = ConnectionFactory.getConnection();
             Statement stmt = conn.createStatement()) {

            boolean isPostgres = false;
            try {
                String prod = conn.getMetaData().getDatabaseProductName();
                if (prod != null && prod.toLowerCase().contains("postgres")) {
                    isPostgres = true;
                }
            } catch (SQLException e) {
                isPostgres = ConnectionFactory.isPostgres();
            }

            String[] ddlStatements = isPostgres ? getPostgresDdl() : getSqliteDdl();

            for (String sql : ddlStatements) {
                stmt.execute(sql.trim());
            }

            // Migração defensiva: garante existência da coluna email em bancos pré-existentes
            try {
                if (isPostgres) {
                    stmt.execute("ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS email VARCHAR(255);");
                } else {
                    stmt.execute("ALTER TABLE usuarios ADD COLUMN email TEXT;");
                }
            } catch (SQLException ignored) {
                // Coluna já existe
            }

            // Migração defensiva: garante existência da coluna rbt12 em bancos pré-existentes
            try {
                if (isPostgres) {
                    stmt.execute("ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS rbt12 DOUBLE PRECISION DEFAULT 0.0;");
                } else {
                    stmt.execute("ALTER TABLE usuarios ADD COLUMN rbt12 REAL DEFAULT 0.0;");
                }
            } catch (SQLException ignored) {
                // Coluna já existe
            }

        } catch (SQLException e) {
            throw new DatabaseException("Falha ao inicializar o esquema DDL do banco de dados.", e);
        }
    }

    private static String[] getPostgresDdl() {
        return new String[] {
            """
            CREATE TABLE IF NOT EXISTS usuarios (
                id SERIAL PRIMARY KEY,
                nome VARCHAR(255) NOT NULL,
                email VARCHAR(255),
                senha TEXT NOT NULL,
                whatsapp VARCHAR(50) NOT NULL,
                cnpj VARCHAR(20) NOT NULL UNIQUE,
                rbt12 DOUBLE PRECISION NOT NULL DEFAULT 0.0
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS clientes (
                id SERIAL PRIMARY KEY,
                nome VARCHAR(255) NOT NULL,
                cpf VARCHAR(20) NOT NULL UNIQUE,
                whatsapp VARCHAR(50) NOT NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS servicos (
                id SERIAL PRIMARY KEY,
                tipo_servico VARCHAR(50) NOT NULL,
                descricao TEXT NOT NULL,
                data VARCHAR(50) NOT NULL,
                padrao VARCHAR(50) NOT NULL,
                cliente_id INTEGER NOT NULL REFERENCES clientes(id) ON DELETE RESTRICT,
                imposto DOUBLE PRECISION NOT NULL DEFAULT 0.06,
                area DOUBLE PRECISION,
                valor_metro_quadrado DOUBLE PRECISION,
                horas_trabalhadas DOUBLE PRECISION,
                valor_hora DOUBLE PRECISION,
                custo_total_obra DOUBLE PRECISION,
                percentual DOUBLE PRECISION
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS itens_etapa (
                id SERIAL PRIMARY KEY,
                servico_id INTEGER NOT NULL REFERENCES servicos(id) ON DELETE CASCADE,
                etapa VARCHAR(50) NOT NULL,
                valor DOUBLE PRECISION NOT NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS orcamentos (
                id SERIAL PRIMARY KEY,
                data_emissao VARCHAR(50) NOT NULL,
                validade INTEGER NOT NULL,
                valor_total DOUBLE PRECISION NOT NULL,
                status_orcamento VARCHAR(50) NOT NULL,
                servico_id INTEGER NOT NULL REFERENCES servicos(id) ON DELETE RESTRICT,
                cliente_id INTEGER NOT NULL REFERENCES clientes(id) ON DELETE RESTRICT
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS cobrancas (
                id SERIAL PRIMARY KEY,
                valor DOUBLE PRECISION NOT NULL,
                data_vencimento VARCHAR(50) NOT NULL,
                data_pagamento VARCHAR(50),
                status_cobranca VARCHAR(50) NOT NULL,
                orcamento_id INTEGER NOT NULL REFERENCES orcamentos(id) ON DELETE RESTRICT
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS sessoes (
                token VARCHAR(255) PRIMARY KEY,
                usuario_id INTEGER NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
                expira_em BIGINT NOT NULL
            )
            """,
            "CREATE INDEX IF NOT EXISTS idx_clientes_cpf ON clientes(cpf)",
            "CREATE INDEX IF NOT EXISTS idx_usuarios_cnpj ON usuarios(cnpj)",
            "CREATE INDEX IF NOT EXISTS idx_usuarios_email ON usuarios(email)",
            "CREATE INDEX IF NOT EXISTS idx_servicos_cliente ON servicos(cliente_id)",
            "CREATE INDEX IF NOT EXISTS idx_itens_etapa_servico ON itens_etapa(servico_id)",
            "CREATE INDEX IF NOT EXISTS idx_orcamentos_cliente ON orcamentos(cliente_id)",
            "CREATE INDEX IF NOT EXISTS idx_cobrancas_orcamento ON cobrancas(orcamento_id)",
            "CREATE INDEX IF NOT EXISTS idx_sessoes_token ON sessoes(token)",
            "CREATE INDEX IF NOT EXISTS idx_sessoes_usuario ON sessoes(usuario_id)"
        };
    }

    private static String[] getSqliteDdl() {
        return new String[] {
            """
            CREATE TABLE IF NOT EXISTS usuarios (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nome TEXT NOT NULL,
                email TEXT,
                senha TEXT NOT NULL,
                whatsapp TEXT NOT NULL,
                cnpj TEXT NOT NULL UNIQUE,
                rbt12 REAL NOT NULL DEFAULT 0.0
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS clientes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nome TEXT NOT NULL,
                cpf TEXT NOT NULL UNIQUE,
                whatsapp TEXT NOT NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS servicos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                tipo_servico TEXT NOT NULL,
                descricao TEXT NOT NULL,
                data TEXT NOT NULL,
                padrao TEXT NOT NULL,
                cliente_id INTEGER NOT NULL,
                imposto REAL NOT NULL DEFAULT 0.06,
                area REAL,
                valor_metro_quadrado REAL,
                horas_trabalhadas REAL,
                valor_hora REAL,
                custo_total_obra REAL,
                percentual REAL,
                FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE RESTRICT
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS itens_etapa (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                servico_id INTEGER NOT NULL,
                etapa TEXT NOT NULL,
                valor REAL NOT NULL,
                FOREIGN KEY (servico_id) REFERENCES servicos(id) ON DELETE CASCADE
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS orcamentos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                data_emissao TEXT NOT NULL,
                validade INTEGER NOT NULL,
                valor_total REAL NOT NULL,
                status_orcamento TEXT NOT NULL,
                servico_id INTEGER NOT NULL,
                cliente_id INTEGER NOT NULL,
                FOREIGN KEY (servico_id) REFERENCES servicos(id) ON DELETE RESTRICT,
                FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE RESTRICT
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS cobrancas (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                valor REAL NOT NULL,
                data_vencimento TEXT NOT NULL,
                data_pagamento TEXT,
                status_cobranca TEXT NOT NULL,
                orcamento_id INTEGER NOT NULL,
                FOREIGN KEY (orcamento_id) REFERENCES orcamentos(id) ON DELETE RESTRICT
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS sessoes (
                token TEXT PRIMARY KEY,
                usuario_id INTEGER NOT NULL,
                expira_em INTEGER NOT NULL,
                FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
            )
            """,
            "CREATE INDEX IF NOT EXISTS idx_clientes_cpf ON clientes(cpf)",
            "CREATE INDEX IF NOT EXISTS idx_usuarios_cnpj ON usuarios(cnpj)",
            "CREATE INDEX IF NOT EXISTS idx_usuarios_email ON usuarios(email)",
            "CREATE INDEX IF NOT EXISTS idx_servicos_cliente ON servicos(cliente_id)",
            "CREATE INDEX IF NOT EXISTS idx_itens_etapa_servico ON itens_etapa(servico_id)",
            "CREATE INDEX IF NOT EXISTS idx_orcamentos_cliente ON orcamentos(cliente_id)",
            "CREATE INDEX IF NOT EXISTS idx_cobrancas_orcamento ON cobrancas(orcamento_id)",
            "CREATE INDEX IF NOT EXISTS idx_sessoes_token ON sessoes(token)",
            "CREATE INDEX IF NOT EXISTS idx_sessoes_usuario ON sessoes(usuario_id)"
        };
    }
}
