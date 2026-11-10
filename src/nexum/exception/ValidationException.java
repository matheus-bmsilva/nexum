package nexum.exception;

/**
 * Exceção de negócio para falhas de validação de dados de entrada da aplicação.
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
