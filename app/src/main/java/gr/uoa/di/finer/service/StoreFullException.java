package gr.uoa.di.finer.service;

/**
 * Thrown to indicate that the store used is full.
 *
 * @author Vasilis Poulimenos
 */
public class StoreFullException extends StoreException {

    private static final String MESSAGE = "Store or disk is full";
    /**
     * Constructs a new {@code StoreFullException} with a default detail message.
     */
    public StoreFullException() {
        super(MESSAGE);
    }

    /**
     * Constructs a new {@code StoreFullException} with a default detail message and specified cause.
     *
     * @param cause the cause of this exception.
     */
    public StoreFullException(Throwable cause) {
        super(MESSAGE, cause);
    }

}
