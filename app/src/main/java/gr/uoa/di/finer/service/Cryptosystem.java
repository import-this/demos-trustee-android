package gr.uoa.di.finer.service;

import android.support.annotation.WorkerThread;

/**
 *
 * @author Vasilis Poulimenos
 */
@WorkerThread
public interface Cryptosystem {

    /**
     * Add the decommitment specified to the decommitment bundle.
     *
     * @param decommitment the decommitment to add
     */
    void add(String decommitment);

    /**
     * Compute the final decommitment bundle.
     *
     * @return the final decommitment bundle
     */
    String computeBundle();

}
