package nexum.repository.sqlite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import nexum.Cliente;
import nexum.Etapa;
import nexum.ItemEtapa;
import nexum.Servico;
import nexum.ServicoPorEtapa;
import nexum.ServicoPorHora;
import nexum.ServicoPorMetroQuadrado;
import nexum.ServicoPorPercentualObra;
import nexum.database.ConnectionFactory;
import nexum.exception.DatabaseException;
import nexum.repository.ClienteRepository;
import nexum.repository.ServicoRepository;

/**
 * Repositório SQLite para persistência polimórfica da hierarquia de Servico (Single Table Inheritance).
 * Respeita o Princípio de Inversão de Dependência (DIP) recebendo o ClienteRepository via construtor.
 */
public class SqliteServicoRepository implements ServicoRepository {

    private final ClienteRepository clienteRepository;

    public SqliteServicoRepository() {
        this(new SqliteClienteRepository());
    }

    public SqliteServicoRepository(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Override
    public Servico salvar(Servico servico) {
        String sql = """
            INSERT INTO servicos (
                tipo_servico, descricao, data, padrao, cliente_id, imposto,
                area, valor_metro_quadrado, horas_trabalhadas, valor_hora,
                custo_total_obra, percentual
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        String tipo = ServicoFactory.obterTipoServico(servico);
        int clienteId = (servico.getCliente() != null) ? servico.getCliente().getId() : 0;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, tipo);
            stmt.setString(2, servico.getDescricao());
            stmt.setString(3, servico.getData());
            stmt.setString(4, servico.getPadrao() != null ? servico.getPadrao().name() : "MEDIO");
            stmt.setInt(5, clienteId);
            stmt.setDouble(6, servico.getImposto());

            // Parâmetros específicos por subclasse polimórfica
            if (servico instanceof ServicoPorMetroQuadrado m2) {
                stmt.setDouble(7, m2.getArea());
                stmt.setDouble(8, m2.getValorMetroQuadrado());
                setNull(stmt, 9, 10, 11, 12);
            } else if (servico instanceof ServicoPorHora hora) {
                setNull(stmt, 7, 8);
                stmt.setDouble(9, hora.getHorasTrabalhadas());
                stmt.setDouble(10, hora.getValorHora());
                setNull(stmt, 11, 12);
            } else if (servico instanceof ServicoPorPercentualObra perc) {
                setNull(stmt, 7, 8, 9, 10);
                stmt.setDouble(11, perc.getCustoTotalObra());
                stmt.setDouble(12, perc.getPercentual());
            } else {
                setNull(stmt, 7, 8, 9, 10, 11, 12);
            }

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    servico.setId(rs.getInt(1));
                }
            }

            // Se for serviço por etapa, persiste itens associados na tabela itens_etapa
            if (servico instanceof ServicoPorEtapa etapaServico) {
                salvarItensEtapa(conn, servico.getId(), etapaServico.getItensEtapas());
            }

            return servico;
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao salvar serviço polimórfico no banco de dados.", e);
        }
    }

    @Override
    public Optional<Servico> buscarPorId(int id) {
        String sql = "SELECT * FROM servicos WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(reconstruirServico(conn, rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao buscar serviço por ID.", e);
        }
    }

    @Override
    public List<Servico> listarPorCliente(int clienteId) {
        String sql = "SELECT * FROM servicos WHERE cliente_id = ? ORDER BY id ASC";
        List<Servico> servicos = new ArrayList<>();
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, clienteId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    servicos.add(reconstruirServico(conn, rs));
                }
            }
            return servicos;
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao listar serviços do cliente.", e);
        }
    }

    @Override
    public List<Servico> listarTodos() {
        String sql = "SELECT * FROM servicos ORDER BY id ASC";
        List<Servico> servicos = new ArrayList<>();
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                servicos.add(reconstruirServico(conn, rs));
            }
            return servicos;
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao listar todos os serviços.", e);
        }
    }

    @Override
    public void atualizar(Servico servico) {
        String sql = """
            UPDATE servicos SET
                descricao = ?, data = ?, padrao = ?, cliente_id = ?, imposto = ?,
                area = ?, valor_metro_quadrado = ?, horas_trabalhadas = ?, valor_hora = ?,
                custo_total_obra = ?, percentual = ?
            WHERE id = ?
        """;

        int clienteId = (servico.getCliente() != null) ? servico.getCliente().getId() : 0;

        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, servico.getDescricao());
            stmt.setString(2, servico.getData());
            stmt.setString(3, servico.getPadrao() != null ? servico.getPadrao().name() : "MEDIO");
            stmt.setInt(4, clienteId);
            stmt.setDouble(5, servico.getImposto());

            if (servico instanceof ServicoPorMetroQuadrado m2) {
                stmt.setDouble(6, m2.getArea());
                stmt.setDouble(7, m2.getValorMetroQuadrado());
                setNull(stmt, 8, 9, 10, 11);
            } else if (servico instanceof ServicoPorHora hora) {
                setNull(stmt, 6, 7);
                stmt.setDouble(8, hora.getHorasTrabalhadas());
                stmt.setDouble(9, hora.getValorHora());
                setNull(stmt, 10, 11);
            } else if (servico instanceof ServicoPorPercentualObra perc) {
                setNull(stmt, 6, 7, 8, 9);
                stmt.setDouble(10, perc.getCustoTotalObra());
                stmt.setDouble(11, perc.getPercentual());
            } else {
                setNull(stmt, 6, 7, 8, 9, 10, 11);
            }

            stmt.setInt(12, servico.getId());
            stmt.executeUpdate();

            // Sincroniza itens da etapa caso aplicável
            if (servico instanceof ServicoPorEtapa etapaServico) {
                removerItensEtapa(conn, servico.getId());
                salvarItensEtapa(conn, servico.getId(), etapaServico.getItensEtapas());
            }

        } catch (SQLException e) {
            throw new DatabaseException("Erro ao atualizar serviço.", e);
        }
    }

    @Override
    public void deletar(int id) {
        String sql = "DELETE FROM servicos WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao remover serviço.", e);
        }
    }

    private Servico reconstruirServico(Connection conn, ResultSet rs) throws SQLException {
        int clienteId = rs.getInt("cliente_id");
        Cliente cliente = clienteRepository.buscarPorId(clienteId).orElse(null);

        String tipo = rs.getString("tipo_servico");
        List<ItemEtapa> itens = null;
        if (ServicoFactory.TIPO_ETAPA.equals(tipo)) {
            itens = carregarItensEtapa(conn, rs.getInt("id"));
        }

        return ServicoFactory.criarServico(rs, cliente, itens);
    }

    private List<ItemEtapa> carregarItensEtapa(Connection conn, int servicoId) throws SQLException {
        String sql = "SELECT etapa, valor FROM itens_etapa WHERE servico_id = ? ORDER BY id ASC";
        List<ItemEtapa> itens = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, servicoId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String etapaNome = rs.getString("etapa");
                    Etapa etapa = Etapa.valueOf(etapaNome);
                    double valor = rs.getDouble("valor");
                    itens.add(new ItemEtapa(etapa, valor));
                }
            }
        }
        return itens;
    }

    private void salvarItensEtapa(Connection conn, int servicoId, List<ItemEtapa> itens) throws SQLException {
        if (itens == null || itens.isEmpty()) {
            return;
        }
        String sql = "INSERT INTO itens_etapa (servico_id, etapa, valor) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (ItemEtapa item : itens) {
                stmt.setInt(1, servicoId);
                stmt.setString(2, item.getEtapa().name());
                stmt.setDouble(3, item.getValor());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    private void removerItensEtapa(Connection conn, int servicoId) throws SQLException {
        String sql = "DELETE FROM itens_etapa WHERE servico_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, servicoId);
            stmt.executeUpdate();
        }
    }

    private void setNull(PreparedStatement stmt, int... indices) throws SQLException {
        for (int idx : indices) {
            stmt.setNull(idx, Types.DOUBLE);
        }
    }
}
