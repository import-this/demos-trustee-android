package gr.uoa.di.finer.parse;

/**
 * Thrown to indicate that a data file is incomplete.
 *
 * @author Vasilis Poulimenos
 */
public class TruncatedFileException extends EOFException {

    /**
     * Constructs a new {@code TruncatedFileException} with the specified detail message.
     *
     * @param message the detail message for this exception.
     */
    public TruncatedFileException(String message) {
        super(message);
    }

}
