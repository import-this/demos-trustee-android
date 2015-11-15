package gr.uoa.di.finer.parse;

/**
 * Thrown to indicate that an empty (zero-length) key was read.
 *
 * @author Vasilis Poulimenos
 */
public class ZeroLengthKeyException extends EmptyTokenException {

    /**
     * Constructs a new {@code ZeroLengthKeyException} with a default detail message.
     */
    public ZeroLengthKeyException() {
        super("decommitment key");
    }

}
