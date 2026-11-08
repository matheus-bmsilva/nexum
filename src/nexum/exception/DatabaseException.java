package nexum.exception;

/**
 * Exceção personalizada de infraestrutura e persistência de dados.
 * Encapsula falhas técnicas de acesso a banco de dados sem expor detalhes sensíveis.
 */
public class DatabaseException extends RuntimeException {

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
