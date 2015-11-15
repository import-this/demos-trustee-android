package gr.uoa.di.finer.parse;

/**
 * Thrown to indicate that an empty token was encountered.
 *
 * @author Vasilis Poulimenos
 */
class EmptyTokenException extends InvalidTokenException {

    /**
     * Constructs a new {@code EmptyTokenException} with a detail message containing the token type.
     */
    EmptyTokenException(String tokenType) {
        super("Empty token: " + tokenType);
    }

}
