package gr.uoa.di.finer.service;

/**
 * Thrown to indicate that an error occurred while attempting to perform an operation with a store.
 *
 * @author Vasilis Poulimenos
 */
public class StoreException extends Exception {

    /**
     * Constructs a new {@code StoreException} with the specified detail message.
     *
     * @param message the detail message for this exception.
     */
    public StoreException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code StoreException} with the specified detail message and cause.
     *
     * @param message the detail message for this exception.
     * @param cause the cause of this exception.
     */
    public StoreException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new {@code StoreException} with the specified cause.
     *
     * @param cause the cause of this exception.
     */
    public StoreException(Throwable cause) {
        super(cause);
    }

}
