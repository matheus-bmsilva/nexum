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
import nexum.Orcamento;
import nexum.Servico;
import nexum.StatusOrcamento;
import nexum.database.ConnectionFactory;
import nexum.exception.DatabaseException;
import nexum.repository.ClienteRepository;
import nexum.repository.OrcamentoRepository;
import nexum.repository.ServicoRepository;

/**
 * Repositório SQLite para gerenciamento de orçamentos e propostas comerciais.
 * Injeta dependências abstratas (DIP) de ServicoRepository e ClienteRepository via construtor.
 */
public class SqliteOrcamentoRepository implements OrcamentoRepository {

    private final ServicoRepository servicoRepository;
    private final ClienteRepository clienteRepository;

    public SqliteOrcamentoRepository() {
        this(new SqliteServicoRepository(), new SqliteClienteRepository());
    }

    public SqliteOrcamentoRepository(ServicoRepository servicoRepository, ClienteRepository clienteRepository) {
        this.servicoRepository = servicoRepository;
        this.clienteRepository = clienteRepository;
    }

    @Override
    public Orcamento salvar(Orcamento orcamento) {
        String sql = """
            INSERT INTO orcamentos (
                data_emissao, validade, valor_total, status_orcamento, servico_id, cliente_id
            ) VALUES (?, ?, ?, ?, ?, ?)
        """;

        int servicoId = (orcamento.getServico() != null) ? orcamento.getServico().getId() : 0;
        int clienteId = (orcamento.getCliente() != null) ? orcamento.getCliente().getId() : 0;
        String statusStr = (orcamento.getStatusOrcamento() != null)
                ? orcamento.getStatusOrcamento().name()
                : StatusOrcamento.PENDENTE.name();

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, orcamento.getDataEmissao());
            stmt.setInt(2, orcamento.getValidade());
            stmt.setDouble(3, orcamento.getValorTotal());
            stmt.setString(4, statusStr);
            stmt.setInt(5, servicoId);
            stmt.setInt(6, clienteId);

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    orcamento.setId(rs.getInt(1));
                }
            }
            return orcamento;
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao salvar orçamento no banco de dados.", e);
        }
    }

    @Override
    public Optional<Orcamento> buscarPorId(int id) {
        String sql = "SELECT id, data_emissao, validade, valor_total, status_orcamento, servico_id, cliente_id FROM orcamentos WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapearOrcamento(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao buscar orçamento por ID.", e);
        }
    }

    @Override
    public List<Orcamento> listarPorCliente(int clienteId) {
        String sql = "SELECT id, data_emissao, validade, valor_total, status_orcamento, servico_id, cliente_id FROM orcamentos WHERE cliente_id = ? ORDER BY id DESC";
        List<Orcamento> orcamentos = new ArrayList<>();
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, clienteId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    orcamentos.add(mapearOrcamento(rs));
                }
            }
            return orcamentos;
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao listar orçamentos por cliente.", e);
        }
    }

    @Override
    public List<Orcamento> listarTodos() {
        String sql = "SELECT id, data_emissao, validade, valor_total, status_orcamento, servico_id, cliente_id FROM orcamentos ORDER BY id DESC";
        List<Orcamento> orcamentos = new ArrayList<>();
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                orcamentos.add(mapearOrcamento(rs));
            }
            return orcamentos;
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao listar todos os orçamentos.", e);
        }
    }

    @Override
    public void atualizarStatus(int id, StatusOrcamento status) {
        String sql = "UPDATE orcamentos SET status_orcamento = ? WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            stmt.setInt(2, id);

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao atualizar status do orçamento.", e);
        }
    }

    @Override
    public void deletar(int id) {
        String sql = "DELETE FROM orcamentos WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao excluir orçamento.", e);
        }
    }

    private Orcamento mapearOrcamento(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String dataEmissao = rs.getString("data_emissao");
        int validade = rs.getInt("validade");
        double valorTotal = rs.getDouble("valor_total");
        String statusStr = rs.getString("status_orcamento");
        StatusOrcamento status = (statusStr != null) ? StatusOrcamento.valueOf(statusStr) : StatusOrcamento.PENDENTE;

        int servicoId = rs.getInt("servico_id");
        int clienteId = rs.getInt("cliente_id");

        Servico servico = servicoRepository.buscarPorId(servicoId).orElse(null);
        Cliente cliente = clienteRepository.buscarPorId(clienteId).orElse(null);

        Orcamento orcamento = new Orcamento(id, dataEmissao, validade, valorTotal, status, servico, cliente);
        orcamento.setStatusOrcamento(status);
        return orcamento;
    }
}
