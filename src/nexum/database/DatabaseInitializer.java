package nexum.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import nexum.exception.DatabaseException;

/**
 * Inicializador de esquema do banco de dados relacional (DDL).
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
        String[] ddlStatements = {
            // Tabela de usuários (profissionais/arquitetos)
            """
            CREATE TABLE IF NOT EXISTS usuarios (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nome TEXT NOT NULL,
                senha TEXT NOT NULL,
                whatsapp TEXT NOT NULL,
                cnpj TEXT NOT NULL UNIQUE
            )
            """,

            // Tabela de clientes
            """
            CREATE TABLE IF NOT EXISTS clientes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nome TEXT NOT NULL,
                cpf TEXT NOT NULL UNIQUE,
                whatsapp TEXT NOT NULL
            )
            """,

            // Tabela polimórfica de serviços (Single Table Inheritance)
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

            // Tabela de itens das etapas (para ServicoPorEtapa)
            """
            CREATE TABLE IF NOT EXISTS itens_etapa (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                servico_id INTEGER NOT NULL,
                etapa TEXT NOT NULL,
                valor REAL NOT NULL,
                FOREIGN KEY (servico_id) REFERENCES servicos(id) ON DELETE CASCADE
            )
            """,

            // Tabela de orçamentos
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

            // Tabela de cobranças
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

            // Índices para otimização de consultas e unicidade
            "CREATE INDEX IF NOT EXISTS idx_clientes_cpf ON clientes(cpf)",
            "CREATE INDEX IF NOT EXISTS idx_usuarios_cnpj ON usuarios(cnpj)",
            "CREATE INDEX IF NOT EXISTS idx_servicos_cliente ON servicos(cliente_id)",
            "CREATE INDEX IF NOT EXISTS idx_itens_etapa_servico ON itens_etapa(servico_id)",
            "CREATE INDEX IF NOT EXISTS idx_orcamentos_cliente ON orcamentos(cliente_id)",
            "CREATE INDEX IF NOT EXISTS idx_cobrancas_orcamento ON cobrancas(orcamento_id)"
        };

        try (Connection conn = ConnectionFactory.getConnection();
             Statement stmt = conn.createStatement()) {

            for (String sql : ddlStatements) {
                stmt.execute(sql.trim());
            }

        } catch (SQLException e) {
            throw new DatabaseException("Falha ao inicializar o esquema DDL do banco de dados.", e);
        }
    }
}
