package gr.uoa.di.finer.parse;

/**
 * Thrown to indicate that a data file is empty.
 *
 * @author Vasilis Poulimenos
 */
public class EmptyFileException extends TruncatedFileException {

    /**
     * Constructs a new {@code EmptyFileException} with a default detail message.
     */
    public EmptyFileException() {
        super("Empty file");
    }

}
