package nexum.service;

import java.util.List;
import java.util.Optional;
import nexum.Cliente;
import nexum.exception.EntityNotFoundException;
import nexum.exception.ValidationException;
import nexum.repository.ClienteRepository;
import nexum.repository.sqlite.SqliteClienteRepository;

import nexum.validation.DocumentoValidator;

/**
 * Serviço de negócio para gerenciamento e validação de clientes (RF02).
 * Respeita SRP isolando as regras cadastrais da persistência e dos controladores HTTP.
 */
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService() {
        this(new SqliteClienteRepository());
    }

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    public Cliente cadastrar(String nome, String cpf, String whatsApp) {
        validarCampos(nome, cpf, whatsApp);

        String cpfFormatado = DocumentoValidator.formatarCpf(cpf);
        String cpfLimpo = DocumentoValidator.sanitizar(cpf);

        if (clienteRepository.buscarPorCpf(cpfFormatado).isPresent() || clienteRepository.buscarPorCpf(cpfLimpo).isPresent()) {
            throw new ValidationException("Já existe um cliente cadastrado com o CPF informado: " + cpfFormatado);
        }

        Cliente cliente = new Cliente(0, nome.trim(), cpfFormatado, sanitizarTelefone(whatsApp));
        return clienteRepository.salvar(cliente);
    }

    public Cliente atualizar(int id, String nome, String cpf, String whatsApp) {
        validarCampos(nome, cpf, whatsApp);

        Cliente clienteExistente = buscarPorId(id);
        String cpfFormatado = DocumentoValidator.formatarCpf(cpf);
        String cpfLimpo = DocumentoValidator.sanitizar(cpf);

        Optional<Cliente> existenteComCpf = clienteRepository.buscarPorCpf(cpfFormatado);
        if (existenteComCpf.isEmpty()) {
            existenteComCpf = clienteRepository.buscarPorCpf(cpfLimpo);
        }
        if (existenteComCpf.isPresent() && existenteComCpf.get().getId() != id) {
            throw new ValidationException("Já existe outro cliente cadastrado com o CPF informado: " + cpfFormatado);
        }

        clienteExistente.setNome(nome.trim());
        clienteExistente.setCpf(cpfFormatado);
        clienteExistente.setWhatsApp(sanitizarTelefone(whatsApp));

        clienteRepository.atualizar(clienteExistente);
        return clienteExistente;
    }

    public Cliente buscarPorId(int id) {
        return clienteRepository.buscarPorId(id)
                .orElseThrow(() -> new EntityNotFoundException("Cliente não localizado para o ID: " + id));
    }

    public Optional<Cliente> buscarPorCpf(String cpf) {
        if (cpf == null || cpf.isBlank()) {
            return Optional.empty();
        }
        Optional<Cliente> encontrado = clienteRepository.buscarPorCpf(DocumentoValidator.formatarCpf(cpf));
        if (encontrado.isPresent()) {
            return encontrado;
        }
        return clienteRepository.buscarPorCpf(DocumentoValidator.sanitizar(cpf));
    }

    public List<Cliente> listarTodos() {
        return clienteRepository.listarTodos();
    }

    public void deletar(int id) {
        buscarPorId(id); // Garante que existe antes de deletar
        clienteRepository.deletar(id);
    }

    private void validarCampos(String nome, String cpf, String whatsApp) {
        if (nome == null || nome.trim().isBlank()) {
            throw new ValidationException("O nome do cliente é obrigatório.");
        }
        if (nome.trim().length() < 2) {
            throw new ValidationException("O nome do cliente deve conter ao menos 2 caracteres.");
        }
        if (cpf == null || cpf.trim().isBlank()) {
            throw new ValidationException("O CPF do cliente é obrigatório.");
        }
        if (!DocumentoValidator.isCpfValido(cpf)) {
            throw new ValidationException("O CPF informado é inválido: cálculo dos dígitos verificadores não confere ou número inválido.");
        }
        if (whatsApp == null || whatsApp.trim().isBlank()) {
            throw new ValidationException("O número de WhatsApp é obrigatório para envio de propostas.");
        }
        String apenasNumerosTel = sanitizarTelefone(whatsApp);
        if (apenasNumerosTel.length() < 10 || apenasNumerosTel.length() > 13) {
            throw new ValidationException("O número de WhatsApp informado é inválido (informe DDD + número).");
        }
    }

    public static String sanitizarCpf(String cpf) {
        return DocumentoValidator.sanitizar(cpf);
    }

    public static String sanitizarTelefone(String whatsApp) {
        if (whatsApp == null) return "";
        String digitos = whatsApp.replaceAll("\\D", "");
        // Se informado sem código do país (DDI 55 do Brasil), adiciona 55 para compatibilidade wa.me
        if (digitos.length() == 10 || digitos.length() == 11) {
            return "55" + digitos;
        }
        return digitos;
    }
}
