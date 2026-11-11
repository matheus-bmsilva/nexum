package nexum.repository.sqlite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import nexum.Usuario;
import nexum.database.ConnectionFactory;
import nexum.exception.DatabaseException;
import nexum.exception.ValidationException;
import nexum.repository.UsuarioRepository;

/**
 * Implementação SQLite do repositório de usuários (arquitetos/profissionais).
 */
public class SqliteUsuarioRepository implements UsuarioRepository {

    @Override
    public Usuario salvar(Usuario usuario) {
        String sql = "INSERT INTO usuarios (nome, email, senha, whatsapp, cnpj, rbt12) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, usuario.getNome());
            stmt.setString(2, usuario.getEmail());
            stmt.setString(3, usuario.getSenha());
            stmt.setString(4, usuario.getWhatsApp());
            stmt.setString(5, usuario.getCnpj());
            stmt.setDouble(6, usuario.getRbt12());

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    usuario.setId(rs.getInt(1));
                }
            }
            return usuario;
        } catch (SQLException e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            String state = e.getSQLState() != null ? e.getSQLState() : "";
            if ("23505".equals(state) || msg.contains("unique") || msg.contains("duplicate key")) {
                throw new ValidationException("Violação de unicidade: CNPJ ou e-mail já cadastrado.", e);
            }
            throw new DatabaseException("Erro ao salvar usuário no banco de dados.", e);
        }
    }

    @Override
    public Optional<Usuario> buscarPorId(int id) {
        String sql = "SELECT id, nome, email, senha, whatsapp, cnpj, rbt12 FROM usuarios WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapearUsuario(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao buscar usuário por ID.", e);
        }
    }

    @Override
    public Optional<Usuario> buscarPorCnpj(String cnpj) {
        String sql = "SELECT id, nome, email, senha, whatsapp, cnpj, rbt12 FROM usuarios WHERE cnpj = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, cnpj);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapearUsuario(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao buscar usuário por CNPJ.", e);
        }
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        String sql = "SELECT id, nome, email, senha, whatsapp, cnpj, rbt12 FROM usuarios WHERE LOWER(email) = LOWER(?)";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email.trim());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapearUsuario(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao buscar usuário por e-mail.", e);
        }
    }

    @Override
    public List<Usuario> listarTodos() {
        String sql = "SELECT id, nome, email, senha, whatsapp, cnpj, rbt12 FROM usuarios ORDER BY nome ASC";
        List<Usuario> usuarios = new ArrayList<>();
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                usuarios.add(mapearUsuario(rs));
            }
            return usuarios;
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao listar usuários.", e);
        }
    }

    @Override
    public void atualizar(Usuario usuario) {
        String sql = "UPDATE usuarios SET nome = ?, email = ?, senha = ?, whatsapp = ?, cnpj = ?, rbt12 = ? WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, usuario.getNome());
            stmt.setString(2, usuario.getEmail());
            stmt.setString(3, usuario.getSenha());
            stmt.setString(4, usuario.getWhatsApp());
            stmt.setString(5, usuario.getCnpj());
            stmt.setDouble(6, usuario.getRbt12());
            stmt.setInt(7, usuario.getId());

            stmt.executeUpdate();
        } catch (SQLException e) {
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            String state = e.getSQLState() != null ? e.getSQLState() : "";
            if ("23505".equals(state) || msg.contains("unique") || msg.contains("duplicate key")) {
                throw new ValidationException("Violação de unicidade ao atualizar dados: documento ou e-mail já em uso.", e);
            }
            throw new DatabaseException("Erro ao atualizar dados do usuário.", e);
        }
    }

    @Override
    public void deletar(int id) {
        String sql = "DELETE FROM usuarios WHERE id = ?";
        try (Connection conn = ConnectionFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao remover usuário.", e);
        }
    }

    private Usuario mapearUsuario(ResultSet rs) throws SQLException {
        double rbt12 = 0.0;
        try {
            rbt12 = rs.getDouble("rbt12");
        } catch (SQLException ignored) {
            // Em caso de query legada sem a coluna
        }

        return new Usuario(
            rs.getInt("id"),
            rs.getString("nome"),
            rs.getString("email"),
            rs.getString("senha"),
            rs.getString("whatsapp"),
            rs.getString("cnpj"),
            rbt12
        );
    }
}
