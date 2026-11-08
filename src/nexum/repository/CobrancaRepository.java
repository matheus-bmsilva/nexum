package nexum.repository;

import java.util.List;
import java.util.Optional;
import nexum.Cobranca;

/**
 * Interface de repositório para o controle de cobranças e inadimplência.
 */
public interface CobrancaRepository {

    Cobranca salvar(Cobranca cobranca);

    Optional<Cobranca> buscarPorId(int id);

    List<Cobranca> listarPorOrcamento(int orcamentoId);

    List<Cobranca> listarTodas();

    void registrarPagamento(int id, String dataPagamento);

    void deletar(int id);
}
