package nexum.repository;

import java.util.List;
import java.util.Optional;
import nexum.Cliente;

/**
 * Interface de repositório para a entidade Cliente (Princípio de Segregação de Interface - ISP).
 * Desacopla a camada de domínio e regras de negócio da tecnologia de persistência (DIP).
 */
public interface ClienteRepository {

    Cliente salvar(Cliente cliente);

    Optional<Cliente> buscarPorId(int id);

    Optional<Cliente> buscarPorCpf(String cpf);

    List<Cliente> listarTodos();

    void atualizar(Cliente cliente);

    void deletar(int id);

    boolean possuiVinculos(int clienteId);

    void deletarEmCascata(int clienteId);
}
