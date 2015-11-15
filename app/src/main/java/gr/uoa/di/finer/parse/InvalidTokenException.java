package gr.uoa.di.finer.parse;

import gr.uoa.di.finer.service.ParseException;

/**
 * Thrown to indicate that an invalid token was encountered.
 *
 * @author Vasilis Poulimenos
 */
class InvalidTokenException extends ParseException {

    /**
     * Constructs a new {@code InvalidTokenException} with a detail message containing the token.
     */
    InvalidTokenException(String token) {
        super(token);
    }

}
