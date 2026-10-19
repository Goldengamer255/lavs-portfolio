package lavs.service;

import java.util.function.Consumer;
import lavs.gateway.OperationResult;

/**
 * JavaFX-independent monitoring contract. Scheduled probes use the same serial
 * worker as service calls; callers must also use that worker for manual probes
 * to avoid concurrent access to the shared mainframe connection.
 * Callbacks may run on that worker; the shell marshals UI updates to JavaFX.
 */
public interface ConnectionMonitor extends AutoCloseable {
    /** Starts periodic checks; repeated starts do not create duplicate schedules. */
    void start();
    /** Last known state; false until the first successful probe. No I/O. */
    boolean isOnline();
    /**
     * Performs a synchronous probe on the calling thread; invoke through the
     * shared worker. A successful result containing false means unreachable.
     */
    OperationResult<Boolean> checkNow();
    /** Registers a listener; close the returned subscription to remove it. */
    Subscription subscribe(Consumer<Boolean> listener);
    /** Stops checks and clears listeners; does not shut down the shared executor. */
    @Override void close();

    interface Subscription extends AutoCloseable {
        @Override void close();
    }
}
