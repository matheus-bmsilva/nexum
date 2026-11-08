package nexum.repository;

import java.util.List;
import java.util.Optional;
import nexum.Servico;

/**
 * Interface de repositório polimórfica para a hierarquia de Servico (Princípio de Substituição de Liskov - LSP).
 * Trata especializações de precificação de forma transparente para as camadas superiores.
 */
public interface ServicoRepository {

    Servico salvar(Servico servico);

    Optional<Servico> buscarPorId(int id);

    List<Servico> listarPorCliente(int clienteId);

    List<Servico> listarTodos();

    void atualizar(Servico servico);

    void deletar(int id);
}
