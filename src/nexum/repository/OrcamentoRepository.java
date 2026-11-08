package nexum.repository;

import java.util.List;
import java.util.Optional;
import nexum.Orcamento;
import nexum.StatusOrcamento;

/**
 * Interface de repositório para a emissão e consulta de orçamentos.
 */
public interface OrcamentoRepository {

    Orcamento salvar(Orcamento orcamento);

    Optional<Orcamento> buscarPorId(int id);

    List<Orcamento> listarPorCliente(int clienteId);

    List<Orcamento> listarTodos();

    void atualizarStatus(int id, StatusOrcamento status);

    void deletar(int id);
}
