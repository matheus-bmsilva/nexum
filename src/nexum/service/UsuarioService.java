package nexum.service;

import java.util.List;
import java.util.Optional;
import nexum.Usuario;
import nexum.exception.EntityNotFoundException;
import nexum.exception.ValidationException;
import nexum.repository.UsuarioRepository;
import nexum.repository.sqlite.SqliteUsuarioRepository;
import nexum.security.PasswordSecurity;
import nexum.validation.DocumentoValidator;

/**
 * Serviço de negócio para autenticação e gestão de usuários (arquitetos/escritórios).
 * Implementa SRP, isolando as regras de validação de CNPJ, hashing de senhas e unicidade.
 */
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService() {
        this(new SqliteUsuarioRepository());
    }

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Cadastra um novo arquiteto/escritório com validação matemática estrita de CNPJ
     * e hashing criptográfico seguro de senha.
     */
    public Usuario cadastrar(String nome, String email, String whatsApp, String cnpj, String senha) {
        validarCampos(nome, email, whatsApp, cnpj, senha, true);

        String cnpjFormatado = DocumentoValidator.formatarCnpj(cnpj);
        String cnpjLimpo = DocumentoValidator.sanitizar(cnpj);
        String emailLimpo = (email != null) ? email.trim().toLowerCase() : "";

        if (usuarioRepository.buscarPorCnpj(cnpjFormatado).isPresent() || usuarioRepository.buscarPorCnpj(cnpjLimpo).isPresent()) {
            throw new ValidationException("Já existe um arquiteto/escritório cadastrado com o CNPJ informado: " + cnpjFormatado);
        }

        if (!emailLimpo.isBlank() && usuarioRepository.buscarPorEmail(emailLimpo).isPresent()) {
            throw new ValidationException("Já existe um arquiteto/escritório cadastrado com o e-mail informado: " + emailLimpo);
        }

        String senhaHash = PasswordSecurity.hashPassword(senha);
        String telSanitizado = ClienteService.sanitizarTelefone(whatsApp);

        Usuario usuario = new Usuario(0, nome.trim(), emailLimpo, senhaHash, telSanitizado, cnpjFormatado);
        return usuarioRepository.salvar(usuario);
    }

    /**
     * Autentica o usuário via CNPJ ou E-mail e senha.
     */
    public Usuario login(String identificador, String senha) {
        if (identificador == null || identificador.isBlank() || senha == null || senha.isBlank()) {
            throw new ValidationException("Identificador de acesso (CNPJ ou E-mail) e senha são obrigatórios.");
        }

        String idLimpo = identificador.trim();
        Optional<Usuario> usuarioOpt;

        if (idLimpo.contains("@")) {
            usuarioOpt = usuarioRepository.buscarPorEmail(idLimpo.toLowerCase());
        } else {
            String cnpjFormatado = DocumentoValidator.formatarCnpj(idLimpo);
            usuarioOpt = usuarioRepository.buscarPorCnpj(cnpjFormatado);
            if (usuarioOpt.isEmpty()) {
                usuarioOpt = usuarioRepository.buscarPorCnpj(DocumentoValidator.sanitizar(idLimpo));
            }
        }

        if (usuarioOpt.isEmpty()) {
            throw new ValidationException("Credenciais inválidas: CNPJ/e-mail ou senha incorretos.");
        }

        Usuario usuario = usuarioOpt.get();
        if (!PasswordSecurity.checkPassword(senha, usuario.getSenha())) {
            throw new ValidationException("Credenciais inválidas: CNPJ/e-mail ou senha incorretos.");
        }

        return usuario;
    }

    public Usuario buscarPorId(int id) {
        return usuarioRepository.buscarPorId(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado para o ID: " + id));
    }

    public Optional<Usuario> buscarPorCnpj(String cnpj) {
        if (cnpj == null || cnpj.isBlank()) {
            return Optional.empty();
        }
        Optional<Usuario> user = usuarioRepository.buscarPorCnpj(DocumentoValidator.formatarCnpj(cnpj));
        if (user.isPresent()) {
            return user;
        }
        return usuarioRepository.buscarPorCnpj(DocumentoValidator.sanitizar(cnpj));
    }

    public Optional<Usuario> buscarPorEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return usuarioRepository.buscarPorEmail(email.trim().toLowerCase());
    }

    public List<Usuario> listarTodos() {
        return usuarioRepository.listarTodos();
    }

    /**
     * Atualiza dados de perfil do usuário autenticado.
     */
    public Usuario atualizarPerfil(int id, String nome, String email, String whatsApp, String novaSenhaOpcional) {
        Usuario usuario = buscarPorId(id);

        if (nome == null || nome.trim().length() < 2) {
            throw new ValidationException("O nome do escritório/arquiteto deve ter ao menos 2 caracteres.");
        }

        String emailLimpo = (email != null) ? email.trim().toLowerCase() : "";
        if (!emailLimpo.isBlank()) {
            if (!emailLimpo.contains("@") || !emailLimpo.contains(".")) {
                throw new ValidationException("O formato do e-mail informado é inválido.");
            }
            Optional<Usuario> outroComEmail = usuarioRepository.buscarPorEmail(emailLimpo);
            if (outroComEmail.isPresent() && outroComEmail.get().getId() != id) {
                throw new ValidationException("O e-mail informado já está sendo utilizado por outro cadastro.");
            }
        }

        usuario.setNome(nome.trim());
        usuario.setEmail(emailLimpo);
        usuario.setWhatsApp(ClienteService.sanitizarTelefone(whatsApp));

        if (novaSenhaOpcional != null && !novaSenhaOpcional.isBlank()) {
            validarComplexidadeSenha(novaSenhaOpcional);
            usuario.setSenha(PasswordSecurity.hashPassword(novaSenhaOpcional));
        }

        usuarioRepository.atualizar(usuario);
        return usuario;
    }

    public void deletar(int id) {
        buscarPorId(id);
        usuarioRepository.deletar(id);
    }

    /**
     * Valida a política rigorosa de complexidade de senha:
     * - Mínimo de 12 caracteres;
     * - Pelo menos uma letra maiúscula;
     * - Pelo menos um caractere especial.
     */
    public static void validarComplexidadeSenha(String senha) {
        if (senha == null || senha.isBlank()) {
            throw new ValidationException("A senha de acesso é obrigatória.");
        }
        if (senha.length() < 12) {
            throw new ValidationException("A senha deve conter no mínimo 12 caracteres.");
        }

        boolean temMaiuscula = false;
        boolean temEspecial = false;

        for (char c : senha.toCharArray()) {
            if (Character.isUpperCase(c)) {
                temMaiuscula = true;
            } else if (!Character.isLetterOrDigit(c)) {
                temEspecial = true;
            }
        }

        if (!temMaiuscula) {
            throw new ValidationException("A senha deve conter pelo menos uma letra maiúscula.");
        }
        if (!temEspecial) {
            throw new ValidationException("A senha deve conter pelo menos um caractere especial (ex: @, #, $, %, &, *).");
        }
    }

    private void validarCampos(String nome, String email, String whatsApp, String cnpj, String senha, boolean validarSenha) {
        if (nome == null || nome.trim().isBlank()) {
            throw new ValidationException("O nome do escritório ou profissional é obrigatório.");
        }
        if (nome.trim().length() < 2) {
            throw new ValidationException("O nome deve conter ao menos 2 caracteres.");
        }
        if (email != null && !email.isBlank()) {
            if (!email.contains("@") || !email.contains(".")) {
                throw new ValidationException("O e-mail informado é inválido.");
            }
        }
        if (cnpj == null || cnpj.trim().isBlank()) {
            throw new ValidationException("O CNPJ do escritório/arquiteto é obrigatório.");
        }
        if (!DocumentoValidator.isCnpjValido(cnpj)) {
            throw new ValidationException("O CNPJ informado é inválido: cálculo dos dígitos verificadores não confere ou número inválido.");
        }
        if (whatsApp == null || whatsApp.trim().isBlank()) {
            throw new ValidationException("O telefone/WhatsApp de contato é obrigatório.");
        }
        String apenasNumerosTel = ClienteService.sanitizarTelefone(whatsApp);
        if (apenasNumerosTel.length() < 10 || apenasNumerosTel.length() > 13) {
            throw new ValidationException("O telefone/WhatsApp informado é inválido.");
        }
        if (validarSenha) {
            validarComplexidadeSenha(senha);
        }
    }
}
