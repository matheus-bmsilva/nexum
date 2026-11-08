package nexum.exception;

/**
 * Exceção de negócio disparada quando uma entidade solicitada não é localizada no repositório.
 */
public class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException(String message) {
        super(message);
    }
}
