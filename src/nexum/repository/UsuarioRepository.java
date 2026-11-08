package nexum.repository;

import java.util.List;
import java.util.Optional;
import nexum.Usuario;

/**
 * Interface de repositório para a entidade Usuario (arquitetos/profissionais liberais).
 */
public interface UsuarioRepository {

    Usuario salvar(Usuario usuario);

    Optional<Usuario> buscarPorId(int id);

    Optional<Usuario> buscarPorCnpj(String cnpj);

    List<Usuario> listarTodos();

    void atualizar(Usuario usuario);

    void deletar(int id);
}
