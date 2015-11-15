package gr.uoa.di.finer;

import java.util.HashMap;
import java.util.Map;

/**
 * An enumeration of the states that an election can be in.
 *
 * @author Vasilis Poulimenos
 */
public enum ElectionStatus {
    /**
     * The election is created, but not initialized.
     */
    UNINITIALIZED(0),
    /**
     * The election is in the middle of the initialization process.
     */
    INITIALIZING(1),
    /**
     * The election is initialized, but not verified.
     */
    INITIALIZED(2),
    /**
     * The election is in the middle of the verification process.
     */
    VERIFYING(3),
    /**
     * The election is verified, but the results are not yet published to the ABB.
     */
    VERIFIED(4),
    /**
     * The election results are being published to the ABB.
     */
    PUBLISHING(5),
    /**
     * The trustee has completed their job and has nothing else to do.
     */
    COMPLETED(6),
    /**
     * The election is being erased. Used mainly for logistic purposes.
     */
    ERASING(7);

    private static final Map<Integer, ElectionStatus> values = new HashMap<>();

    static {
        for (ElectionStatus status: values()) {
            values.put(status.value, status);
        }
    }

    public static ElectionStatus valueOf(int status) {
        return values.get(status);
    }

    private final int value;

    ElectionStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
