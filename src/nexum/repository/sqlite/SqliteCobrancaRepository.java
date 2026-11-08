package nexum.repository.sqlite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import nexum.Cobranca;
import nexum.Orcamento;
import nexum.StatusCobranca;
import nexum.database.ConnectionFactory;
import nexum.exception.DatabaseException;
import nexum.repository.CobrancaRepository;
import nexum.repository.OrcamentoRepository;

/**
 * Repositório SQLite para controle de cobranças e conciliação de recebíveis.
 * Injeta OrcamentoRepository via construtor (DIP).
 */
public class SqliteCobrancaRepository implements CobrancaRepository {

    private final OrcamentoRepository orcamentoRepository;

    public SqliteCobrancaRepository() {
        this(new SqliteOrcamentoRepository());
    }

    public SqliteCobrancaRepository(OrcamentoRepository orcamentoRepository) {
        this.orcamentoRepository = orcamentoRepository;
    }

    @Override
    public Cobranca salvar(Cobranca cobranca) {
        String sql = """
            INSERT INTO cobrancas (
                valor, data_vencimento, data_pagamento, status_cobranca, orcamento_id
            ) VALUES (?, ?, ?, ?, ?)
        """;

        int orcamentoId = (cobranca.getOrcamento() != null) ? cobranca.getOrcamento().getId() : 0;
        String statusStr = (cobranca.getStatusCobranca() != null)
                ? cobranca.getStatusCobranca().name()
                : StatusCobranca.EM_ABERTO.name();

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setDouble(1, cobranca.getValor());
            stmt.setString(2, cobranca.getDataVencimento());
            stmt.setString(3, cobranca.getDataPagamento());
            stmt.setString(4, statusStr);
            stmt.setInt(5, orcamentoId);

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    cobranca.setId(rs.getInt(1));
                }
            }
            return cobranca;
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao salvar cobrança no banco de dados.", e);
        }
    }

    @Override
    public Optional<Cobranca> buscarPorId(int id) {
        String sql = "SELECT id, valor, data_vencimento, data_pagamento, status_cobranca, orcamento_id FROM cobrancas WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapearCobranca(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao buscar cobrança por ID.", e);
        }
    }

    @Override
    public List<Cobranca> listarPorOrcamento(int orcamentoId) {
        String sql = "SELECT id, valor, data_vencimento, data_pagamento, status_cobranca, orcamento_id FROM cobrancas WHERE orcamento_id = ? ORDER BY id DESC";
        List<Cobranca> cobrancas = new ArrayList<>();
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, orcamentoId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    cobrancas.add(mapearCobranca(rs));
                }
            }
            return cobrancas;
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao listar cobranças por orçamento.", e);
        }
    }

    @Override
    public List<Cobranca> listarTodas() {
        String sql = "SELECT id, valor, data_vencimento, data_pagamento, status_cobranca, orcamento_id FROM cobrancas ORDER BY id DESC";
        List<Cobranca> cobrancas = new ArrayList<>();
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                cobrancas.add(mapearCobranca(rs));
            }
            return cobrancas;
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao listar todas as cobranças.", e);
        }
    }

    @Override
    public void registrarPagamento(int id, String dataPagamento) {
        String sql = "UPDATE cobrancas SET data_pagamento = ?, status_cobranca = ? WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, dataPagamento);
            stmt.setString(2, StatusCobranca.PAGO.name());
            stmt.setInt(3, id);

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao registrar liquidação de pagamento.", e);
        }
    }

    @Override
    public void deletar(int id) {
        String sql = "DELETE FROM cobrancas WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao excluir cobrança.", e);
        }
    }

    private Cobranca mapearCobranca(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        double valor = rs.getDouble("valor");
        String dataVencimento = rs.getString("data_vencimento");
        String dataPagamento = rs.getString("data_pagamento");
        String statusStr = rs.getString("status_cobranca");
        StatusCobranca status = (statusStr != null) ? StatusCobranca.valueOf(statusStr) : StatusCobranca.EM_ABERTO;
        int orcamentoId = rs.getInt("orcamento_id");

        Orcamento orcamento = orcamentoRepository.buscarPorId(orcamentoId).orElse(null);

        Cobranca cobranca = new Cobranca(id, valor, dataVencimento, dataPagamento, status, orcamento);
        cobranca.setStatusCobranca(status);
        cobranca.setDataPagamento(dataPagamento);
        return cobranca;
    }
}
