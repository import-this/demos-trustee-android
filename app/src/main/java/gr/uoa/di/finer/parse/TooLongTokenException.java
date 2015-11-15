package gr.uoa.di.finer.parse;

/**
 * Thrown to indicate that an unacceptably large token was encountered.
 *
 * @author Vasilis Poulimenos
 */
class TooLongTokenException extends InvalidTokenException {

    /**
     * Constructs a new {@code TooLongTokenException} with a detail message containing the token.
     */
    TooLongTokenException(String token) {
        super("Unacceptably large token: " + token);
    }

}
