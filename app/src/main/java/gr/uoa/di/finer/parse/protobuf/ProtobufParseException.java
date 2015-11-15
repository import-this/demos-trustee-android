package gr.uoa.di.finer.parse.protobuf;

import gr.uoa.di.finer.service.ParseException;

/**
 * Thrown to indicate that an error occurred while parsing a Protocol Buffer message.
 *
 * @author Vasilis Poulimenos
 */
class ProtobufParseException extends ParseException {

    /**
     * Constructs a new {@code ProtobufParseException} with the specified detail message and cause.
     *
     * @param message the detail message for this exception.
     * @param cause the cause of this exception.
     */
    ProtobufParseException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new {@code ProtobufParseException} with the specified cause.
     *
     * @param cause the cause of this exception.
     */
    ProtobufParseException(Throwable cause) {
        super(cause);
    }

}
