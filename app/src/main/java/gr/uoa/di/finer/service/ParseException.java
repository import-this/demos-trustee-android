package gr.uoa.di.finer.service;

/**
 * Thrown to indicate that an error occurred while parsing some arbitrary data.
 *
 * @author Vasilis Poulimenos
 */
public class ParseException extends Exception {

    /**
     * Constructs a new {@code ParseException} with the specified detail message.
     *
     * @param message the detail message for this exception.
     */
    public ParseException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code ParseException} with the specified detail message and cause.
     *
     * @param message the detail message for this exception.
     * @param cause the cause of this exception.
     */
    public ParseException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new {@code ParseException} with the specified cause.
     *
     * @param cause the cause of this exception.
     */
    public ParseException(Throwable cause) {
        super(cause);
    }

}
