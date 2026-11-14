package nexum.repository.sqlite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import nexum.Cliente;
import nexum.database.ConnectionFactory;
import nexum.exception.DatabaseException;
import nexum.exception.ValidationException;
import nexum.repository.ClienteRepository;

/**
 * Implementação SQLite do repositório de clientes com consultas parametrizadas (PreparedStatement)
 * para segurança contra SQL Injection e tratamento seguro de conexões.
 */
public class SqliteClienteRepository implements ClienteRepository {

    @Override
    public Cliente salvar(Cliente cliente) {
        String sql = "INSERT INTO clientes (nome, cpf, whatsapp) VALUES (?, ?, ?)";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, cliente.getNome());
            stmt.setString(2, cliente.getCpf());
            stmt.setString(3, cliente.getWhatsApp());

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    cliente.setId(rs.getInt(1));
                }
            }
            return cliente;
        } catch (SQLException e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            String state = e.getSQLState() != null ? e.getSQLState() : "";
            if ("23505".equals(state) || msg.contains("unique") || msg.contains("duplicate key")) {
                throw new ValidationException("Violação de unicidade: o CPF informado já está cadastrado no sistema.", e);
            }
            throw new DatabaseException("Erro ao salvar cliente no banco de dados.", e);
        }
    }

    @Override
    public Optional<Cliente> buscarPorId(int id) {
        String sql = "SELECT id, nome, cpf, whatsapp FROM clientes WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapearCliente(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao buscar cliente por ID.", e);
        }
    }

    @Override
    public Optional<Cliente> buscarPorCpf(String cpf) {
        String sql = "SELECT id, nome, cpf, whatsapp FROM clientes WHERE cpf = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, cpf);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapearCliente(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao buscar cliente por CPF.", e);
        }
    }

    @Override
    public List<Cliente> listarTodos() {
        String sql = "SELECT id, nome, cpf, whatsapp FROM clientes ORDER BY nome ASC";
        List<Cliente> clientes = new ArrayList<>();
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                clientes.add(mapearCliente(rs));
            }
            return clientes;
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao listar todos os clientes.", e);
        }
    }

    @Override
    public void atualizar(Cliente cliente) {
        String sql = "UPDATE clientes SET nome = ?, cpf = ?, whatsapp = ? WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, cliente.getNome());
            stmt.setString(2, cliente.getCpf());
            stmt.setString(3, cliente.getWhatsApp());
            stmt.setInt(4, cliente.getId());

            stmt.executeUpdate();
        } catch (SQLException e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            String state = e.getSQLState() != null ? e.getSQLState() : "";
            if ("23505".equals(state) || msg.contains("unique") || msg.contains("duplicate key")) {
                throw new ValidationException("Violação de unicidade: o CPF informado já pertence a outro cliente.", e);
            }
            throw new DatabaseException("Erro ao atualizar cliente.", e);
        }
    }

    @Override
    public void deletar(int id) {
        String sql = "DELETE FROM clientes WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao remover cliente.", e);
        }
    }

    @Override
    public boolean possuiVinculos(int clienteId) {
        String sql = """
            SELECT (
                (SELECT COUNT(1) FROM servicos WHERE cliente_id = ?) +
                (SELECT COUNT(1) FROM orcamentos WHERE cliente_id = ?)
            ) AS total_vinculos
        """;
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, clienteId);
            stmt.setInt(2, clienteId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
            return false;
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao verificar vínculos do cliente.", e);
        }
    }

    @Override
    public void deletarEmCascata(int clienteId) {
        String sqlDelCobrancas = """
            DELETE FROM cobrancas
            WHERE orcamento_id IN (
                SELECT id FROM orcamentos
                WHERE cliente_id = ? OR servico_id IN (SELECT id FROM servicos WHERE cliente_id = ?)
            )
        """;
        String sqlDelOrcamentos = """
            DELETE FROM orcamentos
            WHERE cliente_id = ? OR servico_id IN (SELECT id FROM servicos WHERE cliente_id = ?)
        """;
        String sqlDelItensEtapa = """
            DELETE FROM itens_etapa
            WHERE servico_id IN (SELECT id FROM servicos WHERE cliente_id = ?)
        """;
        String sqlDelServicos = "DELETE FROM servicos WHERE cliente_id = ?";
        String sqlDelCliente = "DELETE FROM clientes WHERE id = ?";

        try (Connection conn = ConnectionFactory.getConnection()) {
            boolean autoCommitOriginal = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement stmtCob = conn.prepareStatement(sqlDelCobrancas)) {
                    stmtCob.setInt(1, clienteId);
                    stmtCob.setInt(2, clienteId);
                    stmtCob.executeUpdate();
                }
                try (PreparedStatement stmtOrc = conn.prepareStatement(sqlDelOrcamentos)) {
                    stmtOrc.setInt(1, clienteId);
                    stmtOrc.setInt(2, clienteId);
                    stmtOrc.executeUpdate();
                }
                try (PreparedStatement stmtItens = conn.prepareStatement(sqlDelItensEtapa)) {
                    stmtItens.setInt(1, clienteId);
                    stmtItens.executeUpdate();
                }
                try (PreparedStatement stmtServ = conn.prepareStatement(sqlDelServicos)) {
                    stmtServ.setInt(1, clienteId);
                    stmtServ.executeUpdate();
                }
                try (PreparedStatement stmtCli = conn.prepareStatement(sqlDelCliente)) {
                    stmtCli.setInt(1, clienteId);
                    stmtCli.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    e.addSuppressed(rollbackEx);
                }
                throw e;
            } finally {
                try {
                    conn.setAutoCommit(autoCommitOriginal);
                } catch (SQLException ignored) {
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao remover cliente e registros vinculados em cascata.", e);
        }
    }

    private Cliente mapearCliente(ResultSet rs) throws SQLException {
        return new Cliente(
            rs.getInt("id"),
            rs.getString("nome"),
            rs.getString("cpf"),
            rs.getString("whatsapp")
        );
    }
}
