package gr.uoa.di.finer.parse;

import gr.uoa.di.finer.service.ParseException;

/**
 * Thrown to indicate that the end of the stream was reached unexpectedly.
 *
 * @author Vasilis Poulimenos
 */
class EOFException extends ParseException {

    /**
     * Constructs a new {@code EOFException} with a default detail message.
     */
    EOFException() {
        super("Unexpected end of input");
    }

    /**
     * Constructs a new {@code EOFException} with the specified detail message.
     *
     * @param message the detail message for this exception.
     */
    EOFException(String message) {
        super(message);
    }
}
